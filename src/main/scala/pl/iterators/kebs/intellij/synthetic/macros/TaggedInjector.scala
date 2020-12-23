package pl.iterators.kebs.intellij.synthetic.macros

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.plugins.scala.extensions.PsiClassExt
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScTypeAlias, ScTypeAliasDefinition}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTypeDefinition
import org.jetbrains.plugins.scala.lang.psi.impl.toplevel.typedef.SyntheticMembersInjector
import org.jetbrains.plugins.scala.lang.psi.types.{ScParameterizedType, ScType}

class TaggedInjector extends SyntheticMembersInjector {

  import TaggedInjector._

  // for each type in object generate companion's object apply and from method
  // FIXME: do not "inject whole companion object when it is defined, but inject apply and from function to existing object
  // FIXME: from has different signature when there is validate function defined in companion object
  override def injectInners(source: ScTypeDefinition): Seq[String] = {
    findTaggedObjectTypeName(source) match {
      case Some(objectTypeName) =>
        findTypeDefByName(source.getProject, objectTypeName)
          .toSeq
          .flatMap(_.aliases)
          .map(makeTypeCompanionObject)
          .flatMap(_.toSeq)
          .map(a => { println(s"DEBUG: $a"); a })
      case _ => Nil
    }
  }

  private def makeTypeCompanionObject(typeAlias: ScTypeAlias) =
    typeAlias match {
      case definition: ScTypeAliasDefinition =>
        for {
          (t, tag)  <- getTypeWithTag(definition)
          tagParams =  getTagTypeParamsAsString(tag)
        } yield
          s"""object ${definition.name} {
             |  def apply$tagParams(_v: ${t.toString}): ${definition.name}$tagParams = ???
             |  def from$tagParams(_v: ${t.toString}): ${definition.name}$tagParams = ???
             |}""".stripMargin
      case _ => None
    }
}

object TaggedInjector {
  private val taggedAnnotation = "pl.iterators.kebs.tag.meta.tagged"

  private def findTaggedObjectTypeName(source: ScTypeDefinition): Option[String] =
    for {
      _               <- Option(source.findAnnotationNoAliases(taggedAnnotation)) if source.isObject
      objectClassType <- source.`type`().toOption
      objectClass     <- objectClassType.extractClass
    } yield objectClass.qualifiedName + "$" //FIXME: find way to get scala qualified object name (without need of adding '$')

  private def findTypeDefByName(project: Project, qualifiedName: String): Option[ScTypeDefinition] =
    JavaPsiFacade.getInstance(project).findClass(qualifiedName, GlobalSearchScope.projectScope(project)) match {
      case typeDef: ScTypeDefinition => Some(typeDef)
      case _                         => None
    }

  private def getTypeWithTag(definition: ScTypeAliasDefinition): Option[(ScType, ScType)] = {
    for {
      aliasedType              <- definition.aliasedType.toOption if aliasedType.isInstanceOf[ScParameterizedType]
      aliasedParameterizedType =  aliasedType.asInstanceOf[ScParameterizedType] if isTaggedType(aliasedParameterizedType)
      scType                   =  aliasedParameterizedType.typeArguments.head
      scTag                    =  aliasedParameterizedType.typeArguments.tail.head
    } yield (scType, scTag)
  }

  private def isTaggedType(scParameterizedType: ScParameterizedType) =
    scParameterizedType.designator.toString == "tagged.@@" && scParameterizedType.typeArguments.size == 2

  private def getTagTypeParamsAsString(tag: ScType): String =
    tag match {
      case parameterizedType: ScParameterizedType => s"[${parameterizedType.typeArguments.mkString(",")}]"
      case _                                      => ""
    }
}