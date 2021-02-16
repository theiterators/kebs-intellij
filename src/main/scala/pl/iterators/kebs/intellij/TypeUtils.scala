package pl.iterators.kebs.intellij

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScTypeAlias, ScTypeAliasDefinition}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScTemplateDefinition, ScTypeDefinition}
import org.jetbrains.plugins.scala.lang.psi.types.{ScParameterizedType, ScType}

object TypeUtils {

  def getTypeDefinition(templateDefinition: ScTemplateDefinition): Option[ScTypeDefinition] =
    for {
      typeName       <- getTypeName(templateDefinition)
      typeDefinition <- getTypeDefinition(templateDefinition.getProject, typeName)
    } yield typeDefinition

  def getTypeDefinition(project: Project, qualifiedName: String): Option[ScTypeDefinition] =
    JavaPsiFacade.getInstance(project).findClass(qualifiedName, GlobalSearchScope.projectScope(project)) match {
      case typeDef: ScTypeDefinition => Some(typeDef)
      case _                         => None
    }

  def getTypeName(templateDefinition: ScTemplateDefinition): Option[String] =
    for {
      objectClassType <- templateDefinition.`type`().toOption
      objectClass     <- objectClassType.extractClass
      className       <- Option(objectClass.getQualifiedName)
    } yield className

  def getTypeAliasDefinition(typeAlias: ScTypeAlias): Option[ScTypeAliasDefinition] =
    typeAlias match {
      case definition: ScTypeAliasDefinition => Some(definition)
      case _                                 => None
    }

  def getParameterizedType(scType: ScType): Option[ScParameterizedType] =
    scType match {
      case parameterizedType: ScParameterizedType => Some(parameterizedType)
      case _                                      => None
    }
}
