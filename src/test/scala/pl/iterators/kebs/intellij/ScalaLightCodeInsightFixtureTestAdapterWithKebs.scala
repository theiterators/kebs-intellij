package pl.iterators.kebs.intellij

import org.jetbrains.plugins.scala.DependencyDescriptionOps.RichStr
import org.jetbrains.plugins.scala.{IvyManagedLoader, LibraryLoader, ScalaLightCodeInsightFixtureTestAdapter}

trait ScalaLightCodeInsightFixtureTestAdapterWithKebs extends ScalaLightCodeInsightFixtureTestAdapter {

  private val iteratorsOrg = "pl.iterators"
  private val kebsVersion  = "1.8.1"

  override def librariesLoaders: Seq[LibraryLoader] =
    super.librariesLoaders :+
      IvyManagedLoader(iteratorsOrg %% "kebs-tagged"      % kebsVersion) :+
      IvyManagedLoader(iteratorsOrg %% "kebs-tagged-meta" % kebsVersion) :+
      IvyManagedLoader(iteratorsOrg %% "kebs-macro-utils" % kebsVersion)

}
