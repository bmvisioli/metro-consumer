package stream

import scala.collection.JavaConverters._

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.server.{HttpApp, Route}
import akka.stream.alpakka.cassandra.scaladsl.CassandraSource
import akka.stream.scaladsl.{GraphDSL, MergePreferred, Source}
import akka.stream.{ActorMaterializer, SourceShape}
import com.datastax.driver.core.{Cluster, SimpleStatement}
import model._

object HttpStream extends HttpApp
  with App
  with StreamSupport {

  implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()

  implicit val system = ActorSystem("http-consumer")
  implicit val materializer = ActorMaterializer()

  val cassandraHost = config.getString("cassandra.host")
  val cassandraPort = config.getInt("cassandra.port")

  implicit val session = Cluster.builder
    .addContactPoint(cassandraHost)
    .withPort(cassandraPort)
    .build
    .connect()

  override def routes: Route =
    get {
      pathPrefix("api") {
        pathPrefix("vehicles") {
          path("list") {
            complete(sourceListKafka)
          } ~
            pathPrefix("vehicle" / ".*".r) { vehicleId =>
              path("lastPosition") {
                complete(sourceLastPosition(vehicleId))
              }
            }
        } ~
          pathPrefix("tiles") {
            path("filled") {
              complete(sourceFilledTiles)
            } ~
              pathPrefix("tile" / ".*".r) { tile =>
                path("availableVehicles") {
                  complete(sourceVehiclesInTiles(Seq(tile)))
                }
              } ~
              pathPrefix("usecase" / "vehicleCount") {
                parameter("tile_list") { listStr =>
                  complete(sourceVehiclesCount(listStr.split(",")))
                }
              }
          }
      }
    }

  val sourceListCassandra = CassandraSource(new SimpleStatement("SELECT vehicleId, longitude, latitude FROM metro.vehicle;"))
    .map(row => VehiclePosition(row.getString(0), row.getDouble(1), row.getDouble(2)))

  val sourceListKafka = new KafkaVehicleStream().buildStream.take(1)
      .map{elem => println("yeah"); elem}
      .mapConcat(_.items)

  val sourceListMerged = Source.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val merge = builder.add(MergePreferred[VehiclePosition](1, true))

      sourceListKafka ~>  merge.preferred
      sourceListCassandra ~>  merge.in(0)

      SourceShape(merge.out)
  })

  def sourceLastPosition(vehicleId: String) = sourceListCassandra.filter(_.id.id == vehicleId)

  val sourceTiles = CassandraSource(new SimpleStatement("SELECT tile, vehicles FROM metro.tile_vehicles;"))

  def sourceVehiclesInTiles(tiles: Seq[String]) = sourceTiles
    .filter { row => tiles.contains(row.getString(0)) }
    .mapConcat { row => row.getSet("vehicles", classOf[String]).asScala.toSet.map { id: String => Vehicle(id) } }

  val sourceGroupedTiles = sourceTiles
    .map { row => TileVehicles(row.getString(0), row.getSet(1, classOf[String]).size()) }

  val sourceFilledTiles = sourceGroupedTiles.map {
    _.tile
  }

  def sourceVehiclesCount(tiles: Seq[String]) = sourceGroupedTiles.filter { vc => tiles.contains(vc.tile.key) }

  //Run
  startServer("localhost", 8080, system)
}
