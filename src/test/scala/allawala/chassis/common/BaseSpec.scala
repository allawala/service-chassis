package allawala.chassis.common

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}

trait BaseSpec extends AnyWordSpecLike
  with Matchers
  with EvenMoreSugar
  with BeforeAndAfter
  with BeforeAndAfterAll
