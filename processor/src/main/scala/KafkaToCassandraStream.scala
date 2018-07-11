package stream

import akka.actor.ActorSystem
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.stream.alpakka.cassandra.scaladsl.CassandraSink
import com.datastax.driver.core.{Cluster, PreparedStatement}
import model._
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}
import scala.concurrent.duration._

class KafkaVehicleStream(implicit val system: ActorSystem, implicit val materializer: ActorMaterializer) extends StreamSupport {

  val kafkaUrl = config.getString("kafka.host") + ":" + config.getInt("kafka.port")
  val kafkaTopic = config.getString("kafka.topic")

  def consumerSettings(groupId: String) = ConsumerSettings(system, new StringDeserializer, new ByteArrayDeserializer)
    .withBootstrapServers(kafkaUrl)
    .withGroupId(groupId)
    .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
    .withWakeupTimeout(10 seconds)
    .withPollTimeout(0.5 seconds)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")

  def kafkaSource(groupId: String) = Consumer.plainSource(consumerSettings(groupId), Subscriptions.topics(kafkaTopic))

  val unmarshalFlow = (record: ConsumerRecord[String, Array[Byte]]) => Unmarshal(new String(record.value)).to[VehicleList]

  def buildStream = kafkaSource("group1")
    .mapAsync(4)(unmarshalFlow)
}

object KafkaToCassandraStream extends App with StreamSupport {

  implicit val system = ActorSystem("http-consumer")
  implicit val materializer = ActorMaterializer()

  val cassandraHost = config.getString("cassandra.host")
  val cassandraPort = config.getInt("cassandra.port")

  implicit val session = Cluster.builder
    .addContactPoint(cassandraHost)
    .withPort(cassandraPort)
    .build
    .connect()

  val stmtInsertVehicle = session.prepare("INSERT INTO metro.vehicle(vehicleId, longitude, latitude) VALUES (?,?,?);")
  val stmtInsertVehicleBinder = (vp: VehiclePosition, statement: PreparedStatement) => statement.bind()
    .setString(0, vp.id)
    .setDouble(1, vp.longitude)
    .setDouble(2, vp.latitude)
  val sinkVehicle = CassandraSink[VehiclePosition](parallelism = 4, stmtInsertVehicle, stmtInsertVehicleBinder)

  new KafkaVehicleStream()
    .buildStream
    .mapConcat(_.items)
    .to(sinkVehicle)
    .run()

}
