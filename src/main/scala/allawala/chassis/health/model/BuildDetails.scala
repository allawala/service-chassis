package allawala.chassis.health.model

// buildTime is in UTC
case class BuildDetails(name: String, version: String, buildTime: String, branch: String, sha: Option[String])
