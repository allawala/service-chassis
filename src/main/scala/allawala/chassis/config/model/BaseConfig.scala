package allawala.chassis.config.model

import scala.concurrent.duration.FiniteDuration

case class BaseConfig(
                       name: String,
                       httpConfig: HttpConfig,
                       languageConfig: LanguageConfig,
                       corsConfig: CorsConfig,
                       logstash: Logstash,
                       auth: Auth,
                       awaitTermination: FiniteDuration
                     )
