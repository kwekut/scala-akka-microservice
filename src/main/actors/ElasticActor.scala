package actors

import akka.actor.{ActorRef, ActorSystem, Props, Actor}
import akka.actor.{ActorKilledException, ActorInitializationException}
import akka.pattern.pipe
import javax.inject._
import com.google.inject.name.Named
import com.google.inject.assistedinject.Assisted
import java.util.UUID
import play.api.Logger
import models.User
import scala.util.Try
import scala.util.{Success, Failure}
import akka.stream.alpakka.elasticsearch.scaladsl.{ElasticsearchFlow,ElasticsearchSink,ElasticsearchSinkSettings,ElasticsearchSource,ElasticsearchSourceSettings}
import akka.stream.alpakka.elasticsearch.{ElasticsearchFlowStage,ElasticsearchSourceLogic,ElasticsearchSourceStage,IncomingMessage,MessageReader,MessageWriter,OutgoingMessage,ScrollResponse,ScrollResult}
import scala.collection.JavaConverters._
import scala.language.postfixOps
import org.joda.time.LocalTime
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.joda.time.LocalDateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._
import com.google.common.io.Files
import java.io.File
import akka.actor.SupervisorStrategy._
import akka.routing._
import scala.concurrent.{ Future, ExecutionContext }
import models.PartialFunctions._
import services.elastic.ElasticClient
import services.elastic.ElasticClient._
import models.Locality._
import models.mail._
import models._
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy._
import com.typesafe.config.ConfigFactory
import java.util.concurrent.TimeUnit
import scala.util.Random
import scala.concurrent.duration._
import akka.pattern._
import akka.pattern.CircuitBreaker
import akka.pattern.pipe
import play.api.Play.current
import org.elasticsearch.index.search.geo._
import org.elasticsearch.index.search._
import org.elasticsearch.search.sort.SortOrder
import org.elasticsearch.search._
import org.elasticsearch.action.search._
import org.elasticsearch.common.geo.ShapeRelation
import org.elasticsearch.common.geo.builders.ShapeBuilder 
//import org.elasticsearch.index.query.FilterBuilders._
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders._
import org.elasticsearch.index.query.QueryBuilders._
import org.elasticsearch.index.query.QueryBuilder 
import org.elasticsearch.common.geo.GeoDistance
import org.elasticsearch.common.unit._
import org.elasticsearch.client._



object ElasticActor {
  case object Mount
}

class ElasticActor(errorActor: ActorRef)(implicit val ec: ExecutionContext)  extends Actor {
  import ElasticChildActor._
  import ElasticConfig._
  var prompt = 0

    override val supervisorStrategy =
      OneForOneStrategy(maxNrOfRetries = maxnrofretries, withinTimeRange = withintimerange milliseconds) {
        case aIE: ActorInitializationException => SendEmail("ActorInitializationException - KafkaProdChildActor Shut:", aIE.getMessage).send; Stop 
        case aKE: ActorKilledException => SendEmail("ActorKilledException - KafkaProdChildActor Shut:", aKE.getMessage).send; Stop 
        case uE: Exception if prompt < 4 => prompt + 1
          SendEmail("Exception -  Restart:", uE.getMessage).send;Restart
      }
    override def preStart() {
    }

    val reader: ActorRef = context.actorOf(
      BalancingPool(initialsize).props(ElasticChildActor.props()), "elasticrouter")
    
  def receive = {
    case x => reader forward x
    }

}

object ElasticChildActor {
  def props(): Props = Props(new ElasticChildActor())
  val dtz: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-mm-dd HH:MM:SS Z")
}

class ElasticChildActor extends Actor {
  import ElasticChildActor._
  import ElasticConfig._
  val client: Client = ElasticClient.client
  implicit val ec = context.dispatcher

