package allawala.chassis.i18n.service

import java.util.Locale

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpHeader, HttpRequest, Uri}
import allawala.chassis.common.BaseSpec
import allawala.chassis.config.model.LanguageConfig

import scala.collection.immutable

class I18nServiceSpec extends BaseSpec {
  private val langConfig = mock[LanguageConfig]
  private val service = new I18nService(langConfig)

  "i18n service" should {
    "should use query parameter to parse language" in {
      langConfig.parameter returns Some("lang")
      val request = HttpRequest(uri = Uri("http://localhost:8080?lang=fr"))

      val lang = service.getLanguage(request)

      lang.locale should ===(Locale.FRENCH)
    }

    "should use header to parse language" in {
      langConfig.parameter returns None
      langConfig.header returns Some("Accept-Language")
      val header = RawHeader("Accept-Language", "fr")
      val request = HttpRequest(uri = Uri("http://localhost:8080"), headers = immutable.Seq[HttpHeader](header))

      val lang = service.getLanguage(request)

      lang.locale should ===(Locale.FRENCH)
    }

    "should use use query parameter over header if both are present" in {
      langConfig.parameter returns Some("lang")
      langConfig.header returns Some("Accept-Language")
      val header = RawHeader("Accept-Language", "fr")
      val request = HttpRequest(uri = Uri("http://localhost:8080?lang=en"), headers = immutable.Seq[HttpHeader](header))

      val lang = service.getLanguage(request)

      lang.locale should ===(Locale.ENGLISH)
    }

    "should use default language if neither header or parameter are present" in {
      langConfig.parameter returns None
      langConfig.header returns None
      val request = HttpRequest(uri = Uri("http://localhost:8080"))

      val lang = service.getLanguage(request)

      lang.locale should ===(Locale.ENGLISH)
    }

    "should use default language if neither header or parameter match the configuration" in {
      langConfig.parameter returns Some("language")
      langConfig.header returns Some("X-LANG")
      val header = RawHeader("Accept-Language", "fr")
      val request = HttpRequest(uri = Uri("http://localhost:8080?lang=de"), headers = immutable.Seq[HttpHeader](header))

      val lang = service.getLanguage(request)

      lang.locale should ===(Locale.ENGLISH)
    }

    "parse the language" in {
      val lang = service.getLang("en")

      lang.locale should ===(Locale.ENGLISH)
    }

    "parse the language and country" in {
      val lang = service.getLang("en-US")

      lang.locale should ===(Locale.US)
    }

    "parse the language and country ignoring the weighting" in {
      val lang = service.getLang("en-US,en;q=0.5")

      lang.locale should ===(Locale.US)
    }
  }
}
