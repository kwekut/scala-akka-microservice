https://workday.github.io/2015/03/17/scala-days-improving-correctness-with-types
https://www.javatpoint.com/scala-constructor
https://github.com/underscoreio/shapeless-guide


object Math {
  import annotation.implicitNotFound
  @implicitNotFound("No member of type class NumberLike in scope for ${T}")
  trait NumberLike[T] {
    def plus(x: T, y: T): T
    def divide(x: T, y: Int): T
    def minus(x: T, y: T): T
  }
  object NumberLike {
    implicit object NumberLikeDouble extends NumberLike[Double] {
      def plus(x: Double, y: Double): Double = x + y
      def divide(x: Double, y: Int): Double = x / y
      def minus(x: Double, y: Double): Double = x - y
    }
    implicit object NumberLikeInt extends NumberLike[Int] {
      def plus(x: Int, y: Int): Int = x + y
      def divide(x: Int, y: Int): Int = x / y
      def minus(x: Int, y: Int): Int = x - y
    }
  }
}
//A second, implicit parameter list on all methods that expect a member of a type class can be a little verbose.
//A context bound T : NumberLike means that an implicit value of type NumberLike[T] must be available
// If you want to access that implicitly available value, however, you need to call the implicitly method, as we do in the iqr method
object Statistics {
  import Math.NumberLike
  def mean[T](xs: Vector[T])(implicit ev: NumberLike[T]): T =
    ev.divide(xs.reduce(ev.plus(_, _)), xs.size)
  def median[T : NumberLike](xs: Vector[T]): T = xs(xs.size / 2)
  def quartiles[T: NumberLike](xs: Vector[T]): (T, T, T) =
    (xs(xs.size / 4), median(xs), xs(xs.size / 4 * 3))
  def iqr[T: NumberLike](xs: Vector[T]): T = quartiles(xs) match {
    case (lowerQuartile, _, upperQuartile) =>
      implicitly[NumberLike[T]].minus(upperQuartile, lowerQuartile)
  }
}
val numbers = Vector[Double](13, 23.0, 42, 45, 61, 73, 96, 100, 199, 420, 900, 3839)
println(Statistics.mean(numbers))
//////////////////Adding Joda
object JodaImplicits {
  import Math.NumberLike
  import org.joda.time.Duration
  implicit object NumberLikeDuration extends NumberLike[Duration] {
    def plus(x: Duration, y: Duration): Duration = x.plus(y)
    def divide(x: Duration, y: Int): Duration = Duration.millis(x.getMillis / y)
    def minus(x: Duration, y: Duration): Duration = x.minus(y)
  }
}
import Statistics
import JodaImplicits._
import org.joda.time.Duration._

val durations = Vector(standardSeconds(20), standardSeconds(57), standardMinutes(2),
 standardHours(2), standardHours(17), standardDays(1))
println(Statistics.mean(durations).getStandardHours)
//////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////

class Currency private (code:String) extends AnyVal
object Currency{
	val USD = new Currency("USD")
	val EUR = new Currency("EUR")
	def from(code: String): Option[Currency] = {
	}
}

import org.scalactic.TypeCheckTripleEquals._
case class Customer(name: String, currency: Currency)

def creditCardRate(customer:Customer): BigDecimal = {
	if (customer.currency === "USD"){
		BigDecimal("0.977")
	}else {
		BigDecimal("0.799")
	}
}

class MoneyAmount(amount: BigDecimal) extends AnyVal {
	def + (rhs: MoneyAmount): MoneyAmount ={
		new MoneyAmount(amount + rhs.amount)
	}
	def - (rhs: MoneyAmount): MoneyAmount ={
		new MoneyAmount(amount - rhs.amount)
	}
	def * (rhs: MoneyAmount): MoneyAmount ={
		new MoneyAmount(amount * rhs.size)
	}
}

class Rate(size: BigDecimal) extends AnyVal {
	def * (rhs: Rate): MoneyAmount =rhs*this
}


import org.scalactic.Every
def average(items:Every[Int]): Int = items.sum/items.size
def average(first:Int,rest:Int): Int = {
	average(Every(first,rest: _*))
}


///////////////////////////////
import shapeless.tag.@@
trait Foo
def onlyFoo(foo: String @@ Foo): String = { s"$foo"}
//list[String @@ Foo]
implicit tojson :
val fo = tag[Foo]("Whatever")
onlyFoo(fo)




case class Customer(name: String, preferredCurrency: String) { 
	require(Currency.isValid(preferredCurrency)) 
} 
val customer = Customer(name = "Joe Bloggs", preferredCurrency = "SFO")

