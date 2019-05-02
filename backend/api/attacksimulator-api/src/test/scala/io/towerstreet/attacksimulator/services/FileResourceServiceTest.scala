package io.towerstreet.attacksimulator.services

import io.towerstreet.exceptions.NotFoundException
import org.apache.commons.io.IOUtils
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.test.Injecting

import scala.io.Source

class FileResourceServiceTest extends PlaySpec with GuiceOneAppPerTest with Injecting {

  "get file" should {

    "return encoded content of file" in {
      val service = inject[FileResourceService]

      // Get encoded hello world and convert it to bytes
      val is = service.getFile("hello-world")
      val bytes = IOUtils.toByteArray(is)
      is.close()

      // Decode bytes
      val received = bytes.map(_.toChar).mkString

      // Load file and check if it is the same
      val expected = Source.fromURL(getClass.getResource("/test-file-resources/hello-world")).mkString
      received mustBe expected
    }

    "throw not found exception on invalid format of file key" in {
      val service = inject[FileResourceService]

      a[NotFoundException] must be thrownBy {
        service.getFile("unexpected-characters/.")
      }
    }

    "throw not found exception on missing file for provided file key" in {
      val service = inject[FileResourceService]

      a[NotFoundException] must be thrownBy {
        service.getFile("not-existing-file")
      }
    }
  }

  "get encoded file" should {

    "return encoded content of file" in {
      val service = inject[FileResourceService]

      // Get encoded hello world and convert it to bytes
      val is = service.getEncodedFile("hello-world")
      val encodedBytes = IOUtils.toByteArray(is)
      is.close()

      // Decode bytes
      val decodedBytes = encodedBytes.map(b => (b ^ 0xff).toByte)
      val decoded = decodedBytes.map(_.toChar).mkString

      // Load file and check if it is the same
      val expected = Source.fromURL(getClass.getResource("/test-file-resources/hello-world")).mkString
      decoded mustBe expected
    }

    "throw not found exception on invalid format of file key" in {
      val service = inject[FileResourceService]

      a[NotFoundException] must be thrownBy {
        service.getEncodedFile("unexpected-characters/.")
      }
    }

    "throw not found exception on missing file for provided file key" in {
      val service = inject[FileResourceService]

      a[NotFoundException] must be thrownBy {
        service.getEncodedFile("not-existing-file")
      }
    }
  }
}
