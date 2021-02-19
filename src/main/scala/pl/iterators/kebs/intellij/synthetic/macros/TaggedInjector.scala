package pl.iterators.kebs.intellij.synthetic.macros

import com.intellij.psi.PsiClass
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{
  ScObject,
  ScTemplateDefinition,
  ScTrait,
  ScTypeDefinition
}
import org.jetbrains.plugins.scala.lang.psi.impl.toplevel.typedef.SyntheticMembersInjector
import pl.iterators.kebs.intellij.TypeUtils.getTypeDefinition
import pl.iterators.kebs.intellij.kebsPackage

class TaggedInjector extends SyntheticMembersInjector {

  import TaggedInjector._

  // we need companion object for every tag, since macro generates implicit CaseClass1Rep there
  // we cannot inject whole object in injectInners since:
  // * it can be already defined by the user
  // * https://youtrack.jetbrains.com/issue/SCL-18708
  override def needsCompanionObject(source: ScTypeDefinition): Boolean =
    source match {
      case scTrait: ScTrait if hasTaggedAnnotation(scTrait.containingClass) && scTrait.members.isEmpty =>
        true
      case _ => false
    }

  // inject implicit CaseClass1Rep into tags companion objects
  override def injectMembers(source: ScTypeDefinition): Seq[String] =
    source match {
      case TagObject(typesWithTags) => typesWithTags.map(_.makeCaseClass1RepTagImplicit)
      case _                        => Nil
    }

  // inject `apply` and `from` functions to object if it is in tagged object/trait and in the containing type there is
  // type alias with tag (also check if there is validate method, because it changes signature of `from` function (to inject))
  override def injectFunctions(source: ScTypeDefinition): Seq[String] =
    source match {
      case TaggedTypeObject(scObject, typeWithTag) => typeWithTag.makeFunctionsForTypeObject(scObject)
      case _                                       => Nil
    }

  // inject object with `apply` and `from` functions for each type with tag in tagged object or trait
  override def injectInners(source: ScTypeDefinition): Seq[String] =
    source match {
      case TaggedContainer(typesWithTags) => typesWithTags.map(_.makeTypeObject)
      case _                              => Nil
    }
}

object TaggedInjector {

  private val taggedAnnotation = s"$kebsPackage.tag.meta.tagged"

  private object TagObject {
    def unapply(source: ScTypeDefinition): Option[Seq[TypeWithTag]] =
      source match {
        case tagCompanionObject: ScObject if hasTaggedAnnotation(tagCompanionObject.containingClass) =>
          val tagClass             = tagCompanionObject.fakeCompanionClassOrCompanionClass
          val taggedContainerClass = tagCompanionObject.containingClass
          for {
            taggedContainer <- getTypeDefinition(taggedContainerClass)
            if isTagAnEmptyTrait(taggedContainer, tagClass)
          } yield makeTypesWithTagSeq(taggedContainer, tagClass)
        case _ => None
      }
    private def isTagAnEmptyTrait(taggedContainer: ScTypeDefinition, tagClass: PsiClass) =
      taggedContainer.typeDefinitions
        .filter(t => t.isInstanceOf[ScTrait] && t.getSourceMirrorClass == tagClass)
        .map(_.asInstanceOf[ScTrait])
        .exists(_.members.isEmpty)
    private def makeTypesWithTagSeq(taggedContainer: ScTypeDefinition, tagClass: PsiClass) =
      taggedContainer.aliases
        .flatMap(alias => TypeWithTag.fromTypeAlias(alias).toList)
        .filter(typeWithTag => typeWithTag.tagType.extractClass.contains(tagClass))
  }

  private object TaggedTypeObject {
    def unapply(source: ScTypeDefinition): Option[(ScObject, TypeWithTag)] =
      source match {
        case scObject: ScObject if hasTaggedAnnotation(scObject.containingClass) =>
          TypeWithTag.fromTypeObject(scObject).map(typeWithTag => (scObject, typeWithTag))
        case _ => None
      }
  }

  private object TaggedContainer {
    def unapply(source: ScTypeDefinition): Option[Seq[TypeWithTag]] =
      source match {
        case templateDefinition: ScTemplateDefinition if hasTaggedAnnotation(templateDefinition) =>
          Some(TypeWithTag.collectAllWithNoTypeObject(templateDefinition))
        case _ => None
      }
  }

  private def hasTaggedAnnotation(templateDefinition: ScTemplateDefinition): Boolean =
    templateDefinition match {
      case typeDefinition: ScTypeDefinition => hasTaggedAnnotation(typeDefinition)
      case _                                => false
    }

  private def hasTaggedAnnotation(typeDefinition: ScTypeDefinition): Boolean =
    typeDefinition.hasAnnotation(taggedAnnotation) && (typeDefinition.isObject || typeDefinition.isInterface)

}
