package org.jetbrains.plugins.scala.libraryLoaders

import com.intellij.openapi.module.Module
import org.jetbrains.plugins.scala.ScalaVersion

/**
  * @author adkozlov
  */
trait LibraryLoader {
  def init(implicit module: Module, version: ScalaVersion): Unit

  def clean(implicit module: Module): Unit = ()
}
