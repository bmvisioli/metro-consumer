package stream

import akka.actor.ActorSystem
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.server.{HttpApp, Route}
import akka.stream.ActorMaterializer
import akka.stream.alpakka.cassandra.scaladsl.CassandraSource
import com.datastax.driver.core.{Session, SimpleStatement}
import model._
import scala.collection.JavaConverters._

class CassandraToHttpStream(implicit val system: ActorSystem, implicit val materializer: ActorMaterializer, implicit val session: Session)
  extends HttpApp
    with VehicleConversionSupport
    with StreamSupport {

  implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()

  override def routes: Route =
    get {
      pathPrefix("api") {
        pathPrefix("vehicles") {
          path("list") {
            complete(sourceList)
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

  def sourceList = CassandraSource(new SimpleStatement("SELECT vehicleId, longitude, latitude FROM metro.vehicle;"))
    .map(row => VehiclePosition(row.getString(0), row.getDouble(1), row.getDouble(2)))

  def sourceLastPosition(vehicleId: String) = sourceList.filter(_.id.id == vehicleId)

  def sourceTiles = CassandraSource(new SimpleStatement("SELECT tile, vehicles FROM metro.tile_vehicles;"))

  def sourceVehiclesInTiles(tiles : Seq[String]) = sourceTiles
    .filter{ row => tiles.contains(row.getString(0)) }
    .mapConcat{ row => row.getSet("vehicles", classOf[String]).asScala.toSet.map(id => Vehicle(id)) }

  def sourceGroupedTiles = sourceTiles
    .map{ row => TileVehicles(row.getString(0), row.getSet(1, classOf[String]).size()) }

  def sourceFilledTiles = sourceGroupedTiles.map{ _.tile }

  def sourceVehiclesCount(tiles : Seq[String]) = sourceGroupedTiles.filter{ vc => tiles.contains(vc.tile.key) }

  def run = startServer("localhost", 8080, system)
}
