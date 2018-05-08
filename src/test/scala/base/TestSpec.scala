package base

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

abstract class TestSpec extends TestKit(ActorSystem("test-system"))
  with WordSpecLike
  with BeforeAndAfterAll {

  val dockerTag = "DockerComposeTag"

  implicit val materializer = ActorMaterializer()

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

}
