package pl.iterators.kebs.intellij.action

import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent, CommonDataKeys}
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.text.Strings.{capitalize, unpluralize}
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.{PsiElement, PsiFileFactory, PsiPrimitiveType}
import org.jetbrains.plugins.scala.ScalaLanguage
import org.jetbrains.plugins.scala.lang.psi.api.base.types.ScParameterizedTypeElement
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScClassParameter
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScClass
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory
import org.jetbrains.plugins.scala.lang.psi.types.ScParameterizedType
import org.jetbrains.plugins.scala.project.ProjectContext

class GenerateTaggedObjectAction extends AnAction {

  import GenerateTaggedObjectAction._

  override def update(actionEvent: AnActionEvent): Unit = {
    val currentElement = actionEvent.getData(CommonDataKeys.PSI_ELEMENT)

    actionEvent.getPresentation.setEnabledAndVisible(shouldBeVisible(currentElement))
  }

  override def actionPerformed(actionEvent: AnActionEvent): Unit = {
    val currentElement = actionEvent.getData(CommonDataKeys.PSI_ELEMENT)
    if (shouldBeVisible(currentElement)) {
      val scClass         = currentElement.asInstanceOf[ScClass]
      val ccParamsToTag   = caseClassParametersToTag(scClass)
      val tagsFileContent = generateTaggedObject(scClass, ccParamsToTag)
      // TODO: show message dialog when file already exists (and do not try to write it)
      WriteCommandAction.runWriteCommandAction(scClass.getProject, new Runnable() {
        override def run(): Unit = {
          writeTagsFile(scClass, tagsFileContent)
          changeTypesInCaseClass(scClass, ccParamsToTag)
        }
      })
    }
  }

  private def shouldBeVisible(currentElement: PsiElement): Boolean =
    currentElement != null &&
      currentElement.isInstanceOf[ScClass] &&
      currentElement.asInstanceOf[ScClass].isCase

  private def caseClassParametersToTag(scClass: ScClass): Seq[ScClassParameter] =
    scClass.constructor.toSeq
      .flatMap { primaryConstructor =>
        primaryConstructor.parameters
          .filter { classParameter =>
            val isPrimitive = classParameter
              .getType()
              .isInstanceOf[PsiPrimitiveType]
            lazy val `type` = classParameter
              .`type`()
              .toOption
            def isAllowedSimpleType =
              `type`
                .map(_.canonicalText)
                .exists(allowedTypesCanonicalNames.contains)
            def isAllowedComplexType =
              `type`
                .exists {
                  case sp: ScParameterizedType =>
                    allowedComplexTypesCanonicalNames.contains(sp.designator.canonicalText)
                  case _ => false
                }
            isPrimitive || isAllowedSimpleType || isAllowedComplexType
          }
      }

  private def generateTaggedObject(scClass: ScClass, ccParams: Seq[ScClassParameter]): String = {
    val tags = ccParams
      .map { classParameter =>
        new ClassParameter(scClass, classParameter)
      }
      .map(_.makeTypeTag)
      .mkString("\n\n")

    val packageDeclaration =
      if (scClass.getPath.nonEmpty)
        s"package ${scClass.getPath}\n\n"
      else
        ""

    s"""${packageDeclaration}import pl.iterators.kebs.tagged._
       |import pl.iterators.kebs.tag.meta.tagged
       |
       |@tagged object ${scClass.getName()}Tags {
       |$tags
       |}
       |""".stripMargin
  }

  private def writeTagsFile(scClass: ScClass, tagsFileContent: String): PsiElement =
    scClass.getContainingFile.getContainingDirectory.add {
      val psiFile = PsiFileFactory
        .getInstance(scClass.getProject)
        .createFileFromText(s"${scClass.getName()}Tags.scala", ScalaLanguage.INSTANCE, tagsFileContent)
      JavaCodeStyleManager.getInstance(scClass.getProject).shortenClassReferences(psiFile)
    }

  private def changeTypesInCaseClass(scClass: ScClass, ccParamsToTag: Seq[ScClassParameter]): PsiElement = {
    ccParamsToTag
      .map { scClassParameter =>
        (scClassParameter, new ClassParameter(scClass, scClassParameter))
      }
      .foreach {
        case (scClassParameter, classParameter) =>
          scClassParameter.typeElement match {
            case Some(sp: ScParameterizedTypeElement) =>
              sp.typeArgList.typeArgs.head.replace(
                ScalaPsiElementFactory.createTypeElementFromText(
                  s"${scClass.getName}Tags.${classParameter.typeAliasName}"
                )(ProjectContext.fromPsi(scClass))
              )
            case Some(t) =>
              t.replace(
                ScalaPsiElementFactory.createTypeElementFromText(
                  s"${scClass.getName}Tags.${classParameter.typeAliasName}"
                )(ProjectContext.fromPsi(scClass))
              )
            case _ => ()
          }
      }
    JavaCodeStyleManager.getInstance(scClass.getProject).shortenClassReferences(scClass)
  }
}

object GenerateTaggedObjectAction {
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

private class ClassParameter(private val scClass: ScClass, private val scClassParameter: ScClassParameter) {
  private val `type`    = scClassParameter.`type`().toOption
  private val className = scClass.getName()
  private val singularParamName = `type`
    .filter {
      case sp: ScParameterizedType =>
        scClassParameter.getName().endsWith("s") &&
          ClassParameter.allowedComplexPluralTypesCanonicalNames.contains(sp.designator.canonicalText)
      case _ => false
    }
    .map(_ => unpluralize(scClassParameter.getName()))
    .getOrElse(scClassParameter.getName())

  val typeAliasName: String = className ++ capitalize(singularParamName)

  def parameterTypeAsString: String =
    `type`
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

object ClassParameter {
  private val allowedComplexPluralTypesCanonicalNames =
    List("scala.List", "scala.Seq", "_root_.scala.List", "_root_.scala.Seq")
}
