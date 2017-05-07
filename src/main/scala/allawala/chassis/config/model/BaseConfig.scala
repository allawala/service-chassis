package allawala.chassis.config.model

case class BaseConfig(name: String, httpConfig: HttpConfig, logstash: Logstash, auth: Auth)
