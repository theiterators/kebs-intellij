package org.jetbrains.plugins.scala

import com.intellij.application.options.CodeStyle
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.folding.CodeFoldingManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.{CodeStyleSettings, CommonCodeStyleSettings}
import com.intellij.testFramework.fixtures.{JavaCodeInsightTestFixture, LightJavaCodeInsightFixtureTestCase}
import com.intellij.testFramework.{EditorTestUtil, LightProjectDescriptor}
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.plugins.scala.extensions.StringExt
import org.jetbrains.plugins.scala.lang.formatting.settings.ScalaCodeStyleSettings
import org.jetbrains.plugins.scala.libraryLoaders.{LibraryLoader, ScalaSDKLoader, SmartJDKLoader, SourcesLoader}
import org.junit.Assert.assertNotNull

import scala.jdk.CollectionConverters._

abstract class ScalaLightCodeInsightFixtureTestCase
    extends LightJavaCodeInsightFixtureTestCase
    with ScalaSdkOwner
    with FailableTest {

  //common useful constants
  protected val CARET = EditorTestUtil.CARET_TAG
  protected val START = EditorTestUtil.SELECTION_START_TAG
  protected val END   = EditorTestUtil.SELECTION_END_TAG

  override def getTestDataPath: String = TestUtils.getTestDataPath + "/"

  protected def sourceRootPath: String = null

  //start section: project libraries configuration
  protected def loadScalaLibrary: Boolean = true

  protected def includeReflectLibrary: Boolean = false

  protected def includeCompilerAsLibrary: Boolean = false

  protected def additionalLibraries: Seq[LibraryLoader] = Seq.empty

  override protected def librariesLoaders: Seq[LibraryLoader] = {
    val scalaSdkLoader = ScalaSDKLoader(includeReflectLibrary, includeCompilerAsLibrary)
    //note: do we indeed need to register it as libraries?
    // shouldn't source roots be registered just as source roots?
    val sourceLoaders     = Option(sourceRootPath).map(SourcesLoader).toSeq
    val additionalLoaders = additionalLibraries
    scalaSdkLoader +: sourceLoaders :++ additionalLoaders
  }
  //end section: project libraries configuration

  //start section: project descriptor
  protected def sharedProjectToken: SharedTestProjectToken = SharedTestProjectToken.DoNotShare

  override protected def getProjectDescriptor: LightProjectDescriptor =
    new ScalaLightProjectDescriptor(sharedProjectToken) {
      override def tuneModule(module: Module, project: Project): Unit =
        afterSetUpProject(project, module)

      override def getSdk: Sdk = SmartJDKLoader.getOrCreateJDK()

      override def getSourceRootType: JavaSourceRootType =
        if (placeSourceFilesInTestContentRoot)
          JavaSourceRootType.TEST_SOURCE
        else
          JavaSourceRootType.SOURCE
    }

  protected def placeSourceFilesInTestContentRoot: Boolean = false

  protected def afterSetUpProject(project: Project, module: Module): Unit = {
    Registry.get("ast.loading.filter").setValue(true, getTestRootDisposable)

    setUpLibraries(module)
  }

  override def setUpLibraries(implicit module: Module): Unit =
    if (loadScalaLibrary) {
      myFixture.allowTreeAccessForAllFiles()
      super.setUpLibraries(module)
    }
  //end section: project descriptor

  override protected def setUp(): Unit = {
    TestUtils.optimizeSearchingForIndexableFiles()
    super.setUp()
    TestUtils.disableTimerThread()
  }

  override protected def tearDown(): Unit = {
    disposeLibraries(getModule)
    super.tearDown()
  }

  //start section: helper methods
  protected def configureFromFileText(fileText: String): PsiFile =
    configureFromFileText(ScalaFileType.INSTANCE, fileText)

  protected def configureFromFileText(fileType: FileType, fileText: String): PsiFile = {
    val file = myFixture.configureByText(fileType, fileText.stripMargin.withNormalizedSeparator.trim)
    assertNotNull(file)
    file
  }

  protected def configureFromFileTextWithSomeName(fileType: String, fileText: String): PsiFile = {
    val file = myFixture.configureByText("Test." + fileType, fileText.withNormalizedSeparator)
    assertNotNull(file)
    file
  }

  protected def configureFromFileText(fileName: String, fileText: String): PsiFile = {
    val file = myFixture.configureByText(fileName: String, fileText.withNormalizedSeparator)
    assertNotNull(file)
    file
  }

  protected def openEditorAtOffset(startOffset: Int): Editor = {
    import com.intellij.openapi.fileEditor.{FileEditorManager, OpenFileDescriptor}
    val project       = getProject
    val editorManager = FileEditorManager.getInstance(project)
    val vFile         = getFile.getVirtualFile
    val editor        = editorManager.openTextEditor(new OpenFileDescriptor(project, vFile, startOffset), false)
    editor
  }
  //end section: helper methods

  //start section: check errors
  protected def checkTextHasNoErrors(text: String): Unit = {
    myFixture.configureByText(ScalaFileType.INSTANCE, text)

    CodeFoldingManager.getInstance(getProject).buildInitialFoldings(getEditor)

    def doTestHighlighting(virtualFile: VirtualFile): Unit =
      myFixture.testHighlighting(false, false, false, virtualFile)

    if (shouldPass) {
      doTestHighlighting(getFile.getVirtualFile)
    } else {
      try {
        doTestHighlighting(getFile.getVirtualFile)
      } catch {
        case _: AssertionError =>
          return
      }
      throw new RuntimeException(failingPassed)
    }
  }

  protected def checkHasErrorAroundCaret(text: String): Unit = {
    val normalizedText = text.withNormalizedSeparator
    myFixture.configureByText("dummy.scala", normalizedText)
    val caretIndex = normalizedText.indexOf(CARET)

    def isAroundCaret(info: HighlightInfo) =
      caretIndex == -1 || new TextRange(info.getStartOffset, info.getEndOffset).contains(caretIndex)

    val infos = myFixture.doHighlighting().asScala

    val warnings = infos.filter(i => StringUtil.isNotEmpty(i.getDescription) && isAroundCaret(i))

    if (shouldPass) {
      assert(warnings.nonEmpty, "No highlightings found")
    } else if (warnings.nonEmpty) {
      throw new RuntimeException(failingPassed)
    }
  }
  //end section: check errors

  //code style settings
  private def getCurrentCodeStyleSettings: CodeStyleSettings =
    CodeStyle.getSettings(getProject)

  protected def getCommonCodeStyleSettings: CommonCodeStyleSettings =
    getCurrentCodeStyleSettings.getCommonSettings(ScalaLanguage.INSTANCE)

  protected def getScalaCodeStyleSettings: ScalaCodeStyleSettings =
    getCurrentCodeStyleSettings.getCustomSettings(classOf[ScalaCodeStyleSettings])

  //start section: workaround methods
  //Workarounds to make the method callable from traits (using cake pattern)
  //Also needed to workaround https://github.com/scala/bug/issues/3564
  override protected def getProject: Project = super.getProject

  //don't use getFixture, use `myFixture` directly
  protected def getFixture: JavaCodeInsightTestFixture = myFixture
  //end section: workaround methods
}
