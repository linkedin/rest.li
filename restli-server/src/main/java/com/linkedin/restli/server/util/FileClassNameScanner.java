/*
   Copyright (c) 2012 LinkedIn Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.linkedin.restli.server.util;


import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * Scanner to find class names under a directory according to their source file names.
 *
 * @author Keren Jin
 */
public class FileClassNameScanner
{
  /**
   * Construct map from fully qualified class name to filename whose sources are found under a given source directory.
   * All source files are required to have an extension. No specific extension is required.
   *
   * @param sourceDir the source directory to scan
   * @return map from fully qualified class name to filename for scanned source files.
   */
  public static Map<String, String> scan(String sourceDir)
  {
    return scan(sourceDir, null);
  }

  /**
   * Construct map from fully qualified class name to filename whose sources are found under a given source directory.
   * All source files are required to have an extension.
   *
   * @param sourceDir the source directory to scan
   * @param requiredExtension only include files whose extension equals to this parameter
   *                          null if no specific extension is required
   * @return map from fully qualified class name to filename for scanned source files.
   */
  public static Map<String, String> scan(String sourceDir, String requiredExtension)
  {
    final String sourceDirWithSeparator = sourceDir.endsWith(File.separator) ? sourceDir : sourceDir + File.separator;
    final File dir = new File(sourceDirWithSeparator);
    if (!dir.exists() || !dir.isDirectory())
    {
      return Collections.emptyMap();
    }

    final Collection<File> files = FileUtils.listFiles(dir, null, true);
    final Map<String, String> classFileNames = new HashMap<String, String>();
    final int prefixLength = sourceDirWithSeparator.length();
    for (File f : files)
    {
      assert(f.exists() && f.isFile());

      // Ignore hidden dot-files
      if (f.getName().startsWith("."))
      {
        continue;
      }
      final int extensionIndex = f.getName().lastIndexOf('.');
      final String filePath = f.getPath();
      if (extensionIndex < 0 || !filePath.startsWith(sourceDirWithSeparator))
      {
        continue;
      }

      final int reverseExtensionIndex = f.getName().length() - extensionIndex;
      final String classPathName = filePath.substring(prefixLength, filePath.length() - reverseExtensionIndex);
      if (classPathName.contains("."))
      {
        // dot is not allowed in package name, thus not allowed in the directory path
        continue;
      }

      if (requiredExtension != null)
      {
        final String extension = f.getName().substring(extensionIndex + 1);
        if (!extension.equals(requiredExtension))
        {
          continue;
        }
      }
      classFileNames.put(classPathName.replace(File.separator, "."),
                         filePath);
    }

    return classFileNames;
  }
}
