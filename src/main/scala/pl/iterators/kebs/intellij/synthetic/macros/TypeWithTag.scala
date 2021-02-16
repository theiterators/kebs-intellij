package pl.iterators.kebs.intellij.synthetic.macros

import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScTypeAlias, ScTypeAliasDefinition}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScObject, ScTemplateDefinition}
import org.jetbrains.plugins.scala.lang.psi.types.{ScParameterizedType, ScType}
import pl.iterators.kebs.intellij.TypeUtils._
import pl.iterators.kebs.intellij.kebsPackage

class TypeWithTag(
  private val name: String,
  private val internalType: ScType,
  private val tagType: ScType) {

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

  private def makeFromFunction(scObject: ScObject): String =
    ValidateFunction.find(scObject) match {
      case Some(ValidateFunction(errorType)) =>
        s"def from$tagTypeParamsAsString(arg: $internalTypeAsString): Either[${errorType.toString}, $name$tagTypeParamsAsString] = ???"
      case _ =>
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
      taggedType <- getTypeDefinition(templateDefinition)
      typesWithTags = fromTypeAliasesWithNoTypeObject(templateDefinition, taggedType.aliases)
    } yield typesWithTags).toSeq.flatten

  private def fromTypeAliasesWithNoTypeObject(
    templateDefinition: ScTemplateDefinition,
    aliases: Seq[ScTypeAlias]
  ): Seq[TypeWithTag] =
    for {
      alias         <- aliases.filter(withNoTypeObject(templateDefinition))
      typesWithTags <- fromTypeAlias(alias).toSeq
    } yield typesWithTags

  private def withNoTypeObject(templateDefinition: ScTemplateDefinition)(alias: ScTypeAlias): Boolean =
    templateDefinition.members
      .collectFirst({ case scObject: ScObject if scObject.name == alias.name => scObject })
      .isEmpty

  private def fromTypeAlias(typeAlias: ScTypeAlias): Option[TypeWithTag] =
    for {
      definition  <- getTypeAliasDefinition(typeAlias)
      typeWithTag <- fromTypeAliasDefinition(definition)
    } yield typeWithTag

  private def fromTypeAliasDefinition(definition: ScTypeAliasDefinition): Option[TypeWithTag] =
    for {
      aliasedType       <- definition.aliasedType.toOption
      parameterizedType <- getParameterizedType(aliasedType) if isTypeWithTag(parameterizedType)
      scType = parameterizedType.typeArguments.head
      scTag  = parameterizedType.typeArguments.tail.head
    } yield new TypeWithTag(definition.getName(), scType, scTag)

  private val kebsTaggedAlias             = "@@"
  private val kebsTaggedPackageObjectName = s"$kebsPackage.tagged.package$$"

  // is it "SomeType @@ Tag"
  private def isTypeWithTag(scParameterizedType: ScParameterizedType): Boolean =
    (for {
      aliasType       <- scParameterizedType.designator.aliasType
      definition      <- getTypeAliasDefinition(aliasType.ta) if definition.name == kebsTaggedAlias
      containingType  <- definition.containingClass.`type`().toOption
      containingClass <- containingType.extractClass
      containingClassName = containingClass.getQualifiedName
    } yield containingClassName).contains(kebsTaggedPackageObjectName)
}
