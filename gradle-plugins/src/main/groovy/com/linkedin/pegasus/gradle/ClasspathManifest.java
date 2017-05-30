package com.linkedin.pegasus.gradle;

import java.io.File;


/**
 * Prepares classpath manifest of given set of files
 */
class ClasspathManifest {

  /**
   * Prepares classpath manifest of given set of files
   *
   * @param relativePathRoot - the root directory that the relative paths will be calculated from
   * @param files - the files to include in the resulting manifest
   */
  static String relativeClasspathManifest(File relativePathRoot, Iterable<File> files) {
    StringBuilder sb = new StringBuilder();
    files.forEach(f -> sb.append(relativePathRoot.toPath().relativize(f.toPath())).append(" "));
    return sb.toString().trim();
  }
}
