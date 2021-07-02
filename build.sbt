ThisBuild / scalaVersion := "2.13.4"
ThisBuild / organization := "pl.iterators"
ThisBuild / organizationName := "Iterators"
ThisBuild / organizationHomepage := Some(url("https://iteratorshq.com/"))
ThisBuild / intellijPluginName := "kebs-intellij"
// List of release versions: https://www.jetbrains.com/intellij-repository/releases
// List of snapshot versions: https://www.jetbrains.com/intellij-repository/snapshots
ThisBuild / intellijBuild := "211.7628.21"

lazy val `kebs-intellij` = project
  .in(file("."))
  .enablePlugins(SbtIdeaPlugin)
  .settings(
    intellijPlugins := Seq(
      "com.intellij.java".toPlugin, // this is not required in the runtime, although it is required by JetBrains, see https://blog.jetbrains.com/platform/2019/06/java-functionality-extracted-as-a-plugin/
      "org.intellij.scala".toPlugin
    ),
    libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % Test,
    scalacOptions += "-deprecation",
    testOptions += Tests.Argument(TestFrameworks.JUnit, "-v"),
    patchPluginXml := pluginXmlOptions { xml =>
      xml.version = version.value
      xml.changeNotes = ChangeNotes.value
    }
  )

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
