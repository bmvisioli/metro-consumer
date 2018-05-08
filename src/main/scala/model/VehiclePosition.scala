package model

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

import scala.collection.immutable.Seq

case class VehicleList(items: Seq[VehiclePosition])

case class Vehicle(val id: String)

case class VehiclePosition(val id: String, val longitude: Double, val latitude: Double)

case class Tile(val key: String)

case class TileVehicles(val key: String, val vehicleCount : Long)

trait VehicleConversionSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val vehiclePositionFormat = jsonFormat3(VehiclePosition)
  implicit val vehicleListFormat = jsonFormat1(VehicleList)
  implicit val tileVehicleFormat = jsonFormat2(TileVehicles)
  implicit val tileFormat = jsonFormat1(Tile)
  implicit val vehicleFormat = jsonFormat1(Vehicle)
}