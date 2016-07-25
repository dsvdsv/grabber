import java.io.File
import java.nio.file.{Files, Path, Paths}
import java.time.LocalDate
import java.time.temporal.ChronoField

import akka.util.ByteString

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
      now.get(ChronoField.YEAR).toString,
      now.get(ChronoField.MONTH_OF_YEAR).toString,
      now.get(ChronoField.DAY_OF_MONTH).toString
    )
    Files.createTempFile(Files.createDirectories(path), "file", ".bin")
  }
}
