package allawala.chassis.util

import java.io.{File, FileOutputStream, OutputStreamWriter}
import java.nio.file.Paths
import java.security._

import com.typesafe.scalalogging.StrictLogging
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.io.pem.{PemObject, PemWriter}

/*
  Utility class to generate a public/private key pair files in the user home directory.
   To run, call RSAKeyGenerator.generate()
 */

case class RSAKey(privateKey: PrivateKey, publicKey: PublicKey)

object RSAKeyGenerator extends StrictLogging {
  private val KeySize = 2048
  private val homeDir = System.getProperty("user.home")

  private class PemFile(val key: Key, val description: String) {
    private val pemObject = new PemObject(description, key.getEncoded)

    def write(file: File): Unit = {
      val pemWriter = new PemWriter(new OutputStreamWriter(new FileOutputStream(file)))
      try {
        pemWriter.writeObject(this.pemObject)
      }
      finally {
        pemWriter.close()
      }
    }
  }

  private def generateKeyPair(): KeyPair = {
    val generator = KeyPairGenerator.getInstance("RSA", "BC")
    generator.initialize(KeySize)
    val keyPair = generator.generateKeyPair
    keyPair
  }

  private def writePem(key: Key, description: String, filename: String): Unit = {
    val path = Paths.get(homeDir, filename)
    val file = path.toFile
    val pemFile = new PemFile(key, description)
    pemFile.write(file)
    logger.debug(s"Writing $description to $path/$filename")
  }

  def generate(): RSAKey = {
    Security.addProvider(new BouncyCastleProvider)

    val keyPair = generateKeyPair()
    val privateKey = keyPair.getPrivate
    val publicKey = keyPair.getPublic

    RSAKey(privateKey, publicKey)
  }

  def generateToFile(): Unit = {
    val rsaKey = generate()

    writePem(rsaKey.privateKey, "RSA PRIVATE KEY","id_rsa")
    writePem(rsaKey.publicKey, "RSA PUBLIC KEY", "id_rsa.pub")
  }
}
