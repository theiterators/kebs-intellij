package pl.iterators.kebs.intellij.synthetic.macros

import org.junit.Assert.assertEquals

class TaggedInjectorInjectFunctionsTest extends MacrosTest {

  override protected val code =
    s"""package test.unit
       |
       |import pl.iterators.kebs.tagged._
       |import pl.iterators.kebs.tag.meta.tagged
       |
       |@tagged object TaggedExample {
       |  trait NameTag
       |
       |  type Name  = String @@ NameTag
       |
       |  object N${CARET}ame {
       |    val empty = "".asInstanceOf[Name]
       |  }
       |}
       |""".stripMargin

  def testCompile(): Unit = {
    checkTextHasNoErrors(code)
  }

  def testApply(): Unit = {
    assertEquals(
      "def apply(arg: _root_.scala.Predef.String): Name = ???",
      method("apply").getText
    )
  }

  def testFrom(): Unit = {
    assertEquals(
      "def from(arg: _root_.scala.Predef.String): Name = ???",
      method("from").getText
    )
  }
}