class Currency private (val code: String) extends AnyVal 
object Currency { 
	val USD: Currency = new Currency("USD") 
	val EUR: Currency = new Currency("EUR") 
	def from(code: String): Option[Currency] = {

	}
}

case class Customer(name: String, preferredCurrency: Currency) 
def creditCardRate(customer: Customer): BigDecimal = { 
	if (customer.preferredCurrency == "USD") BigDecimal("0.015") 
	else BigDecimal("0.025") 
} 

import org.scalactic.TypeCheckedTripleEquals._ 
case class Customer(name: String, preferredCurrency: Currency) 
def creditCardRate(customer: Customer): BigDecimal = { 
	if (customer.preferredCurrency === "USD") BigDecimal("0.015") 
	else BigDecimal("0.025") 
} //Does not compile

import org.scalactic.TypeCheckedTripleEquals._ 
case class Customer(name: String, preferredCurrency: Currency) 
def creditCardRate(customer: Customer): BigDecimal = { 
	if (customer.preferredCurrency === Currency.USD) BigDecimal("0.015") 
	else BigDecimal("0.025") }

val order: Order = ??? 
val customer: Customer = ??? 
val creditCardCharge = order.amount + creditCardRate(customer) //Eeek this a bug

class MoneyAmount(val amount: BigDecimal) extends AnyVal { 
	def + (rhs: MoneyAmount): MoneyAmount = new MoneyAmount(amount + rhs.amount) 
	def - (rhs: MoneyAmount): MoneyAmount = new MoneyAmount(amount - rhs.amount) 
	def * (rhs: Rate): MoneyAmount = new MoneyAmount(amount * rhs.size) } 

class Rate(val size: BigDecimal) extends AnyVal { 
	def * (rhs: Rate): MoneyAmount = rhs * this 
}
val order: Order = ??? 
val customer: Customer = ??? 
val creditCardCharge = order.amount + creditCardRate(customer) //Does not compile

def average(items: List[Int]): Int = items.sum / items.size 
average(List()) //Does not compile

import org.scalactic.Every 
def average(items: Every[Int]): Int = items.sum / items.size 
average(Every(5)) 
average(Every(5, 10, 15)) 
average(Every()) 

import org.scalactic.Every 
def average(items: Every[Int]): Int = items.sum / items.size 
def average(first: Int, rest: Int*): Int = average(Every(first, rest: _*)) 
average(5) 

case class Agent(agentId: String, jobType: Int, host: String, port: Int, 
	status: String,  maybeLastAccessed: Option[DateTime], inUse: Boolean, 
	maybeInUseBy: Option[String]) 
case class Job(referenceId: String, jobType: Int, status: String, 
	submittedBy: String, submittedAt: DateTime, maybeStartedAt: Option[DateTime], 
	maybeProcessedBy: Option[String], maybeCompletedAt: Option[DateTime])

case class Agent(agentId: String, jobType: JobType, address: AgentAddress, 
	status: AgentStatus, lastAccessed: Option[DateTime], inUse: Boolean, 
	maybeInUseBy: Option[String]) 
case class Job(referenceId: String, jobType: JobType, status: JobStatus, 
	submittedBy: User, submittedAt: DateTime, maybeStartedAt: Option[DateTime], 
	maybeProcessedBy: Option[String], maybeCompletedAt: Option[DateTime])

sealed abstract class JobType(val value: Int) 
case object SmallJob extends JobType(1) 
case object LargeJob extends JobType(2) 
case object BatchJob extends JobType(3) 

sealed abstract class AgentStatus(val value: String) 
case object AgentWaiting extends AgentStatus("Waiting") 
case object AgentActive extends AgentStatus("Active") 
case object AgentFailed extends AgentStatus("Failed") 

sealed abstract class JobStatus(val value: String) 
case object JobWaiting extends JobStatus("Waiting") 
case object JobActive extends JobStatus("Active") 
case object JobCompelete extends JobStatus("Complete") 

case class AgentAddress(host: String, port: Int) 
case class User(name: String)
case class Agent(agentId: String, jobType: JobType, address: AgentAddress, 
	status: AgentStatus, lastAccessed: Option[DateTime], inUse: Boolean, 
	maybeInUseBy: Option[String]) 
case class Job(referenceId: String, jobType: JobType, status: JobStatus, 
	submittedBy: User, submittedAt: DateTime, maybeStartedAt: Option[DateTime], 
	maybeProcessedBy: Option[String], maybeCompletedAt: Option[DateTime])

import tag.@@ 
trait Foo 
def onlyFoo(value: String @@ Foo): String = value 
val foo = tag[Foo]("Foo String") 
onlyFoo(foo) 


