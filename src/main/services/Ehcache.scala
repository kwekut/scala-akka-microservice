//Create cache by Auth Service, and push (JWT,TTL,ID,USER) to cache
//https://blog.knoldus.com/2014/03/21/how-to-integrate-ehcache-with-scala/
//https://github.com/eigengo/akka-patterns/tree/master/server/api/src/main/scala/org/eigengo/akkapatterns/api
var manager: net.sf.ehcache.CacheManager = CacheManager.newInstance("src/main/resources/ehcache.xml")
val cacheConfiguration = new CacheConfiguration("cacheRegionName", 1000).eternal(true)
	.persistence(newPersistenceConfiguration().strategy(Strategy.LOCALRESTARTABLE))
 val cacheRegion = new Cache(cacheConfiguration)
 manager.addCache(cacheRegion)


//Use cache as database for Authentication for all services except Auth App which feeds it
 // create CacheManager
var cacheManager: net.sf.ehcache.CacheManager = CacheManager.newInstance("src/main/resources/ehcache.xml")
val cacheConfiguration = new CacheConfiguration("cacheRegionName", 1000).eternal(true)
	.persistence(newPersistenceConfiguration().strategy(Strategy.LOCALRESTARTABLE))
 
 // create cache region
 val cacheRegion = new Cache(cacheConfiguration)
 cacheManager.addCache(cacheRegion)
 val testCacheRegion = cacheManager.getCache("cacheRegionName")
 testCacheRegion.put(new Element("key", "payload"))
 val getValue = testCacheRegion.get("key").getValue()
 println(" value from the cache :" , getValue)



//////////////////////////////////////////////////////////////////////////////////////////////

trait AuthenticationDirectives {  this: HttpService =>

  def doAuthenticate(token: UUID): Future[Option[UserDetailT[_]]]

  private def doValidUser[A <: UserKind](map: UserDetailT[_] => Authentication[UserDetailT[A]]): RequestContext => Future[Authentication[UserDetailT[A]]] = {
    ctx: RequestContext =>
      getToken(ctx.request) match {
        case None => Future(Left(AuthenticationRequiredRejection("https", "patterns")))
        case Some(token) => doAuthenticate(token) .map {
          case Some(user) => map(user)
          case None       => Left(AuthenticationFailedRejection("Patterns"))
        }
      }
  }

  val uuidRegex = """^\p{XDigit}{8}(-\p{XDigit}{4}){3}-\p{XDigit}{12}$""".r
  def isUuid(token: String) = token.length == 36 && uuidRegex.findPrefixOf(token).isDefined

  def getToken(request: HttpRequest): Option[UUID] = {
    val query = request.queryParams.get("token")
    if (query.isDefined && isUuid(query.get))
      Some(UUID.fromString(query.get))
    else {
      val header = request.headers.find(_.name == "x-token")
      if (header.isDefined && isUuid(header.get.value))
        Some(UUID.fromString(header.get.value))
      else
        None
    }
  }

  //Checks that the token represents a valid user; i.e. someone is logged in. We make no assumptions about the roles
  def validUser: RequestContext => Future[Authentication[UserDetail]] = doValidUser(x => Right(x.asInstanceOf[UserDetailT[UserKind]]))

  def validSuperuser: RequestContext => Future[Authentication[UserDetailT[SuperuserKind.type]]] =
    doValidUser { udc: UserDetailT[_] =>
      udc.kind match {
        case SuperuserKind => Right(new UserDetailT(udc.userReference, SuperuserKind))
        case _ => Left(AuthenticationFailedRejection("Akka-Patterns"))
      }
    }

  def validCustomer: RequestContext => Future[Authentication[UserDetailT[CustomerUserKind]]] = {
    doValidUser { udc: UserDetailT[_] =>
      udc.kind match {
        case k: CustomerUserKind => Right(new UserDetailT(udc.userReference, k))
        case _ => Left(AuthenticationFailedRejection("Akka-Patterns"))
      }
    }
  }
}


trait DefaultAuthenticationDirectives extends AuthenticationDirectives {  this: HttpService =>
  import akka.pattern.ask
  implicit val timeout: Timeout
  
  def loginActor: ActorRef
  override def doAuthenticate(token: UUID) = (loginActor ? TokenCheck(token)).mapTo[Option[UserDetailT[_]]]
}

