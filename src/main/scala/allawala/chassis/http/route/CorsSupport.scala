package allawala.chassis.http.route

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.{Allow, HttpOrigin}
import akka.http.scaladsl.server.{Directives, MethodRejection, RejectionHandler}
import allawala.chassis.auth.shiro.route.RouteSecurity
import ch.megard.akka.http.cors.scaladsl.model.HttpOriginMatcher
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings

trait CorsSupport extends Directives {
  private val AllOrigins = "*"
  def allowedOrigins: Seq[String]

  /*
    Generic options http method handling with akka http
    http://hacking-scala.org/post/122084354623/generic-options-http-method-handling-with
   */
  def optionsRejectionHandler: RejectionHandler =
    RejectionHandler.newBuilder().handleAll[MethodRejection] { rejections =>
      val methods = rejections.map(_.supported)
      lazy val names = methods.map(_.name).mkString(", ")

      respondWithHeader(Allow(methods)) {
        options {
          complete(OK -> s"Supported methods : $names.")
        } ~
          complete(MethodNotAllowed -> s"HTTP method not allowed, supported methods: $names!")
      }
    }.result()

  private lazy val httpOriginRange: HttpOriginMatcher = {
    allowedOrigins.find(_.trim == AllOrigins) match {
      case Some(_) => HttpOriginMatcher.*
      case None => HttpOriginMatcher(allowedOrigins.map(HttpOrigin(_)): _*)
    }
  }

  lazy val corsSettings: CorsSettings = CorsSettings.defaultSettings
    .withAllowedOrigins(httpOriginRange)
    .withAllowedMethods(scala.collection.immutable.Seq(OPTIONS, POST, PUT, GET, DELETE))
    .withExposedHeaders(scala.collection.immutable.Seq(RouteSecurity.Authorization))
}
