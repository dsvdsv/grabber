package grabber

import java.io.File
import java.net.URL
import java.nio.file.StandardOpenOption

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._

import scala.concurrent.Future
import scala.util.Try

object Main extends App {
  implicit val system = ActorSystem("Sys")

  import system.dispatcher

  implicit val materializer = ActorMaterializer()
  val http = Http(system)

  val in = new File("in")
  val out = new File("out")

  val dirs = subdirectories(in)

  val sources = dirs.map(flow)

  Future.sequence(
    sources
      .map(_.runForeach(println))
  ).onComplete(_ => system.shutdown())


  def flow(dir: File) = {
    val outFile = createOutFile(out.getPath, dir.getName)

    Source(files(dir))
      .flatMapConcat { f =>
        FileIO.fromPath(f.toPath)
          .concat(Source.single(lineDelimiter))
      }
      .via(Framing.delimiter(lineDelimiter, 512))
      .map(_.utf8String)
      .mapConcat { elem =>
        Try(new URL(elem)) match {
          case scala.util.Success(url) => List(url)
          case _ => Nil
        }
      }
      .mapAsyncUnordered(4) { url =>
        http.singleRequest(HttpRequest(uri = url.toString))
          .flatMap { response =>
            response.entity.dataBytes
              .runWith(FileIO.toPath(outFile, Set(StandardOpenOption.APPEND)))
          }
      }
  }
}