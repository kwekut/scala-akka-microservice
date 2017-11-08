package models

import org.joda.time.LocalDateTime
import java.util.UUID
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._

//Used by client to send simple messages to server
// LikeShop/ LikeProd/ MarkRead/ Follow/ Unfollow/ Followings/ Followers/ DeleteFeed
// SeeShop/ SeeShopProds/ Profile/ History
case class Links(
  id: String, //UUID as String
  userid: String,
  shopid: String,
  activity: String,
  feedid: String,
  pagination: String,
  filter: String)

trait Links extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val itemFormat = jsonFormat7(Links)
}