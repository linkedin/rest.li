package com.linkedin.pegasus.gradle.internal;

import java.util.List;
import org.gradle.process.CommandLineArgumentProvider;


/**
 * Provides arguments to the task that generates data templates.
 */
public class DataTemplateArgumentProvider implements CommandLineArgumentProvider
{
  private final List<String> _args;

  public DataTemplateArgumentProvider(List<String> args)
  {
    _args = args;
  }

  @Override
  public Iterable<String> asArguments()
  {
    return _args;
  }
}
