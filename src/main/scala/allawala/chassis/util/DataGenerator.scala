package allawala.chassis.util

import java.math.BigInteger
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import scala.collection.mutable

class DataGenerator {
  def uuid: UUID = UUID.randomUUID()
  def uuidStr: String = uuid.toString

  // From com.softwaremill.session.SessionUtil
  def randomString(length: Int): String = {
    // http://stackoverflow.com/questions/41107/how-to-generate-a-random-alpha-numeric-string
    val random = ThreadLocalRandom.current()
    new BigInteger(length * 5, random).toString(32) // because 2^5 = 32
  }

  // From com.softwaremill.session.SessionUtil
  def hashSHA256(value: String): String = {
    val digest = MessageDigest.getInstance("SHA-256")
    toHexString(digest.digest(value.getBytes("UTF-8")))
  }

  // From com.softwaremill.session.SessionUtil
  def toHexString(array: Array[Byte]): String = {
    printHexBinary(array)
  }

  // From com.softwaremill.session.SessionUtil
  def hexStringToByte(hexString: String): Array[Byte] = {
    parseHexBinary(hexString)
  }

  // copied from javax.xml.bind.DatatypeConverterImpl, since java.xml.bind is no longer available in java 11
  private def printHexBinary(data: Array[Byte]): String = {
    import DataGenerator._
    val r: mutable.StringBuilder = new mutable.StringBuilder(data.length * 2)
    for (b <- data) {
      r.append(hexCode((b >> 4) & 0xF))
      r.append(hexCode(b & 0xF))
    }
    r.toString
  }

  private def parseHexBinary(s: String): Array[Byte] = {
    val len = s.length
    // "111" is not a valid hex encoding.
    if (len % 2 != 0) throw new IllegalArgumentException("hexBinary needs to be even-length: " + s)
    val out = new Array[Byte](len / 2)
    var i = 0
    while ( {
      i < len
    }) {
      val h = hexToBin(s.charAt(i))
      val l = hexToBin(s.charAt(i + 1))
      if (h == -1 || l == -1) throw new IllegalArgumentException("contains illegal character for hexBinary: " + s)
      out(i / 2) = (h * 16 + l).toByte

      i += 2
    }
    out
  }

  private def hexToBin(ch: Char): Int = {
    if ('0' <= ch && ch <= '9') {
      ch - '0'
    } else if ('A' <= ch && ch <= 'F') {
      ch - 'A' + 10
    } else if ('a' <= ch && ch <= 'f') {
      ch - 'a' + 10
    } else {
      -1
    }
  }
}

object DataGenerator {
  private val hexCode = "0123456789ABCDEF".toCharArray
}
