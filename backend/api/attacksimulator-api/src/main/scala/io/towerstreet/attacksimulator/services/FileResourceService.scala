package io.towerstreet.attacksimulator.services

import java.io._

import com.typesafe.config.Config
import io.towerstreet.logging.Logging
import io.towerstreet.exceptions.NotFoundException
import javax.inject.{Inject, Singleton}

@Singleton
class FileResourceService @Inject()(config: Config)
  extends Logging
{
  private val fileFolder = config.getString("io.towerstreet.attacksimulator.test.resources.fileFolder")
  private val fileKeyRegex = config.getString("io.towerstreet.attacksimulator.test.resources.fileKeyRegex").r

  private[services] def getFileResourcePath(fileKey: String) = s"/$fileFolder/$fileKey"

  /**
    * Obtains requested file from test resource folder and returns input stream providing content of this file.
    *
    * Don't forget to close the stream.
    */
  def getFile(fileKey: String): InputStream = {
    // We allow only safe file keys (preventing "/" character to get out from folder)
    if (fileKeyRegex.findFirstIn(fileKey).isEmpty) {
      logger.error(s"Invalid format of requested resource file key: $fileKey")
      throw NotFoundException("Invalid format of requested resource file key")
    }

    val resourcePath = getFileResourcePath(fileKey)
    logger.info(s"Resolving resource file $resourcePath")

    val is = getClass.getResourceAsStream(resourcePath)
    if (is != null) {
      is
    } else {
      logger.error(s"File not found by provided file key: $fileKey")
      throw NotFoundException("File not found by provided file key")
    }
  }


  /**
    * Obtains requested file from test resource folder and returns input stream providing content of this file.
    * Content is encoded using XOR with 0xFFFF
    *
    * Don't forget to close the stream.
    */
  def getEncodedFile(fileKey: String): InputStream = {
    val is = getFile(fileKey)
    new EncodingInputStream(is)
  }

  class EncodingInputStream(is: InputStream) extends BufferedInputStream(is) {
    override def read(b: Array[Byte], off: Int, len: Int): Int = {
      val res = super.read(b, off, len)
      for (i <- 0 until Math.min(len, res)) {
        b(i) = encode(b(i))
      }
      res
    }

    private def encode(b: Byte) = (b ^ 0xff).toByte
  }
}
