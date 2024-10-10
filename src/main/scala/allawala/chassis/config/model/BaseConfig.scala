package allawala.chassis.config.model

import scala.concurrent.duration.FiniteDuration

case class BaseConfig(
                       name: String,
                       httpConfig: HttpConfig,
                       languageConfig: LanguageConfig,
                       auth: Auth,
                       awaitTermination: FiniteDuration
                     )
