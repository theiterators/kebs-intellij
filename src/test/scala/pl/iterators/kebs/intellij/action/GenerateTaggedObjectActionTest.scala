package pl.iterators.kebs.intellij.action

import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.testFramework.LightPlatformTestCase
import org.jetbrains.plugins.scala.inWriteAction
import org.junit.Assert.{assertEquals, assertNotNull}
import pl.iterators.kebs.intellij.ScalaLightCodeInsightFixtureTestCaseWithKebs

class GenerateTaggedObjectActionTest extends ScalaLightCodeInsightFixtureTestCaseWithKebs {

  private val actionId = "kebs.tagged-object-generate"

  def testNoPackage(): Unit = {
    val code = StringUtil.convertLineSeparators(s"""
      |case class B${CARET}oat(name: String, length: Int)
      |""".stripMargin)
    getFixture.configureFromExistingVirtualFile(saveBoatScalaFile(None, code))

    getFixture.performEditorAction(actionId)

    val expectedTagsFile =
      """import pl.iterators.kebs.tagged._
        |import pl.iterators.kebs.tag.meta.tagged
        |
        |@tagged object BoatTags {
        |  trait BoatNameTag
        |  type BoatName = String @@ BoatNameTag
        |
        |  trait BoatLengthTag
        |  type BoatLength = Int @@ BoatLengthTag
        |}
        |""".stripMargin
    assertEquals(expectedTagsFile, getBoatTagsScalaFile.getText)
  }

  def testInPackage(): Unit = {
    val code = StringUtil.convertLineSeparators(s"""package example
        |case class B${CARET}oat(name: String, length: Int)
        |""".stripMargin)
    getFixture.configureFromExistingVirtualFile(saveBoatScalaFile(Some("example"), code))

    getFixture.performEditorAction(actionId)

    val expectedTagsFile =
      """package example
        |
        |import pl.iterators.kebs.tagged._
        |import pl.iterators.kebs.tag.meta.tagged
        |
        |@tagged object BoatTags {
        |  trait BoatNameTag
        |  type BoatName = String @@ BoatNameTag
        |
        |  trait BoatLengthTag
        |  type BoatLength = Int @@ BoatLengthTag
        |}
        |""".stripMargin
    assertEquals(expectedTagsFile, getBoatTagsScalaFile.getText)
  }

  def testIgnoringComplexTypes(): Unit = {
    val code = StringUtil.convertLineSeparators(s"""package example
        |case class Address(city: String, country: String)
        |
        |case class B${CARET}oat(name: String, length: Int, portAddress: Address)
        |""".stripMargin)
    getFixture.configureFromExistingVirtualFile(saveBoatScalaFile(Some("example"), code))

    getFixture.performEditorAction(actionId)

    val expectedTagsFile =
      """package example
        |
        |import pl.iterators.kebs.tagged._
        |import pl.iterators.kebs.tag.meta.tagged
        |
        |@tagged object BoatTags {
        |  trait BoatNameTag
        |  type BoatName = String @@ BoatNameTag
        |
        |  trait BoatLengthTag
        |  type BoatLength = Int @@ BoatLengthTag
        |}
        |""".stripMargin
    assertEquals(expectedTagsFile, getBoatTagsScalaFile.getText)
  }

  def testAddingUUIDToImports(): Unit = {
    val code = StringUtil.convertLineSeparators(s"""package example
        |
        |import java.util.UUID
        |
        |case class Address(city: String, country: String)
        |
        |case class B${CARET}oat(id: UUID, name: String, length: Int, portAddress: Address)
        |""".stripMargin)
    getFixture.configureFromExistingVirtualFile(saveBoatScalaFile(Some("example"), code))

    getFixture.performEditorAction(actionId)

    val expectedTagsFile =
      """package example
        |
        |import pl.iterators.kebs.tagged._
        |import pl.iterators.kebs.tag.meta.tagged
        |
        |import java.util.UUID
        |
        |@tagged object BoatTags {
        |  trait BoatIdTag
        |  type BoatId = UUID @@ BoatIdTag
        |
        |  trait BoatNameTag
        |  type BoatName = String @@ BoatNameTag
        |
        |  trait BoatLengthTag
        |  type BoatLength = Int @@ BoatLengthTag
        |}
        |""".stripMargin
    assertEquals(expectedTagsFile, getBoatTagsScalaFile.getText)
  }

  def testChangeTypesInCaseClass(): Unit = {
    val code = StringUtil.convertLineSeparators(s"""package example
        |case class Address(city: String, country: String)
        |
        |case class B${CARET}oat(name: String, length: Int, portAddress: Address)
        |""".stripMargin)
    getFixture.configureFromExistingVirtualFile(saveBoatScalaFile(Some("example"), code))

    getFixture.performEditorAction(actionId)

    val expectedCaseClassFile =
      s"""package example
         |
         |import example.BoatTags.{BoatLength, BoatName}
         |
         |case class Address(city: String, country: String)
         |
         |case class Boat(name: BoatName, length: BoatLength, portAddress: Address)
         |""".stripMargin
    assertEquals(expectedCaseClassFile, getFixture.getFile.getText)
  }

  def testContainers(): Unit = {
    val code = StringUtil.convertLineSeparators(s"""package example
        |case class B${CARET}oat(name: String, alternativeNames: Seq[String], length: Option[Int])
        |""".stripMargin)
    getFixture.configureFromExistingVirtualFile(saveBoatScalaFile(Some("example"), code))

    getFixture.performEditorAction(actionId)

    val expectedTagsFile =
      """package example
        |
        |import pl.iterators.kebs.tagged._
        |import pl.iterators.kebs.tag.meta.tagged
        |
        |@tagged object BoatTags {
        |  trait BoatNameTag
        |  type BoatName = String @@ BoatNameTag
        |
        |  trait BoatAlternativeNameTag
        |  type BoatAlternativeName = String @@ BoatAlternativeNameTag
        |
        |  trait BoatLengthTag
        |  type BoatLength = Int @@ BoatLengthTag
        |}
        |""".stripMargin
    assertEquals(expectedTagsFile, getBoatTagsScalaFile.getText)
    val expectedCaseClassFile =
      s"""package example
         |
         |import example.BoatTags.{BoatAlternativeName, BoatLength, BoatName}
         |
         |case class Boat(name: BoatName, alternativeNames: Seq[BoatAlternativeName], length: Option[BoatLength])
         |""".stripMargin
    assertEquals(expectedCaseClassFile, getFixture.getFile.getText)
  }

  private def saveBoatScalaFile(packageName: Option[String], code: String): VirtualFile =
    inWriteAction {
      val sourceRoot = LightPlatformTestCase.getSourceRoot
      val packageDirectory = packageName match {
        case Some(name) => sourceRoot.createChildDirectory(null, name)
        case None       => sourceRoot
      }
      val file = packageDirectory.createChildData(null, "Boat.scala")
      getFixture.saveText(file, code)
      file
    }

  private def getBoatTagsScalaFile: PsiFile = {
    val file = getFixture.getFile.getContainingDirectory.findFile("BoatTags.scala")
    assertNotNull(file)
    file
  }
}
