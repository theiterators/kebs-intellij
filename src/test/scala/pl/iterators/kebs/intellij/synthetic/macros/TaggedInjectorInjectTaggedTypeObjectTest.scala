package pl.iterators.kebs.intellij.synthetic.macros

import org.junit.Assert.assertEquals

class TaggedInjectorInjectTaggedTypeObjectTest extends MacrosTest {

  override protected val code =
    s"""package test.unit
       |
       |import pl.iterators.kebs.tagged._
       |import pl.iterators.kebs.tag.meta.tagged
       |
       |@tagged object Tagged${CARET}Example {
       |  trait NameTag
       |  trait IdTag[+A]
       |
       |  type Name  = String @@ NameTag
       |  type Id[A] = Int @@ IdTag[A]
       |}
       |""".stripMargin

  def testCompile(): Unit =
    checkTextHasNoErrors(code)

  def testNameObject(): Unit =
    assertEquals("""object Name {
        |  def apply(arg: _root_.scala.Predef.String): Name = ???
        |  def from(arg: _root_.scala.Predef.String): Name = ???
        |}""".stripMargin, innerObject("Name").getText)

  def testIdObject(): Unit =
    assertEquals("""object Id {
        |  def apply[A](arg: Int): Id[A] = ???
        |  def from[A](arg: Int): Id[A] = ???
        |}""".stripMargin, innerObject("Id").getText)
}
