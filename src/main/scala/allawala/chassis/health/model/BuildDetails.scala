package allawala.chassis.health.model

// buildTime is in UTC
case class BuildDetails(
                         name: Option[String] = None,
                         version: Option[String] = None,
                         buildTime: Option[String] = None,
                         branch: Option[String] = None,
                         sha: Option[String] = None
                       )
