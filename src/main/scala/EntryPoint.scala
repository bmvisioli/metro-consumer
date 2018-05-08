import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.datastax.driver.core.Cluster
import stream.{StreamSupport, CassandraToHttpStream, HttpToKafkaStream, KafkaToCassandraStream}

object EntryPoint extends App with StreamSupport {

  implicit val system = ActorSystem("http-consumer")
  implicit val materializer = ActorMaterializer()

  val cassandraHost = config.getString("cassandra.host")
  val cassandraPort = config.getInt("cassandra.port")

  implicit val session = Cluster.builder
    .addContactPoint(cassandraHost)
    .withPort(cassandraPort)
    .build
    .connect()

  def run {
    new HttpToKafkaStream().run
    new KafkaToCassandraStream().run
    new CassandraToHttpStream().run
  }

  run

}
