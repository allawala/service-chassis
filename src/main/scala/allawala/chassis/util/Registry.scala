package allawala.chassis.util

import scala.collection.concurrent.TrieMap

trait Registry[T] {
  private val registry = TrieMap[String, T]()

  def register(name: String, entry: T): Unit = {
    registry += (name -> entry)
  }

  def get(): Seq[T] = {
    registry.values.toSeq
  }
}