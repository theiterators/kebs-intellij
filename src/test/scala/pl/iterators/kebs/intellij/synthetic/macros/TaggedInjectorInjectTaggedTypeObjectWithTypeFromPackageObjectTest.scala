package pl.iterators.kebs.intellij.synthetic.macros

import com.intellij.openapi.vfs.VfsUtil
import com.intellij.testFramework.LightPlatformTestCase
import org.jetbrains.plugins.scala.ScalaLightCodeInsightFixtureTestAdapter.normalize
import org.jetbrains.plugins.scala.inWriteAction

class TaggedInjectorInjectTaggedTypeObjectWithTypeFromPackageObjectTest extends MacrosTest {

  private val packageObject =
    s"""package test
       |
       |import pl.iterators.kebs.tagged._
       |
       |import java.util.UUID
       |
       |package object example {
       |  type UUIDId = UUID
       |  object UUIDId {
       |    def generate[T]: UUIDId @@ T = UUID.randomUUID().taggedWith[T]
       |    def fromString[T](str: String): UUIDId @@ T =
       |      UUID.fromString(str).taggedWith[T]
       |  }
       |}
       |""".stripMargin

  private val taggedExample =
    s"""package test.example
       |
       |import pl.iterators.kebs.tagged._
       |import pl.iterators.kebs.tag.meta.tagged
       |
       |@tagged object TaggedExample {
       |  trait ExampleIdTag
       |  type ExampleId = UUIDId @@ ExampleIdTag
       |}
       |""".stripMargin

  override protected val code = ""

  override def setUp(): Unit = {
    super.setUp()
    inWriteAction {
      val testSourceRoot = LightPlatformTestCase.getSourceRoot.createChildDirectory(null, "test")
      VfsUtil.saveText(testSourceRoot.createChildData(null, "package.scala"), normalize(packageObject))
      VfsUtil.saveText(
        testSourceRoot
          .createChildDirectory(null, "example")
          .createChildData(null, "TaggedExample.scala"),
        normalize(taggedExample)
      )
    }
  }

  def testCompile(): Unit =
    checkTextHasNoErrors(s"""package test.example
         |
         |object TaggedExampleUsage {
         |  import TaggedExample._
         |
         |  object Examples {
         |    ExampleId.apply(UUIDId.generate)
         |    ExampleId.from(UUIDId.generate)
         |    ExampleId(UUIDId.generate)
         |  }
         |}
         |""".stripMargin)
}
