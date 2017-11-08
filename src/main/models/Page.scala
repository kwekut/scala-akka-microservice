package models

import org.joda.time.LocalDateTime
import java.util.UUID
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._

case class Page(
  id: String,
  userid: String,
  shopid: String,
  feedid: String,
  revid: String,
  typ: String,
  activity: String,
  range: String,
  page: String,
  filter: String)

trait Page extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val itemFormat = jsonFormat10(Page)
}
