package grabber

import java.io.File
import java.nio.file.{Files, Paths}
import java.time.LocalDate
import java.time.temporal.ChronoField
import java.util.concurrent.CopyOnWriteArraySet

import akka.http.scaladsl.Http
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

  val applyingTimeouts = new CopyOnWriteArraySet[Int]
  var binding: ServerBinding = _

  override protected def beforeAll(): Unit = {
    super.beforeAll()

    // delete out dir
    FileUtils.deleteDirectory(out)

    binding = Http().bindAndHandle({
      path("resource" / IntNumber) { num =>
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

  def byteString(n: Byte) = ByteString(Vector.fill(resourceSize)(n): _*)

  override protected def afterAll(): Unit = {
    super.afterAll()

    // delete out dir
    FileUtils.deleteDirectory(out)

    binding.unbind().futureValue

  }

  "A crawler" should "correct download all resources" in {

    val result = Crawler.flow(in, out)
      .runForeach(println)

    whenReady(result) { r =>

      // check structure of subdirectories
      val now = LocalDate.now()

      val outDir = Paths.get(
        out.getPath,
        "data",
        now.get(ChronoField.YEAR).toString,
        now.get(ChronoField.MONTH_OF_YEAR).toString,
        now.get(ChronoField.DAY_OF_MONTH).toString
      ).toFile

      assert(outDir.exists() == true)

      // load all bytes from file
      val bytes = Files.readAllBytes(outDir.listFiles()(0).toPath)

      // check file size
      // we have 8 valid urls
      bytes should have length (resourceSize * 8)

      // check file content
      (bytes.count(_ == 11)) should equal(resourceSize) // http://localhost:9000/resource/11?timeout=true
      (bytes.count(_ == 12)) should equal(resourceSize) // http://localhost:9000/resource/12
      (bytes.count(_ == 3)) should equal(resourceSize) // http://localhost:9000/resource/3

      (bytes.count(_ == 4)) should equal(resourceSize) // http://localhost:9000/resource/4
      (bytes.count(_ == 5)) should equal(resourceSize) // http://localhost:9000/resource/5
      (bytes.count(_ == 6)) should equal(resourceSize) // http://localhost:9000/resource/6

      (bytes.count(_ == 7)) should equal(resourceSize) // http://localhost:9000/resource/7
      (bytes.count(_ == 8)) should equal(resourceSize) // http://localhost:9000/resource/8
      (bytes.count(_ == 9)) should equal(0) // invalid url http://localhost:9001/resource/9
    }

  }
}
