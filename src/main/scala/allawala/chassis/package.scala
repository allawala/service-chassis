package allawala

package object chassis {
  /*
    Seq is not aliased to immutable by default like the other collection types,
    https://hseeberger.wordpress.com/2013/10/25/attention-seq-is-not-immutable/
  */
  type Iterable[+A] = scala.collection.immutable.Iterable[A]

  type Seq[+A] = scala.collection.immutable.Seq[A]
  val Seq = scala.collection.immutable.Seq

  type IndexedSeq[+A] = scala.collection.immutable.IndexedSeq[A]
}
