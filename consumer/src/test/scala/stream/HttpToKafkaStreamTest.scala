package stream

import akka.http.scaladsl.model.HttpRequest
import akka.stream.scaladsl.Sink
import akka.testkit.TestProbe
import base.{DockerComposeTag, TestSpec}
import org.scalatest.DoNotDiscover

import scala.concurrent.duration._

@DoNotDiscover
class HttpToKafkaStreamTest extends TestSpec {

  val subject = HttpToKafkaStream

  "The stream" should "read from http source and add json to kafka every two seconds" taggedAs (DockerComposeTag) in {
      val probe = TestProbe()
      val cancellable = subject.source.to(Sink.actorRef(probe.ref, "completed")).run()

      probe.expectMsgType[HttpRequest](2 seconds) // scheduler starts immediately
      probe.expectMsgType[HttpRequest](2 seconds) // rate of 1/2s
      cancellable.cancel()
      probe.expectMsg(2 seconds, "completed")
  }
}
