package com.linkedin.pegasus.gradle.internal;


import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;


/**
 * Parses the output from CLI({@code RestLiResourceModelCompatibilityChecker}.
 * For details on the format of the output generated, see {@code CompatibilityReport}
 */
public class CompatibilityLogChecker extends OutputStream
{
  private static final Logger LOG = Logging.getLogger(CompatibilityLogChecker.class);

  private StringBuilder wholeTextBuilder = new StringBuilder();
  private StringBuilder lineTextBuilder = new StringBuilder();

  List<FileCompatibility> restSpecCompatibility = new ArrayList<>();
  List<FileCompatibility> modelCompatibility = new ArrayList<>();
  /**
   * Holds the status of rest spec compatibility based on the compatibility level specified by user.
   */
  boolean isRestSpecCompatible = true;
  /**
   * Holds the status of model compatibility based on the compatibility level specified by user.
   */
  boolean isModelCompatible = true;

  /**
   * Holds the status of annotation compatibility.
   */
  boolean isAnnotationCompatible = true;

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

  // See CompatibilityReport for the report format.
  private void processLine(String s)
  {
    String message = s.substring(s.indexOf(':') + 1);
    if (s.startsWith("[RS-COMPAT]"))
    {
      isRestSpecCompatible = Boolean.parseBoolean(message.trim());
    }
    else if (s.startsWith("[MD-COMPAT]"))
    {
      isModelCompatible = Boolean.parseBoolean(message.trim());
    }
    else if (s.startsWith("[RS-C]"))
    {
      restSpecCompatibility.add(new FileCompatibility(message, true));
    }
    else if (s.startsWith("[RS-I]"))
    {
      restSpecCompatibility.add(new FileCompatibility(message, false));
    }
    else if (s.startsWith("[MD-C]"))
    {
      modelCompatibility.add(new FileCompatibility(message, true));
    }
    else if (s.startsWith("[MD-I]"))
    {
      modelCompatibility.add(new FileCompatibility(message, false));
    }
    else if (s.startsWith("[SCHEMA-ANNOTATION-COMPAT]"))
    {
      isAnnotationCompatible = Boolean.parseBoolean(message.trim());
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

  /**
   * @return if rest-spec was compatible based on the compat level passed to the compat checker.
   */
  public boolean isRestSpecCompatible()
  {
    return isRestSpecCompatible;
  }

  /**
   * @return if model was compatible based on the compat level passed to the compat checker.
   */
  public boolean isModelCompatible()
  {
    return isModelCompatible;
  }

  /**
   * @return if annotation was compatible.
   */
  public boolean isAnnotationCompatible()
  {
    return isAnnotationCompatible;
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