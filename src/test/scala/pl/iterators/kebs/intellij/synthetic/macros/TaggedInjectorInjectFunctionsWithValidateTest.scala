package pl.iterators.kebs.intellij.synthetic.macros

import org.junit.Assert.assertEquals

class TaggedInjectorInjectFunctionsWithValidateTest extends MacrosTest {

  override protected val code =
    s"""package test.unit
       |
       |import pl.iterators.kebs.tagged._
       |import pl.iterators.kebs.tag.meta.tagged
       |
       |@tagged object TaggedExample {
       |  trait PositiveIntTag
       |
       |  type PositiveInt = Int @@ PositiveIntTag
       |
       |  object Positive${CARET}Int {
       |    sealed trait Error
       |    case object Negative extends Error
       |    case object Zero     extends Error
       |
       |    def validate(i: Int): Either[Error, Int] =
       |      if (i == 0) Left(Zero) else if (i < 0) Left(Negative) else Right(i)
       |  }
       |
       |}
       |""".stripMargin

  def testCompile(): Unit =
    checkTextHasNoErrors(code)

  def testPositiveIntApply(): Unit =
    assertEquals("def apply(arg: Int): PositiveInt = ???", method("apply").getText)

  def testPositiveIntFrom(): Unit =
    assertEquals("def from(arg: Int): Either[PositiveInt.Error, PositiveInt] = ???", method("from").getText)
}
