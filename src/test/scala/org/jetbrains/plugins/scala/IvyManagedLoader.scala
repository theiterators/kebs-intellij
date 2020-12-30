package org.jetbrains.plugins.scala

import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.PsiTestUtil
import org.jetbrains.plugins.scala.DependencyManagerBase.DependencyDescription

import scala.annotation.nowarn

case class IvyManagedLoader(dependencies: DependencyDescription*) extends LibraryLoader {
  protected lazy val dependencyManager: DependencyManagerBase = DependencyManager

  override def init(implicit module: Module, version: ScalaVersion): Unit =
    dependencyManager.resolve(dependencies: _*).foreach { resolved =>
      VfsRootAccess.allowRootAccess(resolved.file.getCanonicalPath): @nowarn("cat=deprecation")
      PsiTestUtil.addLibrary(module, resolved.info.toString, resolved.file.getParent, resolved.file.getName)
    }
}
