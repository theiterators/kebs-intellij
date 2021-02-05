ThisBuild / scalaVersion := "2.13.4"
ThisBuild / version := "0.0.1-SNAPSHOT"
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
    intellijPlugins := Seq("org.intellij.scala".toPlugin),
    libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % Test,
    patchPluginXml := pluginXmlOptions { xml =>
      xml.version     = version.value
      xml.changeNotes = """<![CDATA[
        This is the first version of the plugin.
        <ul>
          <li>
            @tagged support - add hints in Intellij Idea for the tagged types generated by kebs-tagged-meta
            for objects and traits tagged with @tagged annotation
          </li>
        </ul>
      ]]>"""
    }
  )

lazy val runner = createRunnerProject(`kebs-intellij`)

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
