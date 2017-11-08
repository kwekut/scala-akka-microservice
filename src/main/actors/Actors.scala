package actors

import akka.actor.ActorSystem
import akka.event.{LoggingAdapter, Logging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.{HttpResponse, HttpRequest}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import java.io.IOException
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.math._
import spray.json.DefaultJsonProtocol
import scala.util.Try


trait Actors extends 
  CassandraReadActor with 
  CassandraWriteActor with 
  CommunicateActor with 
  ElasticActor {

  override implicit val system = ActorSystem()
  override implicit val executor = system.dispatcher
  override implicit val materializer = ActorMaterializer()

  Try {
    val maintenanceActor: ActorRef = system.actorOf(MaintainanceActor.props("Good day", printer), "maintenanceActor")
    val elasticActor: ActorRef = system.actorOf(ElasticActor.props("Howdy", maintenanceActor), "elasticActor")
    val commActor: ActorRef = system.actorOf(CommunicateActor.props("Hello", maintenanceActor), "commActor")
    val cassReadActor: ActorRef = system.actorOf(CassandraReadActor.props("Good day", maintenanceActor), "cassReadActor")
    val cassWriteActor: ActorRef = system.actorOf(CassandraWriteActor.props("Good day", maintenanceActor), "cassWriteActor")
  } match {
    case Success(lines) => println(s"Actors starting")
    case Failure(ex) => println(s"Problem starting: ${ex.getMessage}"); system.terminate()
  } 
  }
