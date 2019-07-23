package com.linkedin.pegasus.gradle.internal;

import java.io.File;
import java.io.FileFilter;


public class FileExtensionFilter implements FileFilter
{
  private final String _suffix;

  public FileExtensionFilter(String suffix)
  {
    _suffix = suffix;
  }

  public String getSuffix()
  {
    return _suffix;
  }

  @Override
  public boolean accept(File pathname)
  {
    return pathname.isFile() && pathname.getName().toLowerCase().endsWith(_suffix);
  }
}