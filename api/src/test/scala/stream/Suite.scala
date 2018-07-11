package stream

import com.datastax.driver.core.{Cluster, Session}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterAllConfigMap, ConfigMap, Suites}

import scala.collection.Iterator.continually
import scala.io.Source
import scala.util.Try

object Suite {
  def createCassandraSession(configMap : ConfigMap) = {
    val Array(cassandraHost, cassandraPort) = configMap.getRequired[String]("cassandra:9042").split(":")
    Cluster.builder
      .addContactPoint(cassandraHost)
      .withPort(cassandraPort.toInt)
      .build
      .connect()
  }
}

class Suite extends Suites(new CassandraToHttpStreamTest)
  with BeforeAndAfterAll
  with BeforeAndAfterAllConfigMap {

  override val invokeBeforeAllAndAfterAllEvenIfNoTestsAreExpected = true

  override def beforeAll(configMap : ConfigMap) {
    continually{Thread.sleep(500); Try(Suite.createCassandraSession(configMap))}
      .takeWhile{ _.isFailure }
      .foreach{ _ => println("waiting for cassandra...") }
    populateDatabase(Suite.createCassandraSession(configMap))
  }

  override def afterAll(configMap : ConfigMap) {
    //TestKit.shutdownActorSystem(system)
  }

  def populateDatabase(session : Session) {
    Source
      .fromFile("src/test/resources/cassandra-setup")
      .getLines()
      .filterNot( _.startsWith("--") )
      .filterNot( _.isEmpty )
      .foreach{ session.execute }
  }

}
