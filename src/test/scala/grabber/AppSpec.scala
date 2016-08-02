package grabber

import java.io.File
import java.nio.file.{Files, Path, Paths}
import java.time.LocalDate
import java.time.temporal.ChronoField
import java.util.concurrent.CopyOnWriteArraySet

import akka.Done
import akka.http.scaladsl.{Http, HttpExt}
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.Source
import akka.testkit.{DefaultTimeout, ImplicitSender}
import akka.util.ByteString
import org.apache.commons.io.FileUtils
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.duration._

class AppSpec extends AkkaSpec with BeforeAndAfterAll with DefaultTimeout with ImplicitSender {

  val resourceSize = 1024
  val resourceTimeout = (10 second).toMillis
  val in = new File("src/test/resources/in/data")
  val out = new File("target/out")

  import system.dispatcher

  var binding: ServerBinding = _

  val applyingTimeouts = new CopyOnWriteArraySet[Int]

  def byteString(n:Byte) = ByteString(Vector.fill(resourceSize)(n): _*)

  override protected def beforeAll(): Unit = {
    super.beforeAll()

    FileUtils.deleteDirectory(out)

    binding = Http().bindAndHandle({
      path("resource" / IntNumber) {  num =>
        get {
          parameters("timeout" ? false) { timeout =>
            val source = Source.single(byteString(num.toByte))
            if (timeout) {
              if (!applyingTimeouts.contains(num)) {
                Thread.sleep(resourceTimeout)
                applyingTimeouts.add(num)
              }
            }
            complete(HttpEntity(ContentTypes.NoContentType, source))
          }
        }
      }
    }, interface = "localhost", port = 9000).futureValue
  }

  override protected def afterAll(): Unit = {
    super.afterAll()

    FileUtils.deleteDirectory(out)

    binding.unbind().futureValue

  }

  "A flow" should "correct downloaded all resources" in {

    val http = Http(system)

    val result = grabber.flow(in, out, http)
      .runForeach( println )

    whenReady(result) { r =>

      val now = LocalDate.now()

      val outDir = Paths.get(
        out.getPath,
        "data",
        now.get(ChronoField.YEAR).toString,
        now.get(ChronoField.MONTH_OF_YEAR).toString,
        now.get(ChronoField.DAY_OF_MONTH).toString
      ).toFile

      assert(outDir.exists() == true)

      val file = outDir.listFiles()(0)

      assert(file.exists() == true)


      checkFileContents(file.toPath, "")
    }

  }

  def checkFileContents(f: Path, contents: String): Unit = {
    val out = Files.readAllBytes(f)
    out should contain (byteString(3).toArray)
  }
}
