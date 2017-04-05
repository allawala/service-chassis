package allawala.chassis.config.model

case class HttpConfig(host: String, port: Int)
case class Configuration(name: String, httpConfig: HttpConfig)
