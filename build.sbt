ThisBuild / scalaVersion         := "2.13.4"
ThisBuild / version              := "0.0.1-SNAPSHOT"
ThisBuild / organization         := "pl.iterators"
ThisBuild / organizationName     := "Iterators"
ThisBuild / organizationHomepage := Some(url("https://iteratorshq.com/"))
ThisBuild / intellijPluginName   := "kebs-intellij"
ThisBuild / intellijBuild        := "203"

lazy val `kebs-intellij` = project
  .in(file("."))
  .enablePlugins(SbtIdeaPlugin)
  .settings(
    intellijPlugins := Seq(
      "org.intellij.scala".toPlugin
    ),
    libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % Test,
    patchPluginXml := pluginXmlOptions { xml =>
      xml.version = version.value
    }
  )

lazy val runner = createRunnerProject(`kebs-intellij`)