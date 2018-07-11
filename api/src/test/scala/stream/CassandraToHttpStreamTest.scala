package stream

import base.{DockerComposeTag, TestSpec}
import org.scalatest.{ConfigMap, DoNotDiscover}

@DoNotDiscover
class CassandraToHttpStreamTest extends TestSpec {

  val vehicleListResponse = """[{"id":"2","longitude":2.0,"latitude":-2.0},{"id":"1","longitude":1.0,"latitude":-1.0}]"""
  val lastPositionResponse = """[{"id":"1","longitude":1.0,"latitude":-1.0}]"""
  val filledResponse = """["1"]"""
  val availableVehiclesResponse = """["1","2"]"""
  val vehicleCountResponse = """[{"tile":"1","vehicleCount":2}]"""

  var subject = HttpStream

  override def beforeAll(configMap : ConfigMap) = {
    implicit val session = Suite.createCassandraSession(configMap)
  }

  "Endpoint vehicles/list" should "reply with a list of vehicles" taggedAs (DockerComposeTag) in {
    Get("/api/vehicles/list") ~> subject.routes ~> check {
      responseAs[String] shouldEqual vehicleListResponse
    }
  }

  "Endpoint vehicles/lastPosition" should "reply with a vehicle position" taggedAs (DockerComposeTag) in {
    Get("/api/vehicles/vehicle/1/lastPosition") ~> subject.routes ~> check {
      responseAs[String] shouldEqual lastPositionResponse
    }
  }

  "Endpoint tiles/filled" should "reply a list of filled tiles" taggedAs (DockerComposeTag) in {
    Get("/api/tiles/filled") ~> subject.routes ~> check {
      responseAs[String] shouldEqual filledResponse
    }
  }

  "Endpoint tiles/vehicleCount" should "reply a map of tiles to count" taggedAs (DockerComposeTag) in {
    Get("/api/tiles/usecase/vehicleCount?tile_list=1") ~> subject.routes ~> check {
      responseAs[String] shouldEqual vehicleCountResponse
    }
  }
}