case class Agent(agentId: String @@ Agent, jobType: JobType, address: AgentAddress, 
	status: AgentStatus, lastAccessed: Option[DateTime], inUse: Boolean, 
	maybeInUseBy: Option[String @@ Job]) 
case class Job(referenceId: String @@ Job, jobType: JobType, status: JobStatus, 
	submittedBy: User, submittedAt: DateTime, maybeStartedAt: Option[DateTime], 
	maybeProcessedBy: Option[String @@ Agent], maybeCompletedAt: Option[DateTime])
case class Job(referenceId: String @@ Agent, jobType: JobType, status: JobStatus, 
	submittedBy: User, submittedAt: DateTime, maybeStartedAt: Option[DateTime], 
	maybeProcessedBy: Option[String @@ Job], maybeCompletedAt: Option[DateTime])

def recordCompletionMetrics(job: Job): Unit = { 
	for( startedAt <- job.maybeStartedAt ; 
		completedAt <- job.maybeCompletedAt 
		) { writeJobEvent( 
			event = "Completed", 
			time = completedAt, 
			referenceId = job.referenceId, 
			waitingTime = (startedAt - job.submittedAt
		), executionTime = (completedAt - startedAt)) 
		} 
	}
def recordCompletionMetrics(job: Job): Unit = { 
	require(job.status = JobComplete) 
	require(job.maybeStartedAt.isDefined) 
	require(job.maybeCompletedAt.isDefined) 
	for (startedAt <- job.maybeStartedAt ; 
		completedAt <- job.maybeCompletedAt 
		) { writeJobEvent ( 
			event = "Completed", 
			time = completedAt, 
			referenceId = job.referenceId, 
			waitingTime = (startedAt - job.submittedAt), 
			executionTime = (completedAt - startedAt)) 
	} 
}

sealed trait Job { 
	def referenceId: String @@ Job 
	def status: JobStatus 
	def submittedBy: User 
	def submittedAt: DateTime 

} 
case class WaitingJob(referenceId: String @@ Job, submittedBy: User, submittedAt: DateTime) extends Job { val status: JobStatus = JobWaiting }

sealed trait Job { 
	def referenceId: String @@ Job 
	def status: JobStatus 
	def submittedBy: User 
	def submittedAt: DateTime 
}

case class ActiveJob(referenceId: String @@ Job, submittedBy: User, submittedAt: DateTime, startedAt: DateTime, processedBy: String @@ Agent) extends Job { val status: JobStatus = JobActive }
sealed trait Job { 
	def referenceId: String @@ Job 
	def status: JobStatus 
	def submittedBy: User 
	def submittedAt: DateTime 
} 

case class CompleteJob(referenceId: String @@ Job, submittedBy: User, submittedAt: DateTime, 
	startedAt: DateTime, processedBy: String @@ Agent, completedAt: DateTime) extends Job { 
	val status: JobStatus = JobComplete }
def recordCompletionMetrics(job: CompleteJob): Unit = { 
	writeJobEvent( 
		event = "Completed", time = job.completedAt, referenceId = job.referenceId, 
		waitingTime = (job.startedAt - job.submittedAt), 
		executionTime = (job.completedAt - job.startedAt)
	) 
}
http://workday.github.io

////////////////////////////////////////////////////////////////////
class ThisExample{  
    var id:Int = 0  
    var name: String = ""  
    def this(id:Int, name:String){  
        this()  
        this.id = id  
        this.name = name  
    }  
    def show(){  
        println(id+" "+name)  
    }  
}  
  
object MainObject{  
    def main(args:Array[String]){  
        var t = new ThisExample(101,"Martin")  
        t.show()  
    }  
}  
/////////Scala Secondary Constructor Example
//this keyword is used to call constructor from other constructor.
class Student(id:Int, name:String){  
    var age:Int = 0  
    def showDetails(){  
        println(id+" "+name+" "+age)  
    }  
    def this(id:Int, name:String,age:Int){  
        this(id,name)       // Calling primary constructor, and it is first line  
        this.age = age  
    }  
}  
  
object MainObject{  
    def main(args:Array[String]){  
        var s = new Student(101,"Rama",20);  
        s.showDetails()  
    }  
}  

//implicit
case class Donut(name: String, price: Double, productCode: Option[Long] = None)
object DonutImplicits {
 implicit class AugmentedDonut(donut: Donut) {
  def uuid: String = s"${donut.name} - ${donut.productCode.getOrElse(12345)}"
 }
}
import DonutImplicits._
val vanillaDonut: Donut = Donut("Vanilla", 1.50)
Vanilla donut uuid = Vanilla - 12345