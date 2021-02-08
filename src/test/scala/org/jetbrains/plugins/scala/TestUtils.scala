/*
 * Copyright 2000-2008 JetBrains s.r.o.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.plugins.scala

import com.intellij.openapi.diagnostic.Logger

import java.io.{File, IOException}
import java.net.{URISyntaxException, URL}

object TestUtils {
  private lazy val log = Logger.getInstance("org.jetbrains.plugins.scala.util.TestUtils")

  lazy val testDataPath: String =
    try {
      val resource = this.getClass.getClassLoader.getResource("testdata")
      if (resource == null) {
        val f = find("scala/scala-impl", "testdata")
        f.getAbsolutePath
      } else new File(resource.toURI).getPath.replace(File.separatorChar, '/')
    } catch {
      case e @ (_: URISyntaxException | _: IOException) =>
        log.error(e)
        // just rethrowing here because that's a clearer way to make tests fail than some NPE somewhere else
        throw new RuntimeException(e)
    }

  @throws[IOException]
  private def findTestDataDir(pathname: String): String = findTestDataDir(new File(pathname), "testdata")

  @throws[IOException]
  private def find(pathname: String, child: String): File = {
    val file = new File("community/" + pathname, child)
    if (file.exists) file
    else new File(findTestDataDir(pathname))
  }

  /** Go upwards to find testdata, because when running test from IDEA, the launching dir might be some subdirectory. */
  @throws[IOException]
  private def findTestDataDir(parent: File, child: String): String = {
    val testData = new File(parent, child).getCanonicalFile
    if (testData.exists) testData.getCanonicalPath
    else {
      val newParent = parent.getParentFile
      if (newParent == null) throw new RuntimeException("no testdata directory found")
      else findTestDataDir(newParent, child)
    }
  }

}
