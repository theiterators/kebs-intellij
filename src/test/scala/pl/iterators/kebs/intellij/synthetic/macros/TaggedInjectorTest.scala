package pl.iterators.kebs.intellij.synthetic.macros

import org.junit.Assert.assertEquals

class TaggedInjectorTest extends MacrosTest {

  override protected val code =
    s"""import pl.iterators.kebs.tagged._
       |import pl.iterators.kebs.tag.meta.tagged
       |
       |@tagged object Tagged${CARET}Example {
       |  trait NameTag
       |  trait IdTag[+A]
       |  trait PositiveIntTag
       |
       |  type Name  = String @@ NameTag
       |  type Id[A] = Int @@ IdTag[A]
       |
       |  type PositiveInt = Int @@ PositiveIntTag
       |  object PositiveInt {
       |    sealed trait Error
       |    case object Negative extends Error
       |    case object Zero     extends Error
       |
       |    def validate(i: Int) = if (i == 0) Left(Zero) else if (i < 0) Left(Negative) else Right(i)
       |  }
       |}
       |""".stripMargin

  //FIXME: 'Negative' (Object creation impossible, since member equals(that: Any): Boolean in scala.Equals is not defined)
  def ignoreCompile(): Unit = {
    checkTextHasNoErrors(code)
  }

  def testNameObject(): Unit = {
    assertEquals(
      """object Name {
        |  def apply(arg: String): Name = ???
        |  def from(arg: String): Name = ???
        |}""".stripMargin,
      innerObject("Name").getText
    )
  }

  def testIdObject(): Unit = {
    assertEquals(
      """object Id {
        |  def apply[A](arg: Int): Id[A] = ???
        |  def from[A](arg: Int): Id[A] = ???
        |}""".stripMargin,
      innerObject("Id").getText
    )
  }

  // TODO
  def ignorePositiveIntApply(): Unit = {
    assertEquals(
      "def apply(arg: Int): PositiveInt = ???",
      method("apply", innerObject("PositiveInt")).getText
    )
  }

  // TODO
  def ignorePositiveIntFrom(): Unit = {
    assertEquals(
      "def from(arg: Int): Either[Error, PositiveInt] = ???",
      method("from", innerObject("PositiveInt")).getText
    )
  }
}
