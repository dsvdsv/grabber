package grabber

import akka.NotUsed
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.Source
import akka.testkit.{DefaultTimeout, ImplicitSender}
import akka.util.ByteString
import org.scalatest.BeforeAndAfterAll

class AppSpec extends AkkaSpec with BeforeAndAfterAll with DefaultTimeout with ImplicitSender {

  var binding: ServerBinding = _

  def byteString(n:Int) = ByteString(Vector.fill(1024)(n.toByte): _*)

  override protected def beforeAll(): Unit = {
    super.beforeAll()

    binding = Http().bindAndHandle({
      path("long-response" / Segment / IntNumber) {  (s,n) =>
        get {
          val source = Source.single(ByteString(s))
            .concat(Source.single(byteString(n)))

          complete(HttpEntity(ContentTypes.NoContentType, source))
        }
      }
    }, interface = "localhost", port = 9000).futureValue
  }

  override protected def afterAll(): Unit = {
    super.afterAll()

    binding.unbind().futureValue
  }

  "A in directory" should "have subdirectory" in {
    Thread.sleep(16000)
  }
}
