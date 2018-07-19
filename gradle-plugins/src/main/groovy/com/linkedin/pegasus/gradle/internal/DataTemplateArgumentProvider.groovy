package com.linkedin.pegasus.gradle.internal


import org.gradle.api.tasks.Internal
import org.gradle.process.CommandLineArgumentProvider


/**
 * Provides arguments to the task that generates data templates.
 */
class DataTemplateArgumentProvider implements CommandLineArgumentProvider
{
  @Internal
  List<String> args

  public DataTemplateArgumentProvider(List<String> args)
  {
    this.args = args
  }

  @Override
  Iterable<String> asArguments()
  {
    args
  }
}
