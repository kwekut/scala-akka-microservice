package models

import org.joda.time.LocalDateTime
import java.util.UUID
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._
//location,timeslot,time,username,userid,amount,paytoken,rating,like,following,available,picurl,details,
//Action - GoTo, CoundDown, Abuse, Buy, Cancel, QR, Map
case class Booking(
  id: String,
  typ: String,
  activity: String,
  datemy: String,
  datemdy: String,
  position: String,
  timeslot: String,
  userid: String,
  username: String,
  detail: String,
  status: String,
  created: String)

trait Booking extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val itemFormat = jsonFormat12(Booking)
}

//To and From
// val json = Color("CadetBlue", 95, 158, 160).toJson
// val color = json.convertTo[Color]