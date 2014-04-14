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
package com.linkedin.restli.tools.scala

import com.linkedin.restli.internal.server.model.ResourceModelEncoder.DocsProvider
import java.lang.reflect.Method
import scala.tools.nsc.doc.base.{LinkTo, LinkToMember, LinkToTpl, LinkToExternal, Tooltip}
import scala.tools.nsc.doc.model.{MemberEntity, TemplateEntity, Def, DocTemplateEntity}
import tools.nsc.doc.{DocFactory, Settings}
import tools.nsc.reporters.ConsoleReporter
import scala.tools.nsc.doc.base.comment._
import java.util.{Collection => JCollection, Set => JSet, Collections => JCollections}
import scala.collection.JavaConversions.collectionAsScalaIterable
import org.slf4j.{LoggerFactory, Logger}


/**
 * Scaladoc version of a rest.li DocProvider. Compatible with scala nsc 2.10.x.
 */
class ScalaDocsProvider(classpath: Array[String]) extends DocsProvider {
  val log: Logger = LoggerFactory.getLogger(classOf[ScalaDocsProvider])

  def this() = this(Array())

  private var root: Option[DocTemplateEntity] = None

  def registerSourceFiles(files: JCollection[String]) {
    root = if(files.size() == 0) {
      None
    } else {
      val settings = new Settings(error => log.error(error))
      if(classpath == null) {
        settings.usejavacp.value = true
      } else {
        settings.classpath.value = classpath.mkString(":")
      }
      val reporter = new ConsoleReporter(settings)
      val docFactory = new DocFactory(reporter, settings)
      val filelist = if (files == null || files.size == 0) List() else collectionAsScalaIterable(files).toList
      val universe = docFactory.makeUniverse(Left(filelist))
      universe.map(_.rootPackage.asInstanceOf[DocTemplateEntity])
    }
  }

  def supportedFileExtensions: JSet[String] = {
    JCollections.singleton(".scala")
  }

  def getClassDoc(resourceClass: Class[_]): String = {
    findTemplate(resourceClass)
            .flatMap(_.comment)
            .map(toDocString)
            .orNull
  }

  def getClassDeprecatedTag(resourceClass: Class[_]): String = null

  def getMethodDoc(method: Method): String = {
    findMethod(method)
            .flatMap(_.comment)
            .map(toDocString)
            .orNull
  }

  def getMethodDeprecatedTag(method: Method): String = null

  def getParamDoc(method: Method, name: String): String = {
    findMethod(method)
            .flatMap(_.comment)
            .map(_.valueParams(name))
            .map(toDocString)
            .orNull

  }

  private def filterDocTemplates(templates:List[TemplateEntity with MemberEntity]):List[DocTemplateEntity] = {
    val matches = templates filter { template =>
      template.isDocTemplate && template.isClass
    }
    matches.map(_.asInstanceOf[DocTemplateEntity])
  }

  /**
   * Searches the AST starting at "root" for the given class.  E.g. "com.example.Foo.class" is searched for
   * by traversing first down the docTemplate for the template named "com", then "example", then finally "Foo".
   * @param resourceClass
   * @return
   */
  private def findTemplate(resourceClass: Class[_]): Option[DocTemplateEntity] = {
    def findAtPath(docTemplate: DocTemplateEntity, namespaceParts: List[String]): Option[DocTemplateEntity] = {
      namespaceParts match {
        case Nil => None
        case namespacePart :: Nil => filterDocTemplates(docTemplate.templates).find(_.name == namespacePart)
        case namespacePart :: remainingNamespaceParts => {
          docTemplate.templates.find(_.name == namespacePart) match {
            case Some(childDocTemplate: DocTemplateEntity) => findAtPath(childDocTemplate, remainingNamespaceParts)
            case _ => None
          }
        }
      }
    }

    root flatMap {
      r =>
        findAtPath(r, resourceClass.getCanonicalName.split('.').toList)
    }
  }

