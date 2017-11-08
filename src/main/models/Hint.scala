package models

import org.joda.time.LocalDateTime
import java.util.UUID
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._

case class SparkToSocHints(activity: String,hints: List[Hint])
case class Hint(
  id: String,
  activity: String,
  typ: String,
  userid: String,
  lat: String,
  lng: String,
  hint: String,
  created: String)

trait Hint extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val itemFormat = jsonFormat8(Hint)
}
