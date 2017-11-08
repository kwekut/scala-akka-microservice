package controllers

import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._
import akka.actor.{Actor, ActorSystem, Props, ActorLogging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity }
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import spray.json.DefaultJsonProtocol._
import scala.concurrent.duration._
import scala.io.StdIn

class Controller extends Directives with Models with Actors {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

//Home(index,health)
//Consumer()
//Merchant(create,update,delete)
//Auth(signin,signup,signout)
//(getprofile,updateprofile,getpayment,updatepayment)
//Authenticate()

  // format: OFF
  val route: Route =
    get {
      pathSingleSlash {
        complete(Item("thing", 42)) // will render as JSON
      }
    } ~
    post {
      entity(as[Order]) { order => // will unmarshal JSON to Order
        val itemsCount = order.items.size
        val itemNames = order.items.map(_.name).mkString(", ")
        complete(s"Ordered $itemsCount items: $itemNames")
      }
    } ~
    get {
      pathPrefix("item" / LongNumber) { id =>
        // there might be no item for a given id
        val maybeItem: Future[Option[Item]] = fetchItem(id)
        onSuccess(maybeItem) {
          case Some(item) => complete(item)
          case None       => complete(StatusCodes.NotFound)
        }
      }
    } ~
    post {
      path("create-order") {
        entity(as[Order]) { order =>
          val saved: Future[Done] = saveOrder(order)
          onComplete(saved) { done =>
            complete("order created")
          }
        }
      }
    } ~
    path("auction") {
      put {
        parameter("bid".as[Int], "user") { (bid, user) =>
          // place a bid, fire-and-forget
          auctionActor ! Bid(user, bid)
          complete((StatusCodes.Accepted, "bid placed"))
        }
      } ~
      get {
        implicit val timeout: Timeout = 5.seconds
        // query the actor for the current auction state
        val bids: Future[Bids] = (auctionActor ? GetBids).mapTo[Bids]
        complete(bids)
      }
    } ~


    pathEnd {
      post {
        entity(as[Question]) { question =>
          completeWithLocationHeader(
            resourceId = questionService.createQuestion(question),
            ifDefinedStatus = 201, ifEmptyStatus = 409)
          }
        }
    } ~
    
    path(Segment) { id =>
      get {
        complete(questionService.getQuestion(id))
      } ~
      put {
        entity(as[QuestionUpdate]) { update =>
          complete(questionService.updateQuestion(id, update))
        }
      } ~
      delete {
        complete(questionService.deleteQuestion(id))
      }
    }

    logRequestResult("akka-http-microservice") {
      pathPrefix("ip") {
        (get & path(Segment)) { ip =>
          complete {
            fetchIpInfo(ip).map[ToResponseMarshallable] {
              case Right(ipInfo) => ipInfo
              case Left(errorMessage) => BadRequest -> errorMessage
            }
          }
        } ~
        (post & entity(as[IpPairSummaryRequest])) { ipPairSummaryRequest =>
          complete {
            val ip1InfoFuture = fetchIpInfo(ipPairSummaryRequest.ip1)
            val ip2InfoFuture = fetchIpInfo(ipPairSummaryRequest.ip2)
            ip1InfoFuture.zip(ip2InfoFuture).map[ToResponseMarshallable] {
              case (Right(info1), Right(info2)) => IpPairSummary(info1, info2)
              case (Left(errorMessage), _) => BadRequest -> errorMessage
              case (_, Left(errorMessage)) => BadRequest -> errorMessage
            }
          }
        }
      }
    }
  // format: ON
}


trait MyResource extends Directives with JsonSupport {

  implicit def executionContext: ExecutionContext

  def completeWithLocationHeader[T](resourceId: Future[Option[T]], ifDefinedStatus: Int, ifEmptyStatus: Int): Route =
    onSuccess(resourceId) {
      case Some(t) => completeWithLocationHeader(ifDefinedStatus, t)
      case None => complete(ifEmptyStatus, None)
    }

  def completeWithLocationHeader[T](status: Int, resourceId: T): Route =
    extractRequestContext { requestContext =>
      val request = requestContext.request
      val location = request.uri.copy(path = request.uri.path / resourceId.toString)
      respondWithHeader(Location(location)) {
        complete(status, None)
      }
    }

  def complete[T: ToResponseMarshaller](resource: Future[Option[T]]): Route =
    onSuccess(resource) {
      case Some(t) => complete(ToResponseMarshallable(t))
      case None => complete(404, None)
    }

  def complete(resource: Future[Unit]): Route = onSuccess(resource) { complete(204, None) }

}


Put
val route = put { complete("This is a PUT request.") }
Put("/", "put content") ~> route ~> check {
  responseAs[String] shouldEqual "This is a PUT request."
}

Post
val route = post { complete("This is a POST request.") }
Post("/", "post content") ~> route ~> check {
  responseAs[String] shouldEqual "This is a POST request."
}

Patch
val route = patch { complete("This is a PATCH request.") }
Patch("/", "patch content") ~> route ~> check {
  responseAs[String] shouldEqual "This is a PATCH request."
}

Get
val route = get { complete("This is a GET request.") }
Get("/") ~> route ~> check {
  responseAs[String] shouldEqual "This is a GET request."
}

Delete
val route = delete { complete("This is a DELETE request.") }
Delete("/") ~> route ~> check {
  responseAs[String] shouldEqual "This is a DELETE request."
}