trait Api extends RouteConcatenation { this: ServerCore =>
  implicit val executionContext = actorSystem.dispatcher
  val routes = new RecogService(recogCoordinator, actorSystem.settings.config.getString("server.web-server")).route

  def rejectionHandler: PartialFunction[scala.List[Rejection], HttpResponse] = {
    case (rejections: List[Rejection]) => HttpResponse(StatusCodes.BadRequest)
  }
  val rootService = actorSystem.actorOf(Props(new RoutedHttpService(routes)))
}

trait CustomerService extends HttpService {  this: EndpointMarshalling with AuthenticationDirectives =>
  protected val customerController = new CustomerController
  val customerRoute =
    path("customers" / JavaUUID) { id =>
      get {
        complete {  Future[Customer] {  customerController.get(id)  } }
      } ~
      authenticate(validCustomer) { ud =>
        post {
          handleWith { customer: Customer =>
            Future[Customer] {  customerController.update(ud, customer) }
          }
        }
      }
    }
}


case class SystemInfo(host: String, timestamp: Date)

trait HomeService extends HttpService {  this: EndpointMarshalling with AuthenticationDirectives =>
  import akka.pattern.ask
  implicit val timeout: Timeout
  val homeRoute = {
    path(Slash) {
      get {
        complete {  SystemInfo(InetAddress.getLocalHost.getCanonicalHostName, new Date)  }
      }
    }
  }

}


trait UserService extends HttpService {  this: EndpointMarshalling with AuthenticationDirectives =>
  import akka.pattern.ask
  implicit val timeout: Timeout
  def userActor: ActorRef
  implicit val UserRegistrationErrorMarshaller = errorSelectingEitherMarshaller[NotRegisteredUser, RegisteredUser](666)
  val userRoute =
    path("user" / "register") {
      post {
          handleWith {user: User =>
            (userActor ? RegisteredUser(user)).mapTo[Either[NotRegisteredUser, RegisteredUser]]
          }
      }
    }
}


trait ApiMarshalling extends DefaultJsonProtocol  with UuidMarshalling with DateMarshalling {
  this: UserFormats =>
  implicit val NotRegisteredUserFormat = jsonFormat1(NotRegisteredUser)
  implicit val RegisteredUserFormat = jsonFormat1(RegisteredUser)
  implicit val ImplementationFormat = jsonFormat3(Implementation)
  implicit val SystemInfoFormat = jsonFormat2(SystemInfo)
}

case class ErrorResponseException(responseStatus: StatusCode, response: Option[HttpEntity]) extends Exception

trait EitherErrorMarshalling {

  def errorSelectingEitherMarshaller[A, B](status: StatusCode)
                                          (implicit ma: Marshaller[A], mb: Marshaller[B]) =
    Marshaller[Either[A, B]] {
      (value, ctx) =>
        value match {
          case Left(a) =>
            val mc = new CollectingMarshallingContext()
            ma(a, mc)
            ctx.handleError(ErrorResponseException(status, mc.entity))
          case Right(b) =>
            mb(b, ctx)
        }
    }
}

trait EndpointMarshalling extends MetaMarshallers with SprayJsonSupport
  with ApiMarshalling  with UserFormats with CustomerFormats  with EitherErrorMarshalling




case class TrackingStat(path: String, ip: Option[String], auth: Option[UUID], kind: String,
 timestamp: Date = new Date,  id: UUID = UUID.randomUUID())

trait TrackingFormats extends DefaultJsonProtocol  with UuidMarshalling with DateMarshalling {
  protected implicit val TrackingStatFormat = jsonFormat6(TrackingStat)
}

trait TrackingMongo extends TrackingFormats with Configured {
  protected implicit val EndpointHitStartProvider = new SprayMongoCollection[TrackingStat](configured[DB], "tracking")
}

trait Tracking extends TrackingMongo {  this: AuthenticationDirectives with HttpService =>
  private val trackingMongo = new SprayMongo

  def trackRequestT(request: HttpRequest): Any => Unit = {
    val path = request.uri.split('?')(0) // not ideal for parameters in the path, e.g. uuids.
    val ip = request.headers.find(_.name == "Remote-Address").map { _.value }
    val auth = getToken(request)
    val stat = TrackingStat(path, ip, auth, "request")
    // the HttpService dispatcher is used to execute these inserts
    Future{trackingMongo.insertFast(stat)}
    // the code is executed when called, so the date is calculated when the response is ready
    (r:Any) => (Future{trackingMongo.insertFast(stat.copy(kind = "response", timestamp = new Date))})
  }

