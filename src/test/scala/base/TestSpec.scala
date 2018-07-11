package base

import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.ActorMaterializer
import akka.testkit.TestKitBase
import org.scalatest._

object DockerComposeTag extends Tag("DockerComposeTag")

abstract class TestSpec extends FlatSpec
  with ScalatestRouteTest
  with TestKitBase
  with Matchers
  with BeforeAndAfterAllConfigMap {

  override implicit val materializer = ActorMaterializer(namePrefix = Some("materializer"))(system)

  override val invokeBeforeAllAndAfterAllEvenIfNoTestsAreExpected = false

}
