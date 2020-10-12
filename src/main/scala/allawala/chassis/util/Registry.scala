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

  def find(f: T => Boolean): Option[T]= {
    registry.values.find(f)
  }

  def findUnsafe(f: T => Boolean): T= {
    registry.values.find(f).getOrElse(throw new RuntimeException("not found in registry"))
  }

  def findAll(f: T => Boolean): Seq[T] = {
    registry.values.filter(f).toSeq
  }
}
