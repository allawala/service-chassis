package allawala.chassis.auth.service

import java.time.temporal.TemporalAmount
import java.time.{Duration => JDuration}
import javax.inject.Inject

import allawala.chassis.auth.model.{JWTSubject, PrincipalType}
import allawala.chassis.config.model.{Auth, RefreshStrategy}
import allawala.chassis.core.exception.{DomainException, ServerException}
import allawala.chassis.util.DateTimeProvider
import io.circe.Json
import io.circe.parser.parse
import org.threeten.extra.Temporals
import pdi.jwt.algorithms.JwtRSAAlgorithm
import pdi.jwt.exceptions.JwtExpirationException
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtOptions}

import scala.util.{Failure, Success}

class Rsa512JWTTokenServiceImpl @Inject()(
                                           val auth: Auth,
                                           val dateTimeProvider: DateTimeProvider
                                         ) extends JWTTokenService {
  override val jwtAlgorithm: JwtRSAAlgorithm = JwtAlgorithm.RS512

  override def generateToken(principalType: PrincipalType, principal: String, expiresIn: TemporalAmount): String = {
    val now = dateTimeProvider.now
    val parsed = parse(
      s"""
         |{
         |"iat":${now.getEpochSecond},
         |"exp":${now.plus(expiresIn).getEpochSecond},
         |"sub":"$principal",
         |"typ":"${principalType.entryName}",
         |"rnd":${now.toEpochMilli}
         |}
         |""".stripMargin
    )
    val claimJson = parsed.getOrElse(throw new IllegalStateException("Unable to generate token"))

    JwtCirce.encode(claimJson, auth.rsa.privateKey, jwtAlgorithm)
  }

  /*
    When rememberMe is true and the refreshStrategy is simple, we issue only a jwt token with the expiration that of the
    refresh token. There is no explicit refresh token when using the simple strategy.
   */
  override def generateToken(principalType: PrincipalType, principal: String, rememberMe: Boolean): String = {
    def expiresIn = {
      val expiration = auth.expiration
      val duration = if (!rememberMe) {
        expiration.expiry
      } else {
        RefreshStrategy.withName(expiration.refreshTokenStrategy) match {
          case RefreshStrategy.Full => expiration.expiry
          case RefreshStrategy.Simple => expiration.refreshTokenExpiry // issue the token using the refreshTokenExpiry
        }
      }

      JDuration.of(duration._1, Temporals.chronoUnit(duration._2))
    }

    generateToken(principalType, principal, expiresIn)
  }

  override def decodeToken(token: String): Either[DomainException, JWTSubject] = {
    JwtCirce.decodeJson(token, auth.rsa.publicKey, Seq(jwtAlgorithm)) match {
      case Success(json) => decodeJWTSubject(json, token)
      case Failure(e) => Left(ServerException("token.invalid", cause = e))
    }
  }

  override def decodeExpiredToken(token: String): Either[DomainException, JWTSubject] = {
    // Ignoring the expiration when decoding
    JwtCirce.decodeJson(token, auth.rsa.publicKey, Seq(jwtAlgorithm), JwtOptions(expiration = false)) match {
      case Success(json) => decodeJWTSubject(json, token)
      case Failure(e) => Left(ServerException("token.invalid", cause = e))
    }
  }

  private def decodeJWTSubject(json: Json, token: String) = {
    getSubjectDetails(json) match {
      case Some((typ, sub)) => Right(JWTSubject(PrincipalType.withName(typ), sub, token))
      case _ => Left(ServerException("token.invalid", logMap = Map("reason" -> "missing either typ or sub")))
    }
  }

  override def isExpired(jwtToken: String): Boolean = {
    JwtCirce.decodeJson(jwtToken, auth.rsa.publicKey, Seq(jwtAlgorithm)) match {
      case Success(_) => false
      case Failure(e) => e match {
        case _: JwtExpirationException => true
        case _ => false
      }
    }
  }

  /*
    Important!! This will fail to decode if its expired. If we need to check if an expired token can be decoded, we will need
    another method
   */
  override def canDecodeToken(jwtToken: String): Boolean = {
    JwtCirce.decodeJson(jwtToken, auth.rsa.publicKey, Seq(jwtAlgorithm)) match {
      case Success(_) => true
      case Failure(_) => false
    }
  }

  private def getSubjectDetails(json: Json): Option[(String, String)] = {
    import io.circe.optics.JsonPath._

    val typ = root.typ.string.getOption(json)
    val principal = root.sub.string.getOption(json)

    for {
      t <- typ
      p <- principal
    } yield (t, p)
  }
}
