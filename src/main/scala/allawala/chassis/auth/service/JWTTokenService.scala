package allawala.chassis.auth.service

import java.time.temporal.TemporalAmount

import allawala.chassis.auth.model.{JWTSubject, PrincipalType}
import allawala.chassis.core.exception.DomainException
import pdi.jwt.JwtAlgorithm

trait JWTTokenService {
  def jwtAlgorithm: JwtAlgorithm

  def generateToken(principalType: PrincipalType, principal: String, expiresIn: TemporalAmount): String

  def generateToken(principalType: PrincipalType, principal: String, rememberMe: Boolean): String

  def decodeToken(token: String): Either[DomainException, JWTSubject]

  def decodeExpiredToken(token: String): Either[DomainException, JWTSubject]

  def isExpired(token: String): Boolean

  def canDecodeToken(token: String): Boolean
}
