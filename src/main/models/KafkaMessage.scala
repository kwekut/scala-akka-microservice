package models

import org.joda.time.LocalDateTime
import java.util.UUID
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._

//scala Kafka model
case class  KafkaMessage(
  ids: Seq[String], source: String, priority: String, 
  timetolive: Int, clickaction: String, msg: Msg)

object KafkaMessage{
  implicit lazy val kmsgFormat = Jsonx.formatCaseClass[KafkaMessage]
}

case class PersistKmsgs(kmsg: KafkaMessage)
case class JsonProduce(user: User, topicName: String, kmsg: KafkaMessage)