  /**
   * Given a Method signature (where Method is a method from a JVM .class), finds the matching scala method "Def"
   * (a AST type from the new scala compiler) so we can get it's scaladoc.
   *
   * This can be a bit tricky given that scala "Def" can have represent all possible scala signatures, which
   * includes stuff like:
   *
   * def foo = {}
   * def foo() = {}
   * def foo(a: Int)(b: Int) = {}
   * ...
   *
   * @param methodToFind
   * @return
   */
  private def findMethod(methodToFind: Method): Option[Def]  = {
    findTemplate(methodToFind.getDeclaringClass).flatMap { docTemplateForClass =>
      docTemplateForClass.methods find { templateMethod =>

      // e.g. the scala method "foo(a: Int, b: Int)(c: String)" has two "valueParams", one with two params and a second with one param
        val templateValueParamSetCount = templateMethod.valueParams.length

        // e.g. the JVM method "bar(Integer a, Integer b)" has two parameters
        val methodToFindParamCount = methodToFind.getParameterTypes.length

        // true if both have no params, this covers the special case of a scala "Def" method that has no params, e.g. "def baz" instead of "def baz()"
        val bothHaveNoParams = templateValueParamSetCount == 0 && methodToFindParamCount == 0

        // true if the scala "Def" method has only one "valueParams" and it has the same number of parameters as the "Method".
        val bothHaveSameParamCount = templateValueParamSetCount == 1 && templateMethod.valueParams(0).length == methodToFindParamCount

        val haveMatchingParams = bothHaveNoParams || bothHaveSameParamCount

        // To be precise here, we should check all param types match, but this is exceedingly complex.
        // Method is from java.lang.reflect which has java types and templateMethod is from scala's AST
        // which has scala types.  The mapping between the two, particularly for primitive types, is involved.
        // Given that rest.li has strong method naming conventions,  name and param count should be sufficient
        // in all but the most pathological cases.  One option would be to check the annotations if
        // additional disambiguation is needed.

        (templateMethod.name == methodToFind.getName) && haveMatchingParams
      }
    }
  }

  private def toDocString(comment: Comment): String = {
    toDocString(comment.body).trim
  }

  private def toDocString(body: Body): String = {
    val comment = body.blocks.map(toDocString(_)) mkString ""
    comment.trim
  }

  private def toDocString(linkTo:LinkTo):String = linkTo match {
    case LinkToMember(mbr, tpl) => "" // unsupported
    case LinkToTpl(tpl) => "" // unsupported
    case LinkToExternal(string, url) => s"""<a href="${url}">${string}</a>"""
    case Tooltip(name) => name
  }

  private def toDocString(block: Block): String = block match {
    case Paragraph(inline) => s"<p>${toDocString(inline)}</p>"
    case Title(text, level) => s"<h${level}>${toDocString(text)}</h${level}>"
    case Code(data) => s"<pre>${data}</pre>"
    case UnorderedList(items) => {
      "<ul>" + items.map(i => s"<li>${toDocString(i)}</li>").mkString + "</ul>"
    }
    case OrderedList(items, style) => {
      "<ol>" + items.map(i => s"<li>${toDocString(i)}</li>").mkString + "</ol>"
    }
    case DefinitionList(items) => {
      "<dl>" + items.map{ case (key, value) => s"<dt>${key}</dt><dd>${value}</dd>"}.mkString + "</dl>"
    }
    case HorizontalRule() => "<hr>"
  }

  // We're using html formatting here, like is done by rest.li already for javadoc
  private def toDocString(in: Inline): String = in match {
    case Bold(inline) => s"<b>${toDocString(inline)}</b>"
    case Chain(items) => items.map(toDocString(_)).mkString
    case Italic(inline) => s"<i>${toDocString(inline)}</i>"
    case Link(target, inline) => s"""<a href="${target}">${toDocString(inline)}</a>"""
    case Monospace(inline) => s"<code>${toDocString(inline)}</code>"
    case Summary(inline) => toDocString(inline)
    case Superscript(inline) => s"<sup>${toDocString(inline)}</sup>"
    case Subscript(inline) => s"<sub>${toDocString(inline)}</sub>"
    // we don't have a way to retain scaladoc (or javadoc) entity links, so we'll just include the fully qualified name
    case EntityLink(title, linkTo) => s"""<a href="${toDocString(linkTo)}">${toDocString(title)}</a>"""
    case Text(text) => text
    // underlining is discouraged in html because it makes text resemble a link, so we'll go with em, a popular alternative
    case Underline(inline) => s"<em>${toDocString(inline)}</em>"
    case HtmlTag(rawHtml) => rawHtml
  }
}
