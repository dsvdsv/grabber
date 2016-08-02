import java.io.File
import java.net.URL
import java.nio.file.{Files, Path, Paths, StandardOpenOption}
import java.time.LocalDate
import java.time.temporal.ChronoField
import java.time.temporal.ChronoField.{DAY_OF_MONTH, MONTH_OF_YEAR, YEAR}

import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.HttpRequest
import akka.stream.Materializer
import akka.stream.scaladsl.{FileIO, Framing, Source}
import akka.util.ByteString

import scala.concurrent.ExecutionContext
import scala.util.Try

package object grabber {
  val lineDelimiter = ByteString("\n")

  def subdirectories(dir: File): List[File] = {
    dir.listFiles()
      .filter(f => f.isDirectory && f.canRead)
      .toList
  }

  def files(directory: File): List[File] = {
    directory
      .listFiles()
      .filter(f => f.isFile && f.canRead)
      .sortWith(_.getName < _.getName)
      .toList
  }

  def createOutFile(out:String, dataType: String): Path = {
    val now = LocalDate.now()
    val path = Paths.get(
      out,
      dataType,
      now.get(YEAR).toString,
      now.get(MONTH_OF_YEAR).toString,
      now.get(DAY_OF_MONTH).toString
    )
    Files.createTempFile(Files.createDirectories(path), "file", ".bin")
  }

  def flow(inDir: File, outDir: File, http:HttpExt)(implicit fm: Materializer, executor: ExecutionContext) = {
    val outFile = createOutFile(outDir.getPath, inDir.getName)

    Source(files(inDir))
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
