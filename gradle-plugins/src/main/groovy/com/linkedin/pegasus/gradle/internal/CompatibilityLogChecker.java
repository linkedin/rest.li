package com.linkedin.pegasus.gradle.internal;


import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;


/**
 * Parses the output from CLI that report compatibility
 */
public class CompatibilityLogChecker extends OutputStream
{
  private static final Logger LOG = Logging.getLogger(CompatibilityLogChecker.class);

  private StringBuilder wholeTextBuilder = new StringBuilder();
  private StringBuilder lineTextBuilder = new StringBuilder();

  List<FileCompatibility> restSpecCompatibility = new ArrayList<>();
  List<FileCompatibility> modelCompatibility = new ArrayList<>();

  @Override
  public void write(int b)
      throws IOException
  {
    wholeTextBuilder.append((char) b);
    if (b == '\n')
    {
      LOG.lifecycle("[checker] {}", lineTextBuilder.toString());
      processLine(lineTextBuilder.toString());
      lineTextBuilder = new StringBuilder();
    }
    else
    {
      lineTextBuilder.append((char) b);
    }
  }

  private void processLine(String s)
  {
    String fileName = s.substring(s.indexOf(':') + 1);
    if (s.startsWith("[RS-C]"))
    {
      restSpecCompatibility.add(new FileCompatibility(fileName, true));
    }
    else if (s.startsWith("[RS-I]"))
    {
      restSpecCompatibility.add(new FileCompatibility(fileName, false));
    }
    else if (s.startsWith("[MD-C]"))
    {
      modelCompatibility.add(new FileCompatibility(fileName, true));
    }
    else if (s.startsWith("[MD-I]"))
    {
      modelCompatibility.add(new FileCompatibility(fileName, false));
    }
  }

  public String getWholeText()
  {
    return wholeTextBuilder.toString();
  }

  public List<FileCompatibility> getRestSpecCompatibility()
  {
    return restSpecCompatibility;
  }

  public List<FileCompatibility> getModelCompatibility()
  {
    return modelCompatibility;
  }

  public boolean isRestSpecCompatible()
  {
    return !restSpecCompatibility.stream().anyMatch(it -> !it.compatible);
  }

  public boolean isModelCompatible()
  {
    return !modelCompatibility.stream().anyMatch(it -> !it.compatible);
  }

  public static class FileCompatibility
  {
    private final String fileName;
    private final boolean compatible;

    public FileCompatibility(String fileName, boolean compatible)
    {
      this.fileName = fileName;
      this.compatible = compatible;
    }

    public String getFileName()
    {
      return fileName;
    }

    public boolean isCompatible()
    {
      return compatible;
    }
  }
}