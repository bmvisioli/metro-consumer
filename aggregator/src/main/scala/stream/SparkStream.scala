package stream

import scala.concurrent.duration._

import com.datastax.spark.connector.SomeColumns
import com.datastax.spark.connector.streaming.toDStreamFunctions
import com.datastax.spark.connector.writer.{TTLOption, WriteConf}
import kafka.serializer.{DefaultDecoder, StringDecoder}
import model._
import org.apache.kafka.clients.consumer.ConsumerConfig._
import org.apache.spark.SparkConf
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}
import spray.json._
import stream.SparkStream._

class SparkStream extends Serializable {
  KafkaUtils.createDirectStream[String, Array[Byte], StringDecoder, DefaultDecoder](ssc, kafkaParams, Set(kafkaTopic))
    .map{ entry => unmarshal(entry._2) }
    .flatMap{ _.items.map{ vehicle => QuadKey.coordinatesToQuadKey(vehicle.latitude, vehicle.longitude) -> vehicle.id.id} }
    //TODO Change for a reduceByKey and save it grouped
    .groupByKey()
    .saveToCassandra("metro", "tile_vehicles", SomeColumns("tile","vehicles"), writeConf)
}

object SparkStream extends StreamSupport {

  val kafkaUrl = config.getString("kafka.host") + ":" + config.getInt("kafka.port")
  val kafkaTopic = config.getString("kafka.topic")

  val writeConf = new WriteConf(ttl = TTLOption.constant(2 seconds))
  val kafkaParams = Map(BOOTSTRAP_SERVERS_CONFIG -> kafkaUrl, GROUP_ID_CONFIG -> "group2")

  val sparkConf = new SparkConf()
    .setMaster("local[4]")
    .setAppName("metro-consumer")
  implicit val ssc = new StreamingContext(sparkConf, Seconds(2))

  def unmarshal(message: Array[Byte]) = new String(message).parseJson.convertTo[VehicleList]

  def main(args: Array[String]) {
    new SparkStream
    ssc.start()
    ssc.awaitTermination()
  }
}