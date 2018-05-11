package stream

import akka.actor.ActorSystem
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.stream.alpakka.cassandra.scaladsl.CassandraSink
import akka.stream.scaladsl.{Flow, Source}
import com.datastax.driver.core.{PreparedStatement, Session}
import model._
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.apache.kafka.common.serialization.StringDeserializer

import scala.concurrent.duration._
import scala.collection.JavaConverters._


class KafkaToCassandraStream(implicit val system: ActorSystem, implicit val materializer: ActorMaterializer, implicit val session: Session)
  extends VehicleConversionSupport
    with StreamSupport {

  val kafkaUrl = config.getString("kafka.host") + ":" + config.getInt("kafka.port")
  val kafkaTopic = config.getString("kafka.topic")

  def consumerSettings(groupId : String) = ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
    .withBootstrapServers(kafkaUrl)
    .withGroupId(groupId)
    .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
    .withWakeupTimeout(10 seconds)
    .withPollTimeout(0.5 seconds)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")

  def source(groupId : String) = Consumer.plainSource(consumerSettings(groupId), Subscriptions.topics(kafkaTopic))

  val unmarshalFlow = (record: ConsumerRecord[String, String]) => Unmarshal(record.value).to[VehicleList]

  val stmtInsertVehicle = session.prepare("INSERT INTO metro.vehicle(vehicleId, longitude, latitude) VALUES (?,?,?);")
  val stmtInsertVehicleBinder = (vp: VehiclePosition, statement: PreparedStatement) => statement.bind()
    .setString(0, vp.id)
    .setDouble(1, vp.longitude)
    .setDouble(2, vp.latitude)
  val sinkVehicle = CassandraSink[VehiclePosition](parallelism = 1, stmtInsertVehicle, stmtInsertVehicleBinder)

  val stmtInsertVehicleTile = session.prepare("INSERT INTO metro.tile_vehicles(tile, vehicles) VALUES(?,?) USING TTL 2000;")
  val stmtInsertVehicleTileBinder = (tileVehicles: (Tile, Set[String]), statement: PreparedStatement) => statement.bind()
    .setString(0, tileVehicles._1)
    .setSet(1, tileVehicles._2.asJava)
  val sinkVehicleTile = CassandraSink[(Tile, Set[String])](parallelism = 1, stmtInsertVehicleTile, stmtInsertVehicleTileBinder)

  def buildStream = source("group1")
    .mapAsync(1)(unmarshalFlow)
    .alsoTo(Flow[VehicleList].mapConcat(_.items).to(sinkVehicle))
    .mapConcat{ list =>
      list.items
        .map{ vehicle => Tile(QuadKey.coordinatesToQuadKey(vehicle.latitude, vehicle.longitude)) -> vehicle }
        .groupBy(_._1)
        .map{ entry => (entry._1, entry._2.map{ _._2.id.id }) }
        .toSet
    }
    .to(sinkVehicleTile)

  def run = buildStream.run()
}