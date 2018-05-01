import actors.{Consume, HttpClientActor}
import akka.actor.{ActorSystem, Props}

import scala.concurrent.duration._

object EntryPoint extends App {

  val system = ActorSystem("http-consumer")
  val httpClientActor = system.actorOf(Props[HttpClientActor])

  import system.dispatcher

  system.scheduler.schedule(0 seconds, 3 seconds, httpClientActor, Consume)

  println("and now we wait.")
}
