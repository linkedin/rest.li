/*
   Copyright (c) 2020 LinkedIn Corp.

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

package com.linkedin.internal.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;


public class ArgumentFileProcessor
{

  private ArgumentFileProcessor()
  {
    // prevent instantiation
  }

  /**
   * Determine if a path represents an arg file or not
   * @param path the (maybe) arg file path
   * @return true if the path begins with `@`, false otherwise
   */
  public static boolean isArgFile(String path)
  {
    return path.startsWith("@");
  }

  /**
   * Convenience method to expand an argument file.
   * @param path the path representing the arg file
   * @return a String[] holding the arg file contents, one entry per line
   * @throws IOException if unable to open the arg file
   */
  public static String[] getContentsAsArray(String path) throws IOException
  {
    if (!isArgFile(path)) {
      throw new IllegalArgumentException(path + " is not an argument file.");
    }

    File argFile = new File(path.substring(1));
    return Files.readAllLines(argFile.toPath()).toArray(new String[0]);
  }
}
