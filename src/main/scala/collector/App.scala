package collector

import java.io.File
import java.net.URL

import akka.actor.Actor
import akka.actor.Props
import akka.stream.scaladsl.{FileIO, Framing, Source}
import akka.util.ByteString

import scala.util.Try

class App extends Actor {

  val data = new java.io.File("in")

  override def preStart(): Unit = {
    // create the greeter actor
    val greeter = context.actorOf(Props[Greeter], "greeter")
    // tell it to perform the greeting
    greeter ! Greeter.Greet
  }

  def receive = {
    // when the greeter is done, stop this actor and with it the application
    case Greeter.Done => context.stop(self)
  }
}

object App {
  val in = new java.io.File("in")

  val lineDelimiter = ByteString("\n")

  def subdirectories: Iterable[File] = {
    in.listFiles()
      .filter(f => f.isDirectory && f.canRead)
  }

  def files(directory: File):Iterable[File] = {
    directory
      .listFiles()
      .filter(f => f.isFile && f.canRead)
      .sortWith(_.getName < _.getName)
  }

 def urlsStream(dir: File): Source[URL, Any] = {
   Source(files(dir).toList)
     .flatMapConcat { f =>
       FileIO.fromPath(f.toPath)
         .concat(Source.single(lineDelimiter))
     }
     .via(Framing.delimiter(lineDelimiter, 512))
     .map(_.utf8String)
     .mapConcat { elem =>
        Try(new URL(elem)) match {
          case scala.util.Success(url) => List(url)
          case _=> Nil
        }
     }
 }
}

