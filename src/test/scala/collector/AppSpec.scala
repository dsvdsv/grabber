package collector

import java.io.File
import java.net.URL

import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.{DefaultTimeout, ImplicitSender}

/**
  * Created by dsvdsv on 23.07.16.
  */
class AppSpec extends AkkaSpec with DefaultTimeout with ImplicitSender {

  "A in directory" should "have subdirectory" in {
    val dirs = App.subdirectories

    dirs.map(_.getName).toList should contain allOf("data", "image", "page", "video")
  }

  "Files in directory" should "sorted buy name" in {
    val files = App.files(new File("in/page"))

    files.map(_.getName).toList should contain theSameElementsInOrderAs Seq("abc.txt", "fdg.txt", "qwe.txt")
  }

  "url stream" should " deliver elements to subscriber" in {
    val stream = App.urlsStream(new File("in/page"))

    stream
      .runWith(TestSink.probe[URL])
      .request(9)
      .expectNext(
        new URL("http://doubledata.ru/technology.html"),
        new URL("http://doubledata.ru/investors.html"),
        new URL("http://doubledata.ru/production.html"),

        new URL("http://doubledata.ru/technology.html"),
        new URL("http://doubledata.ru/investors.html"),
        new URL("http://doubledata.ru/production1.html"),

        new URL("http://doubledata.ru/technology.html"),
        new URL("http://doubledata.ru/investors.html"),
        new URL("http://doubledata.ru/production.html")
      )
      .expectComplete()
  }
}
