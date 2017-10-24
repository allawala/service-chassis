package allawala.chassis.util

import java.math.BigInteger
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import javax.xml.bind.DatatypeConverter

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
    DatatypeConverter.printHexBinary(array)
  }

  // From com.softwaremill.session.SessionUtil
  def hexStringToByte(hexString: String): Array[Byte] = {
    DatatypeConverter.parseHexBinary(hexString)
  }
}
