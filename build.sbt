enablePlugins(JavaAppPackaging)

name := "akka-http-microservice"
organization := "com.kweku.micro"
version := "1.0"
scalaVersion := "2.12.3"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")
resolvers ++= Seq("Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
                  Resolver.bintrayRepo("hseeberger", "maven"))

libraryDependencies ++= {
  val AkkaVersion = "2.4.18"
  val AkkaHttpVersion = "10.0.6"
  val Json4sVersion = "3.5.2"
  Seq(
    "com.typesafe.akka" %% "akka-slf4j"      % AkkaVersion,
    "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "org.joda" % "joda-convert" % "1.8.1",
    "com.chuusai" % "shapeless_2.11" % "2.3.2",
    "org.typelevel" %% "cats" % "0.9.0",
    "org.julienrf" % "play-json-derived-codecs_2.11" % "3.2",
    "au.id.jazzy" %% "play-geojson" % "1.5.0",
    "com.datastax.cassandra" % "cassandra-driver-core" % "3.3.0",
    "org.xerial.snappy" % "snappy-java" % "1.1.4",
    "org.elasticsearch" % "elasticsearch" % "5.5.1",
    "org.elasticsearch.client" % "transport" % "5.5.1",
    "org.apache.kafka" % "kafka-clients" % "0.11.0.0",
    "org.abstractj.kalium" % "kalium" % "0.6.0",
    "com.typesafe.akka" % "akka-testkit_2.12" % "2.5.3" % "test",
    "org.json4s"        %% "json4s-native"   % Json4sVersion,
    "org.json4s"        %% "json4s-ext"      % Json4sVersion,
    "de.heikoseeberger" %% "akka-http-json4s" % "1.16.0"
  )
}

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)