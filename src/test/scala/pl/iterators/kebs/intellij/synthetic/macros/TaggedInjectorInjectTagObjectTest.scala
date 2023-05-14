package pl.iterators.kebs.intellij.synthetic.macros

import com.intellij.openapi.vfs.VfsUtil
import com.intellij.testFramework.LightPlatformTestCase
import org.jetbrains.plugins.scala.inWriteAction

class TaggedInjectorInjectTagObjectTest extends MacrosTest {

  private val taggedExampleCode =
    s"""
       |import pl.iterators.kebs.tagged._
       |import pl.iterators.kebs.tag.meta.tagged
       |
       |@tagged object TaggedExample {
       |  trait CompanyTag
       |  trait DepartmentTag
       |  trait PositionTag
       |  trait NameTag
       |  trait IdTag[+A]
       |
       |  type Company    = String @@ CompanyTag
       |  type Department = String @@ DepartmentTag
       |  type Position   = String @@ PositionTag
       |  type Name       = String @@ NameTag
       |  type Id[A]      = Int @@ IdTag[A]
       |
       |  object Name {
       |    val empty = "".asInstanceOf[Name]
       |  }
       |  object DepartmentTag {}
       |  object PositionTag {}
       |}
       |""".stripMargin

  override protected val code = ""

  override def setUp(): Unit = {
    super.setUp()
    inWriteAction {
      val sourceRoot = LightPlatformTestCase.getSourceRoot
      VfsUtil.saveText(
        sourceRoot.createChildData(null, "TaggedExample.scala"),
        taggedExampleCode
      )
    }
  }

  private def testCode(testName: String, implicitText: String): String =
    s"""
      |import pl.iterators.kebs.macros.CaseClass1Rep
      |import TaggedExample._
      |
      |object $testName {
      |  $implicitText
      |}
      |""".stripMargin

  def testImplicitWhenNoTagObjectAndNoTaggedTypeObject(): Unit =
    checkTextHasNoErrors(testCode("NoTagObjectAndNoTaggedTypeObject", "implicitly[CaseClass1Rep[Company, String]]"))

  def testImplicitWhenTagObjectAndNoTaggedTypeObject(): Unit =
    checkTextHasNoErrors(testCode("TagObjectAndNoTaggedTypeObject", "implicitly[CaseClass1Rep[Department, String]]"))

  def testImplicitWhenNoTagObjectAndTaggedTypeObject(): Unit =
    checkTextHasNoErrors(testCode("NoTagObjectAndTaggedTypeObject", "implicitly[CaseClass1Rep[Name, String]]"))

  def testImplicitWhenTagObjectAndTaggedTypeObject(): Unit =
    checkTextHasNoErrors(testCode("TagObjectAndTaggedTypeObject", "implicitly[CaseClass1Rep[Position, String]]"))

  def testImplicitForGenericTag(): Unit =
    checkTextHasNoErrors(testCode("GenericTag", "implicitly[CaseClass1Rep[Id[String], Int]]"))
}
