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
// check if user is authorized to perform admin actions:
val admins = Set("Peter")
def hasAdminPermissions(user: User): Boolean =
  admins.contains(user.name)
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

// tests:
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
def greeter: Flow[Message, Message, Any] =
  Flow[Message].mapConcat {
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