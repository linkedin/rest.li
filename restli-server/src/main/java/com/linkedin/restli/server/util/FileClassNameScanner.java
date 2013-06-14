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
 * @author Keren Jin
 */
public class FileClassNameScanner
{
  /**
   * Construct map from fully qualified class name to filename whose sources are found under a given source directory.
   *
   * @param sourceDir the source directory to scan
   * @return map from fully qualified class name to filename for scanned source files.
   */
  public static Map<String, String> scan(String sourceDir)
  {
    final String sourceDirWithSeparator = sourceDir.endsWith(File.separator) ? sourceDir : sourceDir + File.separator;
    final File dir = new File(sourceDirWithSeparator);
    if (!dir.exists() || !dir.isDirectory())
    {
      return Collections.emptyMap();
    }

    // suppress the warning because of inconsistent FileUtils interface
    @SuppressWarnings("unchecked")
    final Collection<File> files = (Collection<File>) FileUtils.listFiles(dir, null, true);
    final Map<String, String> classFileNames = new HashMap<String, String>();
    final int prefixLength = sourceDirWithSeparator.length();
    for (File f : files)
    {
      assert(f.exists() && f.isFile());

      final int extensionIndex = f.getName().lastIndexOf('.');
      final String filePath = f.getPath();
      if (extensionIndex < 0 || !filePath.startsWith(sourceDirWithSeparator))
      {
        continue;
      }

      final int reverseExtensionIndex = f.getName().length() - extensionIndex;
      classFileNames.put(filePath.substring(prefixLength, filePath.length() - reverseExtensionIndex).replace(File.separator, "."),
                         filePath);
    }

    return classFileNames;
  }
}
