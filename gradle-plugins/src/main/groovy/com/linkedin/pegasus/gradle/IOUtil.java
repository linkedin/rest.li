package com.linkedin.pegasus.gradle;

import java.io.File;
import org.gradle.util.GFileUtils;


public class IOUtil {

  /**
   * Writes text to file. Handles IO exceptions.
   */
  public static void writeText(File target, String text) {
    target.getParentFile().mkdirs();
    GFileUtils.writeFile(text, target);
  }
}
