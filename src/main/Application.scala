package main

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


//The App trait can be used to quickly turn objects into executable programs.
object Application extends App with Actors {
  //import Greeter._
  override implicit val system = ActorSystem()
  override implicit val executor = system.dispatcher
  override implicit val materializer = ActorMaterializer()

  override val config = ConfigFactory.load()
  val host = config.getString("http.host")
  val port = config.getInt("http.port")
  override val logger = Logging(system, getClass)

  Try {
    val bindingFuture = Http().bindAndHandle(routes, interface = host, port = port)
  } match {
    case Success(lines) => println(s"REST interface bound to ${binding.localAddress}")
      bindingFuture
        .flatMap(_.unbind()) // trigger unbinding from the port
        .onComplete(_ ⇒ system.terminate()) // and shutdown when done
    case Failure(ex) => println(s"Problem starting: ${ex.getMessage}"); system.terminate()
  } 



}

