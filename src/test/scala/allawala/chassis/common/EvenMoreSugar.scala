package allawala.chassis.common

import org.mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.{Answer, OngoingStubbing}
import org.scalatest.mockito.MockitoSugar

import scala.collection.JavaConverters._
import scala.language.implicitConversions
import scala.reflect.ClassTag

trait EvenMoreSugar extends MockitoSugar {

  /** @return an object supporting the stub methods. */
  implicit def theStubbed[T](c: => T): Stubbed[T] = new Stubbed(c)

  implicit def pimpedInvocationOnMock(im: InvocationOnMock): PimpedInvocationOnMock = new PimpedInvocationOnMock(im)

  class Stubbed[T](c: => T) {
    def returns(t: T, t2: T*): OngoingStubbing[T] = {
      if (t2.isEmpty) {
        Mockito.when(c).thenReturn(t)
      }
      else {
        t2.foldLeft(Mockito.when(c).thenReturn(t)) {
          (res, cur) => res.thenReturn(cur)
        }
      }
    }

    def returnsArgument(position: Int = 0): OngoingStubbing[T] = Mockito.when(c).`then`(AdditionalAnswers.returnsArgAt(position))

    def answers(f: InvocationOnMock => T): OngoingStubbing[T] = Mockito.when(c).thenAnswer(new RiddleMeThis[T](f))

    def getArgument[V](c: Class[V], position: Int)(i: InvocationOnMock): V = i.getArguments()(position).asInstanceOf[V]

    def r[V](f: InvocationOnMock => V, g: V => T)(i: InvocationOnMock): T = g(f(i))

    def answersWithArgument[V](position: Int = 0, c: Class[V], f: V => T): OngoingStubbing[T] = answers(r(getArgument(c, position), f))

    def throws(ex: Throwable): OngoingStubbing[T] = Mockito.when(c).thenThrow(ex)
  }

  class PimpedInvocationOnMock(im: InvocationOnMock) {
    def arg[A: ClassTag](idx: Int): A = im.getArguments()(idx) match {
      case a if scala.reflect.classTag[A].runtimeClass.isInstance(a) => a.asInstanceOf[A]
      case _ => throw new IllegalArgumentException("No argument of expected type at idx: " + idx)
    }
  }

  private class RiddleMeThis[T](f: InvocationOnMock => T) extends Answer[T] {
    def answer(invocation: InvocationOnMock): T = f(invocation)
  }

  case class Capturer[T](argumentCaptor: ArgumentCaptor[T]) {
    def get(i: Int): T = argumentCaptor.getAllValues.get(i)

    def arguments: List[T] = argumentCaptor.getAllValues.asScala.toList
  }

  def capture[T](c: Class[T], f: ArgumentCaptor[T] => Any): Capturer[T] = {
    val argumentCaptor = ArgumentCaptor.forClass(c)
    f(argumentCaptor)
    Capturer(argumentCaptor)
  }

  def any[T](implicit mf: Manifest[T]): T = ArgumentMatchers.any(mf.runtimeClass.asInstanceOf[Class[T]])

  def equ[T](t: T): T = ArgumentMatchers.eq(t)

  def same[T](t: T): T = ArgumentMatchers.eq(t)

  def oneOf[T](mock: T): T = Mockito.verify(mock)

  def noneOf[T](mock: T): T = Mockito.verify(mock, Mockito.times(0))

  def times[T](mock: T, calls: Int): T = Mockito.verify(mock, Mockito.times(calls))

  def reset[T](mocks: T*): Unit = Mockito.reset(mocks : _*)
}
