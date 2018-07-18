---
layout: guide
title: Scala-Integration
permalink: /Scala-Integration
---

## Contents

* [SBT Plugin](https://github.com/linkedin/rest.li/wiki/Scala-Integration#sbt-plugin)
* [Writing Resources in Scala](https://github.com/linkedin/rest.li/wiki/Scala-Integration#writing-resources-in-scala)
* [Scaladoc](https://github.com/linkedin/rest.li/wiki/Scala-Integration#scaladoc)

## SBT Plugin

Rest.li is fully integrated with the SBT build system through an SBT plugin.  See [Rest.li SBT Plugin](https://github.com/linkedin/rest.li-sbt-plugin).

## Writing Resources in Scala

Rest.li resource classes may be written in Scala, for example:

```scala
**
 * A sample scala resource.
 */
@RestLiCollection(name="sampleScala", namespace = "com.example.restli")
class SampleScalaResource extends CollectionResourceTemplate[java.lang.Long, Sample] with PlayRequest {

  /**
   * A sample scala get.
   */
  override def get(key: java.lang.Long): Sample = {
    val message = "hello world"

    new Sample()
      .setMessage(s"You got this from a Scala Resource: ${message}!")
      .setId(key)
  }
}
```

## Scaladoc

[Scaladoc](http://docs.scala-lang.org/style/scaladoc.html) is supported using a plugin.  This is currently ONLY available for scala 2.10.  To enable the plugin in Gradle, modify your build.gradle files, adding a dependency on restli-tools-scala and depending on it the module that contains your Rest.li resource Scala classes.

```gradle
project.ext.externalDependency = [
  // ...
  'scalaLibrary_2_10': 'org.scala-lang:scala-library:2.10.3',
  "restliToolsScala_2_10" : "com.linkedin.pegasus:restli-tools-scala_2.10:"+pegasusVersion
],
```

```gradle
apply plugin: 'pegasus'
apply plugin: 'scala'

ext.apiProject = ...
dependencies {
  // ...
  compile externalDependency.scalaLibrary_2_10
  compile externalDependency.restliToolsScala_2_10
}
```