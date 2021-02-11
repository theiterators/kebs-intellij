ThisBuild / scalaVersion := "2.13.4"
ThisBuild / version := "0.0.3"
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
        This is the first version of the plugin. Currently supported features:
        <ul>
          <li>
            <code>@tagged</code> support - add hints in IntelliJ IDEA for the tagged types generated by kebs-tagged-meta
            for objects and traits tagged with <a href="https://github.com/theiterators/kebs#tagged-types"><code>@tagged</code></a>
            annotation
          </li>
        </ul>
      ]]>"""
    }
  )

lazy val runner = createRunnerProject(`kebs-intellij`)

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
