package allawala.chassis.i18n

import java.text.MessageFormat
import java.util.{Locale, ResourceBundle}

import com.osinka.i18n.Lang

/**
  * Copied from com.osinka.i18n.{Lang, Messages}
  * Overrides file extension from .txt to .properties
  * Allows to specify custom file name
  * Important!! if the custom file name does not exist, it will not try to fallback to default file name
  */
trait I18nMessages {
  val DefaultFileName = "messages"
  val FileExt = "properties"

  /** get the message w/o formatting */
  def raw(resource: Option[String], msg: String)(implicit lang: Lang): String = {
    val bundle = ResourceBundle.getBundle(resource.getOrElse(DefaultFileName), lang.locale, UTF8BundleControl)
    bundle.getString(msg)
  }

  def apply(resource: Option[String], msg: String, args: Any*)(implicit lang: Lang): String = {
    new MessageFormat(raw(resource, msg), lang.locale).format(args.map(_.asInstanceOf[java.lang.Object]).toArray)
  }
}

object I18nMessages extends I18nMessages

// @see https://gist.github.com/alaz/1388917
// @see http://stackoverflow.com/questions/4659929/how-to-use-utf-8-in-resource-properties-with-resourcebundle
private[i18n] object UTF8BundleControl extends ResourceBundle.Control {
  val Format = "properties.utf8"

  override def getFormats(baseName: String): java.util.List[String] = {
    import collection.JavaConverters._

    Seq(Format).asJava
  }

  override def getFallbackLocale(baseName: String, locale: Locale): Locale =
    if (locale == Locale.getDefault) {
      null
    }
    else {
      Locale.getDefault
    }

  override def newBundle(baseName: String, locale: Locale, fmt: String, loader: ClassLoader, reload: Boolean): ResourceBundle = {
    import java.io.InputStreamReader
    import java.util.PropertyResourceBundle

    // The below is an approximate copy of the default Java implementation
    def resourceName = toResourceName(toBundleName(baseName, locale), I18nMessages.FileExt)

    def stream =
      if (reload) {
        for {
          url <- Option(loader getResource resourceName)
          connection <- Option(url.openConnection)
        } yield {
          connection.setUseCaches(false)
          connection.getInputStream
        }
      } else {
        Option(loader getResourceAsStream resourceName)
      }

    (for {
      format <- Option(fmt) if format == Format
      is <- stream
    } yield new PropertyResourceBundle(new InputStreamReader(is, "UTF-8"))).orNull
  }
}
