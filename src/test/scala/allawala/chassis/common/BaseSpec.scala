package allawala.chassis.common

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Matchers, WordSpecLike}

trait BaseSpec extends WordSpecLike
  with Matchers
  with EvenMoreSugar
  with BeforeAndAfter
  with BeforeAndAfterAll