  def trackRequestResponse: Directive0 = {
    mapRequestContext { ctx =>
      val logResponse = trackRequestT(ctx.request)
      ctx.mapRouteResponse { response => logResponse(response); response}
    }
  }
}


trait FailureHandling {  this: HttpService =>
  def rejectionHandler: RejectionHandler = RejectionHandler.Default

  def exceptionHandler(implicit log: LoggingContext) = ExceptionHandler.fromPF {
    case e: IllegalArgumentException => ctx =>
      loggedFailureResponse(ctx, e,
        message = "The server was asked a question that didn't make sense: " + e.getMessage, error = NotAcceptable)
    case e: NoSuchElementException => ctx =>
      loggedFailureResponse(ctx, e,
        message = "The server is missing some information. Try again in a few moments.", error = NotFound)
    case t: Throwable => ctx =>  loggedFailureResponse(ctx, t)
  }

  private def loggedFailureResponse(ctx: RequestContext, thrown: Throwable,
    message: String = "The server is having problems.", error: StatusCode = InternalServerError)
    (implicit log: LoggingContext)
  	{log.error(thrown, ctx.request.toString()); ctx.complete(error, message)}
}


class RoutedHttpService(route: Route) extends Actor with HttpService {
  implicit def actorRefFactory = context
  implicit val handler = ExceptionHandler.fromPF {
    case NonFatal(ErrorResponseException(statusCode, entity)) => ctx =>  ctx.complete(statusCode, entity)
    case NonFatal(e) => ctx =>  ctx.complete(InternalServerError)
  }

  def receive = {
    runRoute(route)(handler, RejectionHandler.Default, context, RoutingSettings.Default, LoggingContext.fromActorRefFactory)
  }
}


trait CrossLocationRouteDirectives extends RouteDirectives {
  implicit def fromObjectCross[T : Marshaller](origin: String)(obj: T) =
    new CompletionMagnet {
      def route: StandardRoute = new CompletionRoute(OK,
        RawHeader("Access-Control-Allow-Origin", origin) :: Nil, obj)
    }
  private class CompletionRoute[T : Marshaller](status: StatusCode, headers: List[HttpHeader], obj: T)
    extends StandardRoute {
    def apply(ctx: RequestContext) {
      ctx.complete(status, headers, obj)
    }
  }
}

class RecogService(coordinator: ActorRef, origin: String)(implicit executionContext: ExecutionContext) extends Directives with CrossLocationRouteDirectives with EndpointMarshalling
  with DefaultTimeout with RecogFormats {
  val headers = RawHeader("Access-Control-Allow-Origin", origin) :: Nil
  import akka.pattern.ask

  	def image(sessionId: RecogSessionId)(ctx: RequestContext) {
	    (coordinator ? ProcessImage(sessionId, Base64.decodeBase64(ctx.request.entity.buffer))) onSuccess {
	      case x: RecogSessionAccepted  => ctx.complete(StatusCodes.Accepted,            headers, x)
	      case x: RecogSessionRejected  => ctx.complete(StatusCodes.BadRequest,          headers, x)
	      case x: RecogSessionCompleted => ctx.complete(StatusCodes.OK,                  headers, x)
	      case x                        => ctx.complete(StatusCodes.InternalServerError, headers, x.toString)
		}
  }

  val route =
    path("recog") {
      post { complete {  (coordinator ? Begin).map(_.toString)  } }
    } ~
    path("recog" / JavaUUID) { sessionId => post { image(sessionId)  } }

}



class CustomerController extends CustomerMongo with Configured {
  val mongo = new SprayMongo

  def register(customer: Customer, user: User) {
    mongo.insert(customer)
  }

  def get(id: CustomerReference) = mongo.findOne[Customer]("id" :> id).get

  def update(userDetail: UserDetailT[CustomerUserKind], customer: Customer) = userDetail.kind match {
    case CustomerUserKind(`customer`.id) => mongo.findAndReplace("id" :> customer.id, customer)
        customer
    case _ => throw new IllegalArgumentException(s"${userDetail.userReference} does not have access rights to any customers.")
  }

}
