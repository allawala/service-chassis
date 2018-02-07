package allawala.chassis.config.model

case class BaseConfig(
                       name: String,
                       httpConfig: HttpConfig,
                       languageConfig: LanguageConfig,
                       corsConfig: CorsConfig,
                       logstash: Logstash,
                       auth: Auth
                     )
