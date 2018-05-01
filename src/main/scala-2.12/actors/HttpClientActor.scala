package actors

import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString

case object Consume

class HttpClientActor extends Actor with ActorLogging {

  val metroUrl = "http://api.metro.net/agencies/lametro/vehicles/";

  import akka.pattern.pipe
  import context.dispatcher

  final implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  val http = Http(context.system)

  override def preStart(): Unit = log.info("HttpClientActor started")
  override def postStop(): Unit = log.info("HttpClientActor stopped")

  override def receive: Receive = {
    case Consume => http.singleRequest(HttpRequest(uri = metroUrl)).pipeTo(self)
    case HttpResponse(StatusCodes.OK, headers, entity, _) =>
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
        log.info("Got response, body: " + body.utf8String)
      }
    case resp @ HttpResponse(code, _, _, _) =>
      log.info("Request failed, response code: " + code)
      resp.discardEntityBytes()
  }
}
