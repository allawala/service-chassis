package allawala.chassis.i18n.service

import java.util.Locale
import javax.inject.Inject

import akka.http.scaladsl.model.HttpRequest
import allawala.chassis.config.model.LanguageConfig
import com.osinka.i18n.{Lang, Messages}
import com.typesafe.scalalogging.StrictLogging

import scala.util.{Failure, Success, Try}

class I18nService @Inject()(val languageConfig: LanguageConfig) extends StrictLogging {
  protected val DefaultLang = Lang("en")

  // Logging messages should be independent of the client's locale
  def getDefaultLocale(code: String, messageParameters: Seq[AnyRef] = Seq.empty): String = {
    getMessage(code, messageParameters, DefaultLang)
  }

  def get(request: HttpRequest, code: String, messageParameters: Seq[AnyRef]): String = {
    getMessage(code, messageParameters, getLanguage(request))
  }

  protected[service] def getLanguage(request: HttpRequest) = {
    val query = request.uri.query()
    val opt = if (languageConfig.parameter.isDefined && query.get(languageConfig.parameter.get).isDefined) {
      query.get(languageConfig.parameter.get)
    } else if (languageConfig.header.isDefined) {
      request.headers.find(_.name == languageConfig.header.get).map(_.value())
    } else {
      None
    }
    opt.map(getLang).getOrElse(DefaultLang)
  }

  protected[service] def getLang(value: String) = {
    val langAndLocale = value.split(",").head.split("-")
    val lang = langAndLocale.head.toLowerCase
    val locale = langAndLocale.tail.headOption.map(_.toUpperCase)

    locale match {
      case Some(loc) => Lang(new Locale(lang, loc))
      case None => Lang(new Locale(lang))
    }
  }

  /*
    We need to cater for not finding the message for the provided code and not throw and exception
   */
  private def getMessage(code: String, messageParameters: Seq[AnyRef], lang: Lang): String = {
    Try(Messages(code, messageParameters:_*)(lang)) match {
      case Success(msg) =>
        msg
      case Failure(e) =>
        logger.error(s"unable to get message for $code", e)
        code
    }
  }
}
