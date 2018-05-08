package stream

import com.typesafe.config.ConfigFactory

trait StreamSupport {

  val config = ConfigFactory.load()

  def run

}
