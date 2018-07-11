package stream

import com.typesafe.config.ConfigFactory
import model.VehicleConversionSupport

trait StreamSupport extends VehicleConversionSupport {

  @transient
  val config = ConfigFactory.load()

}