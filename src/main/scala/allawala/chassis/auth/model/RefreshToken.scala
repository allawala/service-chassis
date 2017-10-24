package allawala.chassis.auth.model

import java.time.Instant

case class RefreshToken(
                         selector: String,
                         encodedSelectorAndToken: Option[String], // DO NOT SAVE THIS IN THE DB
                         tokenHash: String, // THIS IS THE ONE THAT SHOULD BE SAVED IN THE DB
                         expires: Instant)
