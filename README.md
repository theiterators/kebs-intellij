# Kebs for IntelliJ

A companion IntelliJ IDEA plugin for [kebs](https://github.com/theiterators/kebs) library.

It enhances experience with the library by adding support for code generated by macros from kebs.

## Installation

The plugin is not yet published, you can build it and install from disk.

Build using sbt:
```
$ sbt package
```
Plugin jar is located in `target/scala-2.13/` directory of the project.

See https://www.jetbrains.com/help/idea/plugins-settings.html to check how to install the plugin from disk.

## Development

Open plugin project in IntelliJ as usual. Sbt defines runtime configuration "kebs-intellij" which runs a new instance of IntelliJ with actual plugin code.

## Resources

Plugin code was influenced by [zio-intellij](https://github.com/zio/zio-intellij) project.
