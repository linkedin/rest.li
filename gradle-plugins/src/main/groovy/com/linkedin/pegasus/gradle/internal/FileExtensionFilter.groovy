package com.linkedin.pegasus.gradle.internal


class FileExtensionFilter implements FileFilter
{
  FileExtensionFilter(String suffix)
  {
    _suffix = suffix
  }

  public boolean accept(File pathname)
  {
    return pathname.isFile() && pathname.name.toLowerCase().endsWith(_suffix);
  }

  public String getSuffix()
  {
    return _suffix
  }

  private String _suffix
}