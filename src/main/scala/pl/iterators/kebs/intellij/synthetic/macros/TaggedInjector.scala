package pl.iterators.kebs.intellij.synthetic.macros

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScTypeAlias, ScTypeAliasDefinition}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScObject, ScTemplateDefinition, ScTypeDefinition}
import org.jetbrains.plugins.scala.lang.psi.impl.toplevel.typedef.SyntheticMembersInjector
import org.jetbrains.plugins.scala.lang.psi.types.{ScParameterizedType, ScType}

class TaggedInjector extends SyntheticMembersInjector {

  import TaggedInjector._

  // inject `apply` and `from` functions to object if it is in tagged object/trait and in the containing type there is
  // type alias with tag (also check if there is validate method, because it changes signature of `from` function (to inject))
  override def injectFunctions(source : ScTypeDefinition) : Seq[String] = {
    source match {
      case scObject: ScObject if isTagged(scObject.containingClass) =>
        makeFunctionsForTypeObject(scObject)
      case _ => Nil
    }
  }

  // inject object with `apply` and `from` functions for each type with tag in tagged object or trait
  override def injectInners(source: ScTypeDefinition): Seq[String] = {
    source match {
      case templateDefinition: ScTemplateDefinition if isTagged(templateDefinition) =>
        for {
          typeWithTag <- getTypesWithTag(templateDefinition)
          typeObject  <- makeTypeObject(typeWithTag).toSeq
        } yield typeObject
      case _ => Nil
    }
  }
}

object TaggedInjector {
  private val taggedAnnotation = "pl.iterators.kebs.tag.meta.tagged"

  private def getTypesWithTag(templateDefinition: ScTemplateDefinition) = {
    (for {
      objectTypeName <- findTypeName(templateDefinition)
      taggedType <- findTypeDefByName(templateDefinition.getProject, objectTypeName)
      aliases = taggedType.aliases.filter { alias =>
        templateDefinition.members.collectFirst(
          { case scObject: ScObject if scObject.name == alias.name => scObject }
        ).isEmpty
      }
    } yield aliases).toSeq.flatten
  }

  private def makeTypeObject(typeAlias: ScTypeAlias) =
    for {
      definition <- getTypeAliasDefinition(typeAlias)
      (t, tag)   <- getTypeWithTag(definition)
      tagParams  =  getTypeParamsAsString(tag)
    } yield
      s"""object ${definition.name} {
         |  def apply$tagParams(arg: ${t.toString}): ${definition.name}$tagParams = ???
         |  def from$tagParams(arg: ${t.toString}): ${definition.name}$tagParams = ???
         |}""".stripMargin

  private def makeFunctionsForTypeObject(scObject: ScObject): Seq[String] = {
    (for {
      objectTypeName <- findTypeName(scObject.containingClass)
      taggedType <- findTypeDefByName(scObject.getProject, objectTypeName)
      typeWithTag <- taggedType.aliases.find(_.name == scObject.name)
      typeWithTagDefinition <- getTypeAliasDefinition(typeWithTag)
      (internalType, tag)  <- getTypeWithTag(typeWithTagDefinition)
      tagParams  =  getTypeParamsAsString(tag)
      apply = makeApplyFunction(scObject, tagParams, internalType)
      from = makeFromFunction(scObject, tagParams, internalType)
    } yield List(apply, from)).toSeq.flatten
  }

  private def makeApplyFunction(scObject : ScObject, typeParams: String, internalType: ScType): String = {
    s"def apply$typeParams(arg: ${internalType.toString}): ${scObject.name}$typeParams = ???"
  }

  private def makeFromFunction(scObject : ScObject, typeParams: String, internalType: ScType): String = {
    findValidateFunction(scObject) match {
      case Some(ValidateFunction(errorType)) =>
        s"def from$typeParams(arg: ${internalType.toString}): Either[${errorType.toString()}, ${scObject.name}$typeParams] = ???"
      case None =>
        s"def from$typeParams(arg: ${internalType.toString}): ${scObject.name}$typeParams = ???"
    }
  }

  private case class ValidateFunction(errorType: ScType)

  private def findValidateFunction(scObject : ScObject): Option[ValidateFunction] = {
    scObject.functions.find(_.name == "validate") match {
      case Some(validateFunction) =>
        for {
          scType <- validateFunction.returnType.toOption
          errorType <- leftFromEither(scType)
        } yield ValidateFunction(errorType)
      case None => None
    }
  }

  private def leftFromEither(scType: ScType) = scType match {
    case parameterizedType: ScParameterizedType if parameterizedType.designator.toString == "Either" && parameterizedType.typeArguments.size == 2 =>
      Some(parameterizedType.typeArguments.head)
    case _ => None
  }

  private def getTypeAliasDefinition(typeAlias: ScTypeAlias) =
    typeAlias match {
      case definition: ScTypeAliasDefinition => Some(definition)
      case _ => None
    }

  private def isTagged(templateDefinition: ScTemplateDefinition): Boolean =
    templateDefinition match {
      case typeDefinition: ScTypeDefinition => isTagged(typeDefinition)
      case _ => false
    }

  private def isTagged(typeDefinition: ScTypeDefinition): Boolean =
    typeDefinition.hasAnnotation(taggedAnnotation) && (typeDefinition.isObject ||  typeDefinition.isInterface)

  private def findTypeName(templateDefinition: ScTemplateDefinition): Option[String] =
    for {
      objectClassType <- templateDefinition.`type`().toOption
      objectClass     <- objectClassType.extractClass
      className       =  objectClass.getQualifiedName
    } yield className

  private def findTypeDefByName(project: Project, qualifiedName: String): Option[ScTypeDefinition] =
    JavaPsiFacade.getInstance(project).findClass(qualifiedName, GlobalSearchScope.projectScope(project)) match {
      case typeDef: ScTypeDefinition => Some(typeDef)
      case _                         => None
    }

  private def getTypeWithTag(definition: ScTypeAliasDefinition): Option[(ScType, ScType)] =
    for {
      aliasedType              <- definition.aliasedType.toOption if aliasedType.isInstanceOf[ScParameterizedType]
      aliasedParameterizedType =  aliasedType.asInstanceOf[ScParameterizedType] if isTypeWithTag(aliasedParameterizedType)
      scType                   =  aliasedParameterizedType.typeArguments.head
      scTag                    =  aliasedParameterizedType.typeArguments.tail.head
    } yield (scType, scTag)

  private def isTypeWithTag(scParameterizedType: ScParameterizedType) =
    scParameterizedType.designator.toString == "tagged.@@" && scParameterizedType.typeArguments.size == 2

  private def getTypeParamsAsString(tag: ScType): String =
    tag match {
      case parameterizedType: ScParameterizedType => s"[${parameterizedType.typeArguments.mkString(",")}]"
      case _                                      => ""
    }

}