package com.linkedin.pegasus.gradle.internal


import org.gradle.api.tasks.Internal
import org.gradle.process.CommandLineArgumentProvider


/**
 * Provides JVM arguments to the task that generates data templates.
 */
class DataTemplateJvmArgumentProvider implements CommandLineArgumentProvider
{
  @Internal
  String resolverPath

  @Internal
  File rootDir

  DataTemplateJvmArgumentProvider(String resolverPath, File rootDir)
  {
    this.resolverPath = resolverPath
    this.rootDir = rootDir
  }

  @Override
  Iterable<String> asArguments()
  {
    ["-Dgenerator.resolver.path=${resolverPath}".toString(), "-Droot.path=${rootDir.path}".toString()]
  }
}
