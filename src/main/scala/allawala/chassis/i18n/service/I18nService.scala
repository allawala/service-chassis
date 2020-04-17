package allawala.chassis.i18n.service

import java.util.Locale

import javax.inject.Inject
import akka.http.scaladsl.model.HttpRequest
import allawala.chassis.config.model.LanguageConfig
import allawala.chassis.i18n.I18nMessages
import com.osinka.i18n.Lang
import com.typesafe.scalalogging.StrictLogging

import scala.util.{Failure, Success, Try}

class I18nService @Inject()(val languageConfig: LanguageConfig) extends StrictLogging {
  // TODO move default lang to conf
  protected val DefaultLang = Lang("en")

  // Logging messages should be independent of the client's locale
  def getForDefaultLocale(code: String, messageParameters: Seq[AnyRef] = Seq.empty): String = {
    getMessage(code, messageParameters, DefaultLang)
  }

  def getForDefaultLocaleFromResource(resource: String, code: String, messageParameters: Seq[AnyRef] = Seq.empty): String = {
    getMessage(resource, code, messageParameters, DefaultLang)
  }

  def getForRequest(request: HttpRequest, code: String, messageParameters: Seq[AnyRef] = Seq.empty): String = {
    getMessage(code, messageParameters, getLanguage(request))
  }

  def getForRequestFromResource(request: HttpRequest, resource: String, code: String, messageParameters: Seq[AnyRef] = Seq.empty): String = {
    getMessage(resource, code, messageParameters, getLanguage(request))
  }

  def get(code: String, langLoc: String, messageParameters: Seq[AnyRef] = Seq.empty): String = {
    getMessage(code, messageParameters, getLang(langLoc))
  }

  def getFromResource(resource: String, code: String, langLoc: String, messageParameters: Seq[AnyRef] = Seq.empty): String = {
    getMessage(resource, code, messageParameters, getLang(langLoc))
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

  private def getMessage(resource: String, code: String, messageParameters: Seq[AnyRef], lang: Lang): String = {
    getI18nMessage(Some(resource), code, messageParameters, lang)
  }

  private def getMessage(code: String, messageParameters: Seq[AnyRef], lang: Lang): String = {
    getI18nMessage(None, code, messageParameters, lang)
  }

    /*
    We need to cater for not finding the message for the provided code and not throw and exception
   */
  private def getI18nMessage(resource: Option[String], code: String, messageParameters: Seq[AnyRef], lang: Lang): String = {
    Try(I18nMessages(resource, code, messageParameters:_*)(lang)) match {
      case Success(msg) =>
        msg
      case Failure(e) =>
        logger.error(s"unable to get message for $code", e)
        code
    }
  }
}
