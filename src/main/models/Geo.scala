package models

import org.joda.time.LocalDateTime
import java.util.UUID
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._
//import play.extras.geojson._


case class BoundingBox(southWest: LatLng, northEast: LatLng) {
	require(southWest.lat < northEast.lat, "South west bound point is north of north east point")
}

trait BoundingBox extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val itemFormat = jsonFormat2(BoundingBox)
}
