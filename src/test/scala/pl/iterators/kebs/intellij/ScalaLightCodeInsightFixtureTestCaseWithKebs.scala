package pl.iterators.kebs.intellij

import org.jetbrains.plugins.scala.DependencyManagerBase.RichStr
import org.jetbrains.plugins.scala.ScalaSdkOwner.preferableSdkVersion
import org.jetbrains.plugins.scala.libraryLoaders.{IvyManagedLoader, LibraryLoader}
import org.jetbrains.plugins.scala.{ScalaLightCodeInsightFixtureTestCase, ScalaVersion}

trait ScalaLightCodeInsightFixtureTestCaseWithKebs extends ScalaLightCodeInsightFixtureTestCase {

  private val iteratorsOrg = "pl.iterators"
  private val kebsVersion  = "1.8.1"

  override protected def defaultVersionOverride: Option[ScalaVersion] = Some(preferableSdkVersion)

  override def librariesLoaders: Seq[LibraryLoader] =
    super.librariesLoaders :+
      IvyManagedLoader(iteratorsOrg %% "kebs-tagged"      % kebsVersion) :+
      IvyManagedLoader(iteratorsOrg %% "kebs-tagged-meta" % kebsVersion) :+
      IvyManagedLoader(iteratorsOrg %% "kebs-macro-utils" % kebsVersion)

}
