package models.daos.core

import com.datastax.driver.core.{ProtocolOptions, Cluster}
//import com.datastax.driver.CassandraOnMesos
import com.typesafe.config.ConfigFactory
import play.api.Configuration
import akka.actor.ActorSystem
import scala.collection.JavaConverters._
import play.api.Logger

//object CassandraCluster extends ConfigCassandraCluster{}

object CassandraCluster {

  private val cfg = ConfigFactory.load()
  private val port = cfg.getInt("cassandra.port")
  private val hosts = cfg.getStringList("cassandra.hosts")
  private val cpoints = cfg.getInt("cassandra.numberOfContactPoints")
  private val httpServerBaseUri = cfg.getString("cassandra.httpServerBaseUri")
  private val shosts = hosts.asScala
  lazy val cluster = Cluster.builder.addContactPoint(hosts.head).withPort(port).build
}


   

