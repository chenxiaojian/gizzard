import com.twitter.gizzard.config._
import com.twitter.querulous.config._
import com.twitter.querulous.evaluator.QueryEvaluatorFactory
import com.twitter.conversions.time._
import com.twitter.logging.config._

object Priority extends Enumeration {
  val High, Medium, Low = Value
}

trait Credentials extends Connection {
  val username = "root"
  val password = ""
}

object GizzardMemoization {
  var nsQueryEvaluator: QueryEvaluatorFactory = null
}

object TestQueryEvaluator extends QueryEvaluator {
  override def apply() = {
    if (GizzardMemoization.nsQueryEvaluator == null) {
      GizzardMemoization.nsQueryEvaluator = super.apply()
    }

    GizzardMemoization.nsQueryEvaluator
  }
}

class TestScheduler(val name: String) extends Scheduler {
  val schedulerType = new KestrelScheduler {
    path = "/tmp"
    keepJournal = false
  }
  errorLimit = 25
  badJobQueue = new JsonJobLogger { name = "bad_jobs" }
}

new GizzardServer {
  val nameServer = new NameServer {
    jobRelay.priority = Priority.High.id

    val replicas = Seq(new Mysql {
      val connection = new Connection with Credentials {
        val hostnames = Seq("localhost")
        val database  = "gizzard_test"
      }
    })
  }

  val jobQueues = Map(
    Priority.High.id   -> new TestScheduler("high"),
    Priority.Medium.id -> new TestScheduler("medium"),
    Priority.Low.id    -> new TestScheduler("low")
  )

  loggers = List(
    new LoggerConfig {
      level = Level.ERROR
    }, new LoggerConfig {
      node = "w3c"
      useParents = false
      level = Level.DEBUG
    }, new LoggerConfig {
      node = "bad_jobs"
      useParents = false
      level = Level.INFO
    }
  )
}
