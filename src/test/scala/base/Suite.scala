package base

import com.datastax.driver.core.{Cluster, Session}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterAllConfigMap, ConfigMap, Suites}
import stream.CassandraToHttpStreamTest

import scala.io.Source

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
    //continually(Try(Suite.createCassandraSession(configMap))).takeWhile{ _.isFailure }.head
    Thread.sleep(10000) // Cassandra docker image returns before starting fully!!!!
    populateDatabase(Suite.createCassandraSession(configMap))
  }

  override def afterAll(configMap : ConfigMap) {
    //TestKit.shutdownActorSystem(system)
  }

  def populateDatabase(session : Session) {
    Source.fromResource("cassandra-setup")
      .getLines()
      .filterNot( _.startsWith("--") )
      .filterNot( _.isEmpty )
      .foreach{ session.execute }
  }

}
