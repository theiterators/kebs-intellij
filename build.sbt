ThisBuild / scalaVersion := "2.13.4"
ThisBuild / version := "0.0.5-SNAPSHOT"
ThisBuild / organization := "pl.iterators"
ThisBuild / organizationName := "Iterators"
ThisBuild / organizationHomepage := Some(url("https://iteratorshq.com/"))
ThisBuild / intellijPluginName := "kebs-intellij"
ThisBuild / intellijBuild := "203"

ThisBuild / scalacOptions += "-deprecation"

lazy val `kebs-intellij` = project
  .in(file("."))
  .enablePlugins(SbtIdeaPlugin)
  .settings(
    intellijPlugins := Seq(
      "com.intellij.java".toPlugin, // this is not required in the runtime, although it is required by JetBrains, see https://blog.jetbrains.com/platform/2019/06/java-functionality-extracted-as-a-plugin/
      "org.intellij.scala".toPlugin
    ),
    libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % Test,
    patchPluginXml := pluginXmlOptions { xml =>
      xml.version     = version.value
      xml.changeNotes = """<![CDATA[
        TBD
        <ul>
          <li>TBD</li>
        </ul>
      ]]>"""
    }
  )

lazy val runner = createRunnerProject(`kebs-intellij`)

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
