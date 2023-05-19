package org.jetbrains.plugins.scala

import com.intellij.openapi.module.{Module, ModuleManager}
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightProjectDescriptor

class ScalaLightProjectDescriptor(
  private val sharedProjectToken: SharedTestProjectToken = SharedTestProjectToken.DoNotShare)
    extends LightProjectDescriptor {

  override def setUpProject(project: Project, handler: LightProjectDescriptor.SetupHandler): Unit = {
    super.setUpProject(project, handler)
    val modules = ModuleManager.getInstance(project).getModules
    tuneModule(modules.head, project)
  }

  /** We also pass project because `getProject` in test classes might still be not-initialized (null) */
  def tuneModule(module: Module, project: Project): Unit = ()

  /** see [[com.intellij.testFramework.LightPlatformTestCase.doSetup]] */
  override def equals(obj: Any): Boolean =
    obj match {
      case other: ScalaLightProjectDescriptor =>
        val equals = for {
          id1 <- this.sharedProjectToken.value
          id2 <- other.sharedProjectToken.value
        } yield id1 == id2
        equals.getOrElse(false)
      case _ =>
        super.equals(obj)
    }
}

/**
  * Test cases with the same Some(token) will share test project if run one by-one.<br>
  * This can make each test case initialization significantly faster.<br>
  * If you do not want the project to be shared the token should be None or some other unique value.
  *
  * @note Suppose test classes A and B use token T1, and test C uses token T2.<br>
  *       If test are run in following order: A, C, B, then project will not be reused between A and B.
  *       (This is because under the hood IntelliJ platform uses a singleton for storing current test project)
  */
case class SharedTestProjectToken(value: Option[AnyRef])

object SharedTestProjectToken {
  def apply(value: AnyRef): SharedTestProjectToken =
    new SharedTestProjectToken(Some(value))

  val DoNotShare: SharedTestProjectToken =
    SharedTestProjectToken(None)

  def ByScalaSdkAndProjectLibraries(test: LibrariesOwner with ScalaSdkOwner): SharedTestProjectToken =
    SharedTestProjectToken((test.version, test.librariesLoadersPublic))

  def ByTestClassAndScalaSdkAndProjectLibraries(test: LibrariesOwner with ScalaSdkOwner): SharedTestProjectToken =
    SharedTestProjectToken((test.getClass, test.version, test.librariesLoadersPublic))
}