Head
val route = head { complete("This is a HEAD request.") }
Head("/") ~> route ~> check {
  responseAs[String] shouldEqual "This is a HEAD request."
}

Options
val route = options { complete("This is an OPTIONS request.") }
Options("/") ~> route ~> check {
  responseAs[String] shouldEqual "This is an OPTIONS request."
}



Method
val route = method(HttpMethods.PUT) { complete("This is a PUT request.") }
Put("/", "put content") ~> route ~> check {
  responseAs[String] shouldEqual "This is a PUT request."
}
Get("/") ~> Route.seal(route) ~> check {
  status shouldEqual StatusCodes.MethodNotAllowed
  responseAs[String] shouldEqual "HTTP method not allowed, supported methods: PUT"
}

ExtractMethod
val route =
  get {
    complete("This is a GET request.")
  } ~
    extractMethod { method =>
      complete(s"This ${method.name} request, clearly is not a GET!")
    }
Get("/") ~> route ~> check {
  responseAs[String] shouldEqual "This is a GET request."
}
Put("/") ~> route ~> check {
  responseAs[String] shouldEqual "This PUT request, clearly is not a GET!"
}
Head("/") ~> route ~> check {
  responseAs[String] shouldEqual "This HEAD request, clearly is not a GET!"
}


////////////////////////////////////////////////////////////////////////////
def myUserPassAuthenticator(credentials: Credentials): Option[String] =
  credentials match {
    case p @ Credentials.Provided(id) if p.verify("p4ssw0rd") => Some(id)
    case _ => None
  }
val route =
  Route.seal {
    path("secured") {
      authenticateBasic(realm = "secure site", myUserPassAuthenticator) { userName =>
        complete(s"The user is '$userName'")
      }
    }
  }

// tests:
Get("/secured") ~> route ~> check {
  status shouldEqual StatusCodes.Unauthorized
  responseAs[String] shouldEqual "The resource requires authentication, which was not supplied with the request"
  header[`WWW-Authenticate`].get.challenges.head shouldEqual HttpChallenge("Basic", Some("secure site"), Map("charset" → "UTF-8"))
}
val validCredentials = BasicHttpCredentials("John", "p4ssw0rd")
Get("/secured") ~> addCredentials(validCredentials) ~> // adds Authorization header
  route ~> check {
    responseAs[String] shouldEqual "The user is 'John'"
  }

val invalidCredentials = BasicHttpCredentials("Peter", "pan")
Get("/secured") ~>
  addCredentials(invalidCredentials) ~> // adds Authorization header
  route ~> check {
    status shouldEqual StatusCodes.Unauthorized
    responseAs[String] shouldEqual "The supplied authentication is invalid"
    header[`WWW-Authenticate`].get.challenges.head shouldEqual HttpChallenge("Basic", Some("secure site"), Map("charset" → "UTF-8"))
  }

////////////////////////////////////////////////////////////////////////////////////////////////
case class User(name: String)
def myUserPassAuthenticator(credentials: Credentials): Option[User] =
  credentials match {
    case Credentials.Provided(id) => Some(User(id))
    case _                        => None
  }
val admins = Set("Peter")
def hasAdminPermissions(user: User): Boolean = admins.contains(user.name)
// check if user is authorized to perform admin actions:

val route =
  Route.seal {
    authenticateBasic(realm = "secure site", myUserPassAuthenticator) { user =>
      path("peters-lair") {
        authorize(hasAdminPermissions(user)) {
          complete(s"'${user.name}' visited Peter's lair")
        }
      }
    }
  }

////////////////////////////// tests:
val johnsCred = BasicHttpCredentials("John", "p4ssw0rd")
Get("/peters-lair") ~> addCredentials(johnsCred) ~> // adds Authorization header
  route ~> check {
    status shouldEqual StatusCodes.Forbidden
    responseAs[String] shouldEqual "The supplied authentication is not authorized to access this resource"
  }

val petersCred = BasicHttpCredentials("Peter", "pan")
Get("/peters-lair") ~> addCredentials(petersCred) ~> // adds Authorization header
  route ~> check {
    responseAs[String] shouldEqual "'Peter' visited Peter's lair"
  }

/////////////////////////////////////////////////////////////////////////////////////////////////
def greeter: Flow[Message, Message, Any] = Flow[Message].mapConcat {
    case tm: TextMessage =>
      TextMessage(Source.single("Hello ") ++ tm.textStream ++ Source.single("!")) :: Nil
    case bm: BinaryMessage =>
      // ignore binary messages but drain content to avoid the stream being clogged
      bm.dataStream.runWith(Sink.ignore)
      Nil
  }
val websocketRoute =
  path("greeter") {
    handleWebSocketMessages(greeter)
  }

// tests:
// create a testing probe representing the client-side
val wsClient = WSProbe()
// WS creates a WebSocket request for testing
WS("/greeter", wsClient.flow) ~> websocketRoute ~>
  check {
    // check response for WS Upgrade headers
    isWebSocketUpgrade shouldEqual true
    // manually run a WS conversation
    wsClient.sendMessage("Peter")
    wsClient.expectMessage("Hello Peter!")
    wsClient.sendMessage(BinaryMessage(ByteString("abcdef")))
    wsClient.expectNoMessage(100.millis)
    wsClient.sendMessage("John")
    wsClient.expectMessage("Hello John!")
    wsClient.sendCompletion()
    wsClient.expectCompletion()
  }