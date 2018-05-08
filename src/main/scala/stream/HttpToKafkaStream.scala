package stream

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Success

class HttpToKafkaStream(implicit val system: ActorSystem, implicit val materializer: ActorMaterializer)
  extends StreamSupport {

  import system.dispatcher

  val baseUrl = config.getString("metro.baseUrl")
  val vehiclesPath = config.getString("metro.vehicles.path")
  val rate = config.getInt("metro.vehicles.rate")
  val kafkaUrl = config.getString("kafka.host") + ":" + config.getInt("kafka.port")
  val kafkaTopic = config.getString("kafka.topic")

  val producerSettings = ProducerSettings(system, new StringSerializer, new StringSerializer).withBootstrapServers(kafkaUrl)

  val source = Source.tick(Duration.Zero, rate milliseconds, HttpRequest(uri = vehiclesPath))

  val connectionFlow = Http().cachedHostConnectionPool[NotUsed](baseUrl, 80)

  val transformFlow: HttpResponse => Future[Option[String]] = {
    case HttpResponse(StatusCodes.OK, _, entity, _) =>
      entity.dataBytes.map(_.utf8String).runWith(Sink.fold("")(_ + _)).map(Option(_))
    case resp@HttpResponse(code, _, _, _) => {
      resp.discardEntityBytes()
      Future.successful(Option.empty)
    }
  }

  val toKafkaMessage = (elem: String) => new ProducerRecord[String, String](kafkaTopic, elem)

  def buildStream = source
      //Packs this in a tuple of HttpRequest and something we don't care to accommodate for cachedHostConnectionPool
      .map((_, NotUsed))
      .via(connectionFlow)
      .collect{ case (Success(httpResponse),_) => httpResponse }
      //what to use instead of 1
      .mapAsync(1)(transformFlow)
      .collect { case Some(json) => toKafkaMessage(json) }
      .to(Producer.plainSink(producerSettings))

  def run = buildStream.run()

}