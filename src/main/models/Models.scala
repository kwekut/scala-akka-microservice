package models

import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._

trait Models extends 
  CassandraReadActor with 
  CassandraWriteActor with 
  CommunicateActor with 
  ElasticActor
// domain model
final case class Item(name: String, id: Long)
final case class Order(items: List[Item])
case class Color(name: String, red: Int, green: Int, blue: Int)
case class IpInfo(query: String, country: Option[String], city: Option[String], lat: Option[Double], lon: Option[Double])
case class IpPairSummaryRequest(ip1: String, ip2: String)
case class IpPairSummary(distance: Option[Double], ip1Info: IpInfo, ip2Info: IpInfo)
// collect your json format instances into a support trait:
trait Models extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val itemFormat = jsonFormat2(Item)
  implicit val orderFormat = jsonFormat1(Order) // contains List[Item]
  implicit val colorFormat = jsonFormat4(Color)
  implicit val ipInfoFormat = jsonFormat5(IpInfo.apply)
  implicit val ipPairSummaryRequestFormat = jsonFormat2(IpPairSummaryRequest.apply)
  implicit val ipPairSummaryFormat = jsonFormat3(IpPairSummary.apply)
}


object IpPairSummary {
  def apply(ip1Info: IpInfo, ip2Info: IpInfo): IpPairSummary = IpPairSummary(calculateDistance(ip1Info, ip2Info), ip1Info, ip2Info)

  private def calculateDistance(ip1Info: IpInfo, ip2Info: IpInfo): Option[Double] = {
    (ip1Info.lat, ip1Info.lon, ip2Info.lat, ip2Info.lon) match {
      case (Some(lat1), Some(lon1), Some(lat2), Some(lon2)) =>
        // see http://www.movable-type.co.uk/scripts/latlong.html
        val φ1 = toRadians(lat1)
        val φ2 = toRadians(lat2)
        val Δφ = toRadians(lat2 - lat1)
        val Δλ = toRadians(lon2 - lon1)
        val a = pow(sin(Δφ / 2), 2) + cos(φ1) * cos(φ2) * pow(sin(Δλ / 2), 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        Option(EarthRadius * c)
      case _ => None
    }
  }
  private val EarthRadius = 6371.0
}


//To and From
// val json = Color("CadetBlue", 95, 158, 160).toJson
// val color = json.convertTo[Color]