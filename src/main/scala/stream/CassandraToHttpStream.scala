package stream

import akka.actor.ActorSystem
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.server.{HttpApp, Route}
import akka.stream.ActorMaterializer
import akka.stream.alpakka.cassandra.scaladsl.CassandraSource
import com.datastax.driver.core.{Session, SimpleStatement}
import model._

class CassandraToHttpStream(implicit val system: ActorSystem, implicit val materializer: ActorMaterializer, implicit val session: Session)
  extends HttpApp
    with VehicleConversionSupport
    with StreamSupport {

  implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()

  override protected def routes: Route =
    get {
      pathPrefix("api") {
        pathPrefix("vehicles") {
          path("list") {
            complete(sourceList)
          } ~
          pathPrefix("vehicle" / IntNumber) { vehicleId =>
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

  def sourceList = CassandraSource(new SimpleStatement("SELECT vehicleId, longitude, latitude FROM metro.vehicle"))
    .map(row => VehiclePosition(row.getString(0), row.getDouble(1), row.getDouble(2)))

  def sourceLastPosition(vehicleId: Int) = sourceList.filter(_.id.toInt == vehicleId)

  def sourceTiles = CassandraSource(new SimpleStatement("SELECT tile, vehicle FROM metro.vehicle_tile"))

  def sourceVehiclesInTiles(tiles : Seq[String]) = sourceTiles
    .filter{ row => tiles.contains(row.getString(0)) }
    .map{ row => Vehicle(row.getString("vehicle")) }

  def sourceGroupedTiles = CassandraSource(new SimpleStatement("SELECT tile, count(*) FROM metro.vehicle_tile group by tile"))
    .map{ row => TileVehicles(row.getString(0), row.getLong(1)) }

  def sourceFilledTiles = sourceGroupedTiles.map{ tileVehicle => Tile(tileVehicle.key) }

  def sourceVehiclesCount(tiles : Seq[String]) = sourceGroupedTiles.filter{ tile => tiles.contains(tile.key) }

  def run = startServer("localhost", 8080, system)
}
