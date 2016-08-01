package grabber

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers, WordSpecLike}

object AkkaSpec {
  def getCallerName(clazz: Class[_]): String = {
    val s = (Thread.currentThread.getStackTrace map (_.getClassName) drop 1)
      .dropWhile(_ matches "(java.lang.Thread|.*AkkaSpec.?$)")
    val reduced = s.lastIndexWhere(_ == clazz.getName) match {
      case -1 ⇒ s
      case z  ⇒ s drop (z + 1)
    }
    reduced.head.replaceFirst(""".*\.""", "").replaceAll("[^a-zA-Z_0-9]", "_")
  }
}

abstract class AkkaSpec(_system: ActorSystem)
  extends TestKit(_system) with FlatSpecLike with Matchers with BeforeAndAfterAll with ScalaFutures{

  implicit val materializer = ActorMaterializer()

  implicit val patience = PatienceConfig(testKitSettings.DefaultTimeout.duration)

  def this() = this(ActorSystem(AkkaSpec.getCallerName(getClass)))

  override protected def afterAll: Unit = {
    shutdown()
  }
}
