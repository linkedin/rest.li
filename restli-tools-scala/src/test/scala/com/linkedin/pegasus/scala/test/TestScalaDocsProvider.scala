/*
   Copyright (c) 2014 LinkedIn Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.linkedin.pegasus.scala.test

import com.linkedin.restli.tools.scala.ScalaDocsProvider
import com.linkedin.restli.examples.greetings.server.ScalaGreetingsResource
import org.testng.annotations.Test
import org.testng.Assert
import java.util.{Collection=>JavaCollection, Collections=>JavaCollections}

class TestScalaDocsProvider {

  @Test
  def testSampleGreetingsResource {
    val projectDir = System.getProperty("test.projectDir");
    val files = JavaCollections.singleton(projectDir + "/../restli-int-test-server/src/main/scala/com/linkedin/restli/examples/greetings/server/ScalaGreetingsResource.scala")

    Assert.assertEquals("version 2.10.6", util.Properties.versionString)
    val provider = new ScalaDocsProvider(null)
    provider.registerSourceFiles(files)

    val method = classOf[ScalaGreetingsResource].getMethod("get", classOf[java.lang.Long])

    // behavior appears to have regressed in 2.10.  The code in the below <pre> tag no longer is tabbed properly like it was in 2.9.  Lame.
    compareDocString("""<p>A scala rest.li service.</p>
                       |<p>Let's test some scaladoc.  First the wiki formats.</p>
                       |<p>Styles: <b>bold</b>, <i>italic</i>, <code>monospace</code>, <em>underline</em>, <sup>superscript</sup>, <sub>subscript</sub></p>
                       |<h1>Header</h1>
                       |<h3>sub-heading</h3>
                       |<p><a href="http://scala-lang.org">Scala</a></p>
                       |<pre>x match {
                       |case Some(v) => println(v)
                       |case None => ()
                       |}</pre>
                       |<ul>
                       |<li><p>unordered bullet 1</p></li>
                       |<li><p>unordered bullet 2</p></li>
                       |</ul>
                       |<ol>
                       |<li><p>ordered bullet 1</p></li>
                       |<li><p>ordered bullet 2</p></li>
                       |</ol>""".stripMargin,
                       provider.getClassDoc(classOf[ScalaGreetingsResource]))

    compareDocString("""<p>Now let's test some html formatted scaladoc.</p>
                       |<p><b>Some html</b> with a <a href="http://rest.li">link</a>. x<sup>a</sup><sub>b</sub>.</p>
                       |<ul>
                       |<li><p>unordered bullet 1</p></li>
                       |<li><p>unordered bullet 2</p></li>
                       |</ul>""".stripMargin,
                       provider.getMethodDoc(method))

    compareDocString("<p>provides the key.</p>", provider.getParamDoc(method, "id"))

    val action = classOf[ScalaGreetingsResource].getMethod("action", classOf[java.lang.String], classOf[java.lang.Boolean], classOf[java.lang.Boolean])

    compareDocString("<p>An action.</p>".stripMargin, provider.getMethodDoc(action))
    compareDocString("<p>provides a String</p>", provider.getParamDoc(action, "param1"))
    compareDocString("<p>provides a Boolean</p>", provider.getParamDoc(action, "param2"))
  }

  private def compareDocString(actual: String, expected: String) {
    Assert.assertEquals(actual.replaceAll("\n", "").trim, expected.replaceAll("\n", "").trim)
  }
}