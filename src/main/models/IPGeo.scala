package models

import javax.inject.Inject
import scala.concurrent.{ Future, ExecutionContext }
import org.joda.time.LocalDateTime
import java.util.UUID
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._
import scala.collection.JavaConverters._
import com.typesafe.config.ConfigFactory
import scala.util.Try
import scala.util.{Success, Failure}
import java.util.UUID
import scala.concurrent.duration._
import scala.concurrent.{ Future, ExecutionContext }
import models.mail._


object IPGeo {
  private val cong = ConfigFactory.load()
  private val key: String = cong.getString("crypto.key")
}


//class IPGeo @Inject() (ws: WSClient)
case class IPGeo @Inject()(ws: WSClient) {
  import IPGeo._

  def location(ipAddress:String) = {
    val url = "http://api.ipinfodb.com/v3/ip-city"
    val complexResponse = ws.url(url)
      .withRequestTimeout(10000.millis)
      .withQueryString("key" -> key, "ip" -> ipAddress)
      .get()
    val res = complexResponse map { response => 
        Location(
          (response.json \ "latitude").as[String],
          (response.json \ "longitude").as[String]
       ) 
    }
    res
  }
}