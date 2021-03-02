package pl.iterators.kebs.intellij.action

import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent, CommonDataKeys}
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.text.Strings.{capitalize, unpluralize}
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.{PsiDirectory, PsiElement, PsiFileFactory, PsiPrimitiveType}
import org.jetbrains.plugins.scala.ScalaLanguage
import org.jetbrains.plugins.scala.lang.psi.api.base.types.ScParameterizedTypeElement
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScClassParameter
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScClass
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory
import org.jetbrains.plugins.scala.lang.psi.types.{ScParameterizedType, ScType}
import org.jetbrains.plugins.scala.project.ProjectContext

class GenerateTaggedObjectAction extends AnAction {

  override def update(actionEvent: AnActionEvent): Unit = {
    val currentElement = actionEvent.getData(CommonDataKeys.PSI_ELEMENT)

    actionEvent.getPresentation.setEnabledAndVisible(shouldBeVisible(currentElement))
  }

  override def actionPerformed(actionEvent: AnActionEvent): Unit = {
    val currentElement = actionEvent.getData(CommonDataKeys.PSI_ELEMENT)
    if (shouldBeVisible(currentElement)) {
      val scClass       = currentElement.asInstanceOf[ScClass]
      val tagsClassName = s"${scClass.getName()}Tags"
      val tagsFileName  = s"$tagsClassName.scala"
      Option(scClass.getContainingFile.getContainingDirectory.findFile(tagsFileName)) match {
        case None =>
          WriteCommandAction.runWriteCommandAction(scClass.getProject, new Runnable() {
            override def run(): Unit = {
              val ccParamsToTag    = caseClassParametersToTag(scClass)
              val tagsFileContent  = generateTaggedObject(tagsClassName, scClass.getPath, ccParamsToTag)
              val packageDirectory = scClass.getContainingFile.getContainingDirectory
              writeTagsFile(scClass.getProject, tagsFileName, packageDirectory, tagsFileContent)
              changeTypesInCaseClass(scClass, tagsClassName, ccParamsToTag)
            }
          })
        case Some(f) =>
          val editor = actionEvent.getData(CommonDataKeys.EDITOR)
          JBPopupFactory
            .getInstance()
            .createMessage(s"${f.getName} already exists!")
            .showInBestPositionFor(editor)
      }
    }
  }

  private def shouldBeVisible(currentElement: PsiElement): Boolean =
    currentElement != null &&
      currentElement.isInstanceOf[ScClass] &&
      currentElement.asInstanceOf[ScClass].isCase

  private def caseClassParametersToTag(scClass: ScClass): Seq[CaseClassParameter] =
    scClass.constructor.toSeq
      .flatMap(
        _.parameters
          .map(CaseClassParameterType(_))
          .filter(_.shouldBeTagged)
      )
      .map(caseClassParameterType => new CaseClassParameter(scClass, caseClassParameterType))

  private def generateTaggedObject(
    name: String,
    packageName: String,
    ccParams: Seq[CaseClassParameter]
  ): String = {
    val tags = ccParams
      .map(_.makeTypeTag)
      .mkString("\n\n")

    val packageDeclaration =
      if (packageName.nonEmpty)
        s"package $packageName\n\n"
      else
        ""

    s"""${packageDeclaration}import pl.iterators.kebs.tagged._
       |import pl.iterators.kebs.tag.meta.tagged
       |
       |@tagged object $name {
       |$tags
       |}
       |""".stripMargin
  }

  private def writeTagsFile(
    project: Project,
    name: String,
    packageDirectory: PsiDirectory,
    tagsFileContent: String
  ): PsiElement =
    packageDirectory.add {
      val tagsFile = PsiFileFactory
        .getInstance(project)
        .createFileFromText(name, ScalaLanguage.INSTANCE, tagsFileContent)
      shortenClassReferences(project, tagsFile)
    }

