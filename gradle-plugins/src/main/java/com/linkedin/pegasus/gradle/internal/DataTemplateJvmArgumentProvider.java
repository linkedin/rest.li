package com.linkedin.pegasus.gradle.internal;

import java.io.File;
import java.util.Arrays;
import org.gradle.process.CommandLineArgumentProvider;


/**
 * Provides JVM arguments to the task that generates data templates.
 */
public class DataTemplateJvmArgumentProvider implements CommandLineArgumentProvider
{
  private final String _resolverPath;
  private final File _rootDir;

  public DataTemplateJvmArgumentProvider(String resolverPath, File rootDir)
  {
    _resolverPath = resolverPath;
    _rootDir = rootDir;
  }

  @Override
  public Iterable<String> asArguments()
  {
    return Arrays.asList("-Dgenerator.resolver.path=" + _resolverPath, "-Droot.path=" + _rootDir.getPath());
  }
}
