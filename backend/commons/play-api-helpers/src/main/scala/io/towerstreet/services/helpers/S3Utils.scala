package io.towerstreet.services.helpers

import com.amazonaws.services.s3.AmazonS3ClientBuilder
import io.towerstreet.logging.Logging

import scala.util.Try

trait S3Utils {
  this : Logging =>

  /** Reads given path from S3 as String **/
  def loadFileAsString(bucket: String, relativePath: String, region: String): Try[String] = {
    Try(AmazonS3ClientBuilder
      .standard()
      .withRegion(region)
      .build()
      .getObjectAsString(bucket, relativePath)
    )
  }
}