  private def changeTypesInCaseClass(
    scClass: ScClass,
    tagsName: String,
    ccParamsToTag: Seq[CaseClassParameter]
  ): PsiElement = {
    ccParamsToTag.foreach { caseClassParameter =>
      val taggedType = ScalaPsiElementFactory.createTypeElementFromText(
        s"$tagsName.${caseClassParameter.typeAliasName}"
      )(ProjectContext.fromPsi(scClass))
      caseClassParameter.scClassParameter.typeElement match {
        case Some(parameterizedTypeElement: ScParameterizedTypeElement) =>
          parameterizedTypeElement.typeArgList.typeArgs.head.replace(taggedType)
        case Some(typeElement) => typeElement.replace(taggedType)
        case _                 => ()
      }
    }
    shortenClassReferences(scClass.getProject, scClass)
  }

  private def shortenClassReferences(project: Project, element: PsiElement): PsiElement =
    JavaCodeStyleManager.getInstance(project).shortenClassReferences(element)
}

private case class CaseClassParameterType(scClassParameter: ScClassParameter) {
  import CaseClassParameterType._
  lazy val `type`: Option[ScType] = scClassParameter
    .`type`()
    .toOption

  def shouldBeTagged: Boolean = isPrimitive || isAllowedSimpleType || isAllowedComplexType

  private lazy val isPrimitive: Boolean = scClassParameter
    .getType()
    .isInstanceOf[PsiPrimitiveType]

  private def isAllowedSimpleType: Boolean =
    `type`
      .map(_.canonicalText)
      .exists(allowedTypesCanonicalNames.contains)

  private def isAllowedComplexType: Boolean =
    `type`
      .exists {
        case sp: ScParameterizedType =>
          allowedComplexTypesCanonicalNames.contains(sp.designator.canonicalText)
        case _ => false
      }
}

private object CaseClassParameterType {
  private val allowedTypesCanonicalNames = List(
    "_root_.scala.Predef.String",
    "_root_.java.util.UUID",
    "_root_.java.time.DayOfWeek",
    "_root_.java.time.Duration",
    "_root_.java.time.Instant",
    "_root_.java.time.LocalDate",
    "_root_.java.time.LocalDateTime",
    "_root_.java.time.LocalTime",
    "_root_.java.time.Month",
    "_root_.java.time.MonthDay",
    "_root_.java.time.OffsetDateTime",
    "_root_.java.time.OffsetTime",
    "_root_.java.time.Period",
    "_root_.java.time.Year",
    "_root_.java.time.YearMonth",
    "_root_.java.time.ZonedDateTime"
  )

  private val allowedComplexTypesCanonicalNames =
    List("scala.List", "scala.Seq", "_root_.scala.List", "_root_.scala.Seq", "_root_.scala.Option")
}

private class CaseClassParameter(private val scClass: ScClass, private val ccParameterType: CaseClassParameterType) {
  val scClassParameter: ScClassParameter = ccParameterType.scClassParameter

  private val singularParamName = ccParameterType.`type`
    .filter {
      case sp: ScParameterizedType =>
        scClassParameter.getName().endsWith("s") &&
          CaseClassParameter.allowedComplexPluralTypesCanonicalNames.contains(sp.designator.canonicalText)
      case _ => false
    }
    .map(_ => unpluralize(scClassParameter.getName()))
    .getOrElse(scClassParameter.getName())

  val typeAliasName: String = scClass.getName() ++ capitalize(singularParamName)

  def parameterTypeAsString: String =
    ccParameterType.`type`
      .map {
        case sp: ScParameterizedType => sp.typeArguments.head.canonicalText
        case t                       => t.canonicalText
      }
      .getOrElse(scClassParameter.getType().getCanonicalText)

  def makeTypeTag: String = {
    val tagName: String = s"${typeAliasName}Tag"
    s"""  trait $tagName
       |  type $typeAliasName = $parameterTypeAsString @@ $tagName
       |""".stripMargin.stripLineEnd
  }
}

private object CaseClassParameter {
  private val allowedComplexPluralTypesCanonicalNames =
    List("scala.List", "scala.Seq", "_root_.scala.List", "_root_.scala.Seq")
}
