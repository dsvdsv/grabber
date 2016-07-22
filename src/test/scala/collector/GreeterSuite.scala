package collector

import java.io.File
import java.net.URL
import java.nio.file.Path

import akka.testkit.{DefaultTimeout, ImplicitSender, TestActorRef}
import akka.pattern.ask

import scala.util.Success

class GreeterSuite extends AkkaSpec with DefaultTimeout with ImplicitSender {

  "demonstrate testing of behavior" in {
    println(new java.io.File( "." ).getCanonicalPath())
    val actorRef = TestActorRef(new Greeter)
    val future = actorRef ? Greeter.Greet
    val Success(result: Any) = future.value.get
    result should be(Greeter.Done)
  }

  "demonstrate testing url matching" in {
    val URL = """(http|https)://(.*)\.([a-z]+)""".r

    def splitURL(url : String) = url match {
      case u@URL(_) => new URL(u)
    }

    val result = splitURL("https://www.google.com") // prints (http,www.google,com)

    assert(new URL("https://www.google.com") == result)
  }
}
