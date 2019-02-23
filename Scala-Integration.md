---
layout: guide
title: Scala-Integration
permalink: /Scala-Integration
---
# Scala integration

## Contents

* [SBT Plugin](#sbt-plugin)
* [Writing Resources in Scala](#writing-resources-in-scala)
* [Scaladoc](#scaladoc)

## SBT Plugin

Rest.li is fully integrated with the SBT build system through an SBT plugin.  See [Rest.li SBT Plugin](https://github.com/linkedin/sbt-restli).

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

[Scaladoc](http://docs.scala-lang.org/style/scaladoc.html) is supported using a plugin.  To enable the plugin in Gradle, modify your build.gradle files, adding a dependency on restli-tools-scala and depending on it the module that contains your Rest.li resource Scala classes. Find the latest version of restli-tools-scala on [Maven Central](https://search.maven.org/search?q=g:com.linkedin.sbt-restli%20AND%20a:restli-tools-scala_*).

```gradle
project.ext.externalDependency = [
  // ...
  'scalaLibrary_2_12': 'org.scala-lang:scala-library:2.12.7',
  'restliToolsScala_2_12' : 'com.linkedin.sbt-restli:restli-tools-scala_2.12:0.3.9'
],
```

```gradle
apply plugin: 'pegasus'
apply plugin: 'scala'

ext.apiProject = ...
dependencies {
  // ...
  compile externalDependency.scalaLibrary_2_12
  compile externalDependency.restliToolsScala_2_12
}
```
