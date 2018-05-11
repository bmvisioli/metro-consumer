package model

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, DeserializationException, JsString, JsValue, RootJsonFormat}

case class VehicleList(items: Set[VehiclePosition])

case class Vehicle(id: String) extends AnyVal

case class VehiclePosition(id: Vehicle, longitude: Double, latitude: Double)

case class Tile(key: String) extends AnyVal

case class TileVehicles(tile: Tile, vehicleCount: Long)

trait VehicleConversionSupport extends SprayJsonSupport with DefaultJsonProtocol {

  implicit def stringToTile(key: String) = Tile(key)
  implicit def tileToString(tile: Tile) = tile.key
  implicit object TileFormat extends RootJsonFormat[Tile] {
    def write(tile: Tile) = JsString(tile.key)
    def read(value: JsValue) = value match {
      case JsString(key) => Tile(key)
      case _ => throw new DeserializationException("Tile expected")
    }
  }

  implicit def stringToVehicle(id: String) = Vehicle(id)
  implicit def vehicleToString(vehicle: Vehicle) = vehicle.id
  implicit object VehicleFormat extends RootJsonFormat[Vehicle] {
    def write(vehicle: Vehicle) = JsString(vehicle.id)
    def read(value: JsValue) = value match {
      case JsString(id) => Vehicle(id)
      case _ => throw new DeserializationException("Vehicle expected")
    }
  }

  implicit val vehiclePositionFormat = jsonFormat3(VehiclePosition)
  implicit val vehicleListFormat = jsonFormat1(VehicleList)
  implicit val tileVehicleFormat = jsonFormat2(TileVehicles)

}