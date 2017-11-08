package models

import java.util.UUID
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._
import akka.actor.ActorRef
//Scala Cassandra model

case class TestItem(database: String, table: String, key: String, conditions: Set[String])

case class SocToSparkUsrMsg(activity: String, out: ActorRef, user: User, msg: Msg)

case class SocToSparkUsrLnk(activity: String, out: ActorRef, user: User, lnk: Links)

case class SparkToSocUsrFeeds(activity: String, out: ActorRef, sender: ActorRef, user: User, feeds: List[Feed])

//case class SocToSparkUsrSrh(activity: String, out: ActorRef, user: User, srh: Searcher)
case class SocToSparkUsrIp(activity: String, out: ActorRef, user: User, ip: String, x: PaymentUser)

case class SocToSparkUsrUsr(activity: String, out: ActorRef, user: User, usr: Usr)

case class SocToSparkUsrPic(activity: String, out: ActorRef, user: User, photo: Photo)

case class SocToSparkUsrRev(activity: String, out: ActorRef, user: User, rev: Review)

case class SocToSparkUsrShp(activity: String, out: ActorRef, user: User, shp: Shp)

case class SocToSparkCurrentRegions(activity: String, out: ActorRef, shipper: ActorRef, user: User, shopids: Set[String])

case class SocToProcUsrMsg(activity: String, out: ActorRef, user: User, msg: Msg)

case class ProcToSparkUsrMsg(activity: String, out: ActorRef, shipper: ActorRef, user: User, msg: Msg)

case class SparkToProcUsrMsgShop(activity: String, out: ActorRef, user: User, shop: Shop, msg: Msg)

case class SparkToProcUsrMsgShopusr(activity: String, out: ActorRef, user: User, shop: User, msg: Msg)

case class SparkToProcUsrMsgPay(activity: String, out: ActorRef, user: User, msg: Msg, cus: Payment, vdr: Payment)

case class SparkToShopRegsShpStr(activity: String, allShops: Array[String])

case class ShopRegsToSpark(activity: String, shipper: ActorRef)

case class AuthAuth(activity: String, auth: String)

case class AuthAuthFind(authid: String)

case class AuthAuthSave(authid: String, auth: String)

case class AuthAuthRemove(authid: String)

case class AuthIdentityFind(providerID: String, providerKey: String)

case class IdentityFind(userid: String)

case class AuthIdentitySave(userid: String, user: String)