  //If message causes a restart triggering exception, Warn user, Inform Administrator
  override def preRestart(reason: Throwable, message: Option[Any]) {
    message match {
      case Some(m) => SendEmail("ActorRestartException - KafkaProdChildActor Shut:", reason.getMessage).send
      case None => SendEmail("ActorRestartException - KafkaProdChildActor Shut:", reason.getMessage).send
    }
  }

  val system = akka.actor.ActorSystem("system")
  import system.dispatcher


  def receive = {

//(bbox: BoundingBox) = (bbox.southWest,bbox.northEast)
    //bbox, filter, shopid, feedid, page, range, revid, out, user
    case SocToElasticBbox("SEARCH", out, user, bbox) =>
        val date = dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z")).toString
        val bq:QueryBuilder = boolQuery()
          .mustNot(termQuery("expiry", "expired"))
          //.mustNot(termQuery("expiry", date)) before now
          //.should(multiMatchQuery(page.filter, "search"));
        val gq: QueryBuilder = geoBoundingBoxQuery("pin.location") 
          .setCorners(bbox.southWest.lat, bbox.southWest.lng, bbox.northEast.lat, bbox.northEast.lng); 

        val response: SearchResponse = client.prepareSearch()
          .setIndices(searchfeedindex)
          .setTypes(searchfeedtype)
          .setQuery(bq)
          .setPostFilter(gq)
          .setSize(1500)
          .addSort("likes", SortOrder.ASC)
          .execute()
          .actionGet()
          val res:SearchHits = response.getHits() 
          val re: Array[SearchHit] = res.getHits()
          val ra: Array[JsValue] = re collect(ehitTOjmsgPF)
          val r: Set[JsValue] = ra.toSet
          r map {mg=> out ! mg}
          //nb - make activity SEARCHFEED before indexing
          //add shop location to feed before indexing


// include default hints
    case SocToElasticPageBbox("GETHINTS", out, user, bbox, page) =>
        val bq:QueryBuilder = boolQuery()
          .should(multiMatchQuery(page.filter, "summary", "category", "name", "about", "address"))
        val gq: QueryBuilder = geoBoundingBoxQuery("pin.location") 
          .setCorners(bbox.southWest.lat, bbox.southWest.lng, bbox.northEast.lat, bbox.northEast.lng); 

        val response: SearchResponse = client.prepareSearch()
            .setIndices(hintindex)
            .setTypes(hinttype)
            .setQuery(bq)
            .setPostFilter(gq)
            .setSize(1500)
            .addSort("shoplikes", SortOrder.ASC)
            .execute()
            .actionGet();
      
          val res:SearchHits = response.getHits() 
          val re: Array[SearchHit] = res.getHits()
          val ra: Array[JsValue] = re collect(ehitTOjshpPF)
          val r: Set[JsValue] = ra.toSet
          r map {sp=> out ! sp}

val f1 = ElasticsearchSource("source","book","""{"match_all": {}}""",ElasticsearchSourceSettings(5)).map { message: OutgoingMessage[JsObject] =>
    IncomingMessage(Some(message.id), message.source)
  }.runWith(ElasticsearchSink("sink1","book",ElasticsearchSinkSettings(5)))


case class Book(title: String)
implicit val format = jsonFormat1(Book)
val f1 = ElasticsearchSource.typed[Book]("source","book", """{"match_all": {}}""",ElasticsearchSourceSettings(5))
  .map { message: OutgoingMessage[Book] =>  IncomingMessage(Some(message.id), message.source)  }
  .runWith(ElasticsearchSink.typed[Book]("sink2","book",ElasticsearchSinkSettings(5)))


    // Health check both the elastic actors and the elastic cluster
    case HealthCheck => 
        val adminid = "none"
        val date = dtz.parseDateTime(DateTime.now.toString("yyyy-mm-dd HH:MM:SS Z")).toString
        // val trans = Future{sc.cassandraTable[String]("usersdb", "poststable").select().where("userid = ?", adminid).limit(10).collect() }            
        // trans map (x=> ("ChildSearchActor" + "=" + date + ":")) pipeTo sender

    //case x => errorActor ! (Talker(x.toString)) 
  }
}

