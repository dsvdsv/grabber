package grabber

import java.io.File

import akka.actor.ActorSystem
import akka.http.scaladsl.{Http, HttpExt}
import akka.stream.ActorMaterializer

import scala.concurrent.Future

object Main extends App {
  implicit val system = ActorSystem("Sys")

  import system.dispatcher

  implicit val mat = ActorMaterializer()

  val http1: HttpExt = Http(system)

  val config = system.settings.config.getConfig("grabber")

  val in = new File(config.getString("in"))
  val out = new File(config.getString("out"))

  val dirs = Crawler.subdirectories(in)

  val sources = dirs.map(Crawler.flow(_, out, http1))

  Future.sequence(
    sources
      .map(_.runForeach(println))
  ).onComplete(_ => system.terminate())

}