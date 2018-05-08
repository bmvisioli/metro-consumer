package stream

import akka.actor.ActorSystem
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.stream.alpakka.cassandra.scaladsl.CassandraSink
import com.datastax.driver.core.{PreparedStatement, Session}
import model.{QuadKey, VehicleConversionSupport, VehicleList, VehiclePosition}
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.apache.kafka.common.serialization.StringDeserializer

import scala.concurrent.duration._


class KafkaToCassandraStream(implicit val system: ActorSystem, implicit val materializer: ActorMaterializer, implicit val session: Session)
  extends VehicleConversionSupport
    with StreamSupport {

  val kafkaUrl = config.getString("kafka.host") + ":" + config.getInt("kafka.port")
  val kafkaTopic = config.getString("kafka.topic")

  val consumerSettings = ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
    .withBootstrapServers(kafkaUrl)
    .withGroupId("group1")
    .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
    .withWakeupTimeout(10 seconds)
    .withPollTimeout(0.5 seconds)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")

  val source = Consumer.plainSource(consumerSettings, Subscriptions.topics(kafkaTopic))

  val unmarshalFlow = (record: ConsumerRecord[String, String]) => Unmarshal(record.value).to[VehicleList]

  val stmtInsertVehicle = session.prepare("INSERT INTO metro.vehicle(vehicleId, longitude, latitude) VALUES (?,?,?);")
  val stmtInsertVehicleBinder = (vehicle: VehiclePosition, statement: PreparedStatement) => statement.bind()
    .setString(0, vehicle.id)
    .setDouble(1, vehicle.longitude)
    .setDouble(2, vehicle.latitude)
  val sinkVehicle = CassandraSink[VehiclePosition](parallelism = 1, stmtInsertVehicle, stmtInsertVehicleBinder)

  val stmtInsertVehicleTile = session.prepare("INSERT INTO metro.vehicle_tile(tile, vehicle) VALUES (?,?);")
  val stmtInsertVehicleTileBinder = (tileVehicle: (String, VehiclePosition), statement: PreparedStatement) => statement.bind()
    .setString(0, tileVehicle._1)
    .setString(1, tileVehicle._2.id)
  val sinkVehicleTile = CassandraSink[(String, VehiclePosition)](parallelism = 1, stmtInsertVehicleTile, stmtInsertVehicleTileBinder)

  def buildStream = source
    .mapAsync(1)(unmarshalFlow)
    .mapConcat { _.items }
    .alsoTo(sinkVehicle)
    .map{ vehicle => (QuadKey.coordinatesToQuadKey(vehicle.latitude, vehicle.longitude), vehicle) }
    .to(sinkVehicleTile)

  def run = buildStream.run()
}