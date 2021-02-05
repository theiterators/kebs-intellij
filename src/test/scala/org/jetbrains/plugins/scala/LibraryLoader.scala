package org.jetbrains.plugins.scala

import com.intellij.openapi.module.Module

/**
  * @author adkozlov
  */
trait LibraryLoader {
  def init(implicit module: Module, version: ScalaVersion): Unit

  def clean(implicit module: Module): Unit = ()
}
