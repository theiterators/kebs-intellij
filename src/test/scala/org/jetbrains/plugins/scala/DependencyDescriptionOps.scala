package org.jetbrains.plugins.scala

import org.jetbrains.plugins.scala.DependencyManagerBase.DependencyDescription

object DependencyDescriptionOps {
  implicit class RichStr(private val org: String) extends AnyVal {

    def %(artId: String) = DependencyDescription(org, artId, "")

    def %%(artId: String)(implicit scalaVersion: ScalaVersion) =
      DependencyDescription(org, artId + "_" + scalaVersion.major, "")
  }
}