object ElasticConfig {
    //var cntr: Int = 0
    //val client: Client = ElasticClient.client
    val system = akka.actor.ActorSystem("system")
    val c = ConfigFactory.load()
    c.checkValid(ConfigFactory.defaultReference(), "elasticactor")
    
    val initialsize = c.getInt("elasticactor.startingRouteeNumber")
    val withintimerange = c.getDuration("elasticactor.supervisorStrategy.withinTimeRange", TimeUnit.MILLISECONDS) //should be(1 * 1000)
    val maxnrofretries = c.getInt("elasticactor.supervisorStrategy.maxNrOfRetries")  
}


// "query": {
//     "function_score": {
//         "query": {
//             "multi_match": {
//                 "query": "Lambda Expressions",
//                 "fields": [ "title", "tags^0.8", "speakers.name^0.6", "description^0.3" ]
//             }
//         },
//         "functions": [
//             {
//                 "gauss": {
//                     "publishedOn": {
//                         "scale": "130w",
//                         "offset": "26w",
//                         "decay": 0.3
//                     }
//                 }
//             }
//         ]
//     }
// } 
// final MultiMatchQueryBuilder multiMatchQuery = QueryBuilders.multiMatchQuery(
//     text, "title","tags^0.8", "speakers.name^0.6", "description^0.3")
//         .type(MultiMatchQueryBuilder.Type.BEST_FIELDS);
 
// final FunctionScoreQueryBuilder functionScoreQuery = QueryBuilders.functionScoreQuery(multiMatchQuery);
// functionScoreQuery.scoreMode("multiply");
// functionScoreQuery.boostMode(CombineFunction.MULT);
// functionScoreQuery.add(
//     ScoreFunctionBuilders.gaussDecayFunction("publishedOn",
// "130w").setOffset("26w").setDecay(0.3));

// val lat = 52.52
// val lon = 13.402
// QueryBuilders
//   .functionScoreQuery(
//      ScoreFunctionBuilders.gaussDecayFunction("location", new GeoPoint(lat, lon), "10km")
//        .setDecay(0.9)
//        .setOffset("30km"))
//   .add(
//      ScoreFunctionBuilders.gaussDecayFunction("createdAt", new DateTime(), "8h")
//        .setDecay(0.75)
//        .setOffset("4h"))
// )

// QueryBuilder qb = 
// functionScoreQuery()
//     .add(
//         should(multiMatchQuery(srh.filter, "shopname", "category", "title", "summary", "detail", "options", "discount", "price")),             
//         randomFunction("ABCDEF")                  
//     )
//     .add(
//         exponentialDecayFunction("age", 0L, 1L)   
//     );

// q.FunctionScore(c => c
//     .Name("named_query")
//     .Boost(1.1)
//     .Query(qq => qq.MatchAll())
//     .BoostMode(FunctionBoostMode.Multiply)
//     .ScoreMode(FunctionScoreMode.Sum)
//     .MaxBoost(20.0)
//     .MinScore(1.0)
//     .Functions(f => f
//         .Exponential(b => b.Field(p => p.NumberOfCommits).Decay(0.5).Origin(1.0).Scale(0.1).Weight(2.1))
//         .GaussDate(b => b.Field(p => p.LastActivity).Origin(DateMath.Now).Decay(0.5).Scale("1d"))
//         .LinearGeoLocation(b => b.Field(p => p.Location).Origin(new GeoLocation(70, -70)).Scale(Distance.Miles(1)).MultiValueMode(MultiValueMode.Average))
//         .FieldValueFactor(b => b.Field("x").Factor(1.1).Missing(0.1).Modifier(FieldValueFactorModifier.Ln))
//         .RandomScore(1337)
//         .RandomScore("randomstring")
//         .Weight(1.0)
//         .ScriptScore(ss => ss.Script(s => s.File("x")))
//     )
// )