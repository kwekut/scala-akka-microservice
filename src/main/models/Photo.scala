package models

import org.joda.time.LocalDateTime
import java.util.UUID
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._

case class  Photo(id: String, picid: String, picname: String, picformat: String, 
  typ: String, activity: String, picdesc: String, picurl: String, created: String)

trait Page extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val itemFormat = jsonFormat10(Page)
}