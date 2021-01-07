package pl.iterators.kebs.intellij.synthetic.macros

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScTypeAlias, ScTypeAliasDefinition}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScObject, ScTemplateDefinition, ScTypeDefinition}
import org.jetbrains.plugins.scala.lang.psi.types.{ScParameterizedType, ScType}

class TypeWithTag(
  private val name: String,
  private val internalType: ScType,
  private val tagType: ScType
) {

  private val tagTypeParamsAsString: String =
    tagType match {
      case parameterizedType: ScParameterizedType => s"[${parameterizedType.typeArguments.mkString(",")}]"
      case _                                      => ""
    }

  private val internalTypeAsString = internalType.canonicalText

  def makeTypeObject: String =
    s"""object $name {
       |  $makeApplyFunction
       |  $makeFromFunction
       |}""".stripMargin

  def makeFunctionsForTypeObject(scObject: ScObject): Seq[String] =
    List(makeApplyFunction, makeFromFunction(scObject))

  private def makeApplyFunction: String =
    s"def apply$tagTypeParamsAsString(arg: $internalTypeAsString): $name$tagTypeParamsAsString = ???"

  private def makeFromFunction: String =
    s"def from$tagTypeParamsAsString(arg: $internalTypeAsString): $name$tagTypeParamsAsString = ???"

  private def makeFromFunction(scObject : ScObject): String =
    ValidateFunction.find(scObject) match {
      case Some(ValidateFunction(errorType)) =>
        s"def from$tagTypeParamsAsString(arg: $internalTypeAsString): Either[${errorType.toString}, $name$tagTypeParamsAsString] = ???"
      case None =>
        makeFromFunction
    }
}

object TypeWithTag {

  def fromTypeObject(scObject: ScObject): Option[TypeWithTag] =
    for {
      taggedType  <- getTypeDefinition(scObject.containingClass)
      typeAlias   <- taggedType.aliases.find(_.name == scObject.name)
      typeWithTag <- fromTypeAlias(typeAlias)
    } yield typeWithTag

  def fromTypeWithNoTypeObject(templateDefinition: ScTemplateDefinition): Seq[TypeWithTag] =
    (for {
      taggedType    <- getTypeDefinition(templateDefinition)
      typesWithTags =  fromTypeAliases(templateDefinition, taggedType.aliases)
    } yield typesWithTags).toSeq.flatten

  private def fromTypeAliases(templateDefinition: ScTemplateDefinition, aliases: Seq[ScTypeAlias]): Seq[TypeWithTag] =
    for {
      alias <- aliases.filter { alias =>
        templateDefinition.members.collectFirst(
          { case scObject: ScObject if scObject.name == alias.name => scObject }
        ).isEmpty
      }
      typesWithTags <- fromTypeAlias(alias).toSeq
    } yield typesWithTags

  private def fromTypeAlias(typeAlias: ScTypeAlias): Option[TypeWithTag] =
    for {
      definition              <- getTypeAliasDefinition(typeAlias)
      (internalType, tagType) <- getTypeWithTag(definition)
    } yield new TypeWithTag(typeAlias.name, internalType, tagType)

  private def getTypeDefinition(templateDefinition: ScTemplateDefinition): Option[ScTypeDefinition] =
    for {
      typeName <- getTypeName(templateDefinition)
      typeDefinition <- getTypeDefinition(templateDefinition.getProject, typeName)
    } yield typeDefinition

  private def getTypeDefinition(project: Project, qualifiedName: String): Option[ScTypeDefinition] =
    JavaPsiFacade.getInstance(project).findClass(qualifiedName, GlobalSearchScope.projectScope(project)) match {
      case typeDef: ScTypeDefinition => Some(typeDef)
      case _                         => None
    }

  private def getTypeName(templateDefinition: ScTemplateDefinition): Option[String] =
    for {
      objectClassType <- templateDefinition.`type`().toOption
      objectClass     <- objectClassType.extractClass
      className       <- Option(objectClass.getQualifiedName)
    } yield className

  private def getTypeAliasDefinition(typeAlias: ScTypeAlias): Option[ScTypeAliasDefinition] =
    typeAlias match {
      case definition: ScTypeAliasDefinition => Some(definition)
      case _ => None
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

}
