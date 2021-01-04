package pl.iterators.kebs.intellij.synthetic.macros

import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScObject, ScTemplateDefinition, ScTypeDefinition}
import org.jetbrains.plugins.scala.lang.psi.impl.toplevel.typedef.SyntheticMembersInjector

class TaggedInjector extends SyntheticMembersInjector {

  import TaggedInjector._

  // inject `apply` and `from` functions to object if it is in tagged object/trait and in the containing type there is
  // type alias with tag (also check if there is validate method, because it changes signature of `from` function (to inject))
  override def injectFunctions(source : ScTypeDefinition) : Seq[String] = {
    source match {
      case scObject: ScObject if isTagged(scObject.containingClass) =>
        (for {
          typeWithTag <- TypeWithTag.fromTypeObject(scObject)
          functions   =  typeWithTag.makeFunctionsForTypeObject(scObject)
        } yield functions).toSeq.flatten
     case _ => Nil
    }
  }

  // inject object with `apply` and `from` functions for each type with tag in tagged object or trait
  override def injectInners(source: ScTypeDefinition): Seq[String] = {
    source match {
      case templateDefinition: ScTemplateDefinition if isTagged(templateDefinition) =>
        for {
          typeWithTag <- TypeWithTag.fromTypeWithNoTypeObject(templateDefinition)
          typeObject  =  typeWithTag.makeTypeObject
        } yield typeObject
      case _ => Nil
    }
  }
}

object TaggedInjector {
  private val taggedAnnotation = "pl.iterators.kebs.tag.meta.tagged"

  private def isTagged(templateDefinition: ScTemplateDefinition): Boolean =
    templateDefinition match {
      case typeDefinition: ScTypeDefinition => isTagged(typeDefinition)
      case _ => false
    }

  private def isTagged(typeDefinition: ScTypeDefinition): Boolean =
    typeDefinition.hasAnnotation(taggedAnnotation) && (typeDefinition.isObject ||  typeDefinition.isInterface)

}