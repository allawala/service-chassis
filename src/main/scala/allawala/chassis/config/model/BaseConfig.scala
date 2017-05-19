package allawala.chassis.config.model

case class BaseConfig(name: String, httpConfig: HttpConfig, corsConfig: CorsConfig, logstash: Logstash, auth: Auth)
