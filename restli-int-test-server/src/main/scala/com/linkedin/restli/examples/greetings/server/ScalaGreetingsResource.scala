package com.linkedin.restli.examples.greetings.server

import com.linkedin.restli.server.annotations.{ActionParam, RestLiCollection}
import com.linkedin.restli.examples.greetings.api.Greeting
import com.linkedin.restli.server.resources.CollectionResourceTemplate
import com.linkedin.restli.server.annotations.Action

/**
 * A scala rest.li service.
 *
 * Let's test some scaladoc.  First the wiki formats.
 *
 * Styles: '''bold''', ''italic'', `monospace`, __underline__, ^superscript^, ,,subscript,,
 *
 * =Header=
 *
 * ===sub-heading===
 *
 * [[http://scala-lang.org Scala]]
 *
 * {{{
 * x match {
 *   case Some(v) => println(v)
 *   case None => ()
 * }
 * }}}
 *
 *  - unordered bullet 1
 *  - unordered bullet 2
 *
 *  1. ordered bullet 1
 *  1. ordered bullet 2
 *
 * @author Joe Betz
 */
@RestLiCollection(name="scalaGreetings", namespace = "com.linkedin.restli.examples.scala.client")
class ScalaGreetingsResource extends CollectionResourceTemplate[java.lang.Long, Greeting]{

  /**
   * Now let's test some html formatted scaladoc.
   *
   * <b>Some html</b> with a <a href="http://rest.li">link</a>. x<sup>a</sup><sub>b</sub>.
   *
   * <ul>
   *   <li>unordered bullet 1</li>
   *   <li>unordered bullet 2</li>
   * </ul>
   *
   * @param id provides the key.
   * @return a [[com.linkedin.restli.common.EmptyRecord]]
   */
  override def get(id: java.lang.Long) = {
    new Greeting().setId(1l).setMessage("Hello, Scala!")
  }

  /**
   * An action.
   *
   * @param param1 provides a String
   * @param param2 provides a Boolean
   * @return a string response
   */
  @Action(name="action")
  def action(@ActionParam(value="param1") param1: String,
             @ActionParam(value="param2") param2: java.lang.Boolean): String = {
    "Hello"
  }
}

// To make sure we don't accidentally confuse objects and classes, add an object of the same name as the above class
object ScalaGreetingsResource {}