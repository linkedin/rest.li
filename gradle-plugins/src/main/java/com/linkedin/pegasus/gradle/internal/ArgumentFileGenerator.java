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

package com.linkedin.pegasus.gradle.internal;

import org.gradle.api.GradleException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class ArgumentFileGenerator
{

  private ArgumentFileGenerator()
  {
    // prevent instantiation
  }

  /**
   *  Helper method to generate an argument file, containing one argument per line.
   *  This should be later prefixed with '@' and passed to the final argument of RestRequestBuilderGenerator.
   *  <br>
   *  If invoked from a Gradle task, it is recommended pass org.gradle.api.Task#getTemporaryDir()
   *  as the value for tempDir.
   *
   * @param prefix unique value to prepend to the arg file name
   * @param args the iterable of arguments to write to an arg file, one line per entry
   * @param tempDir the directory to hold the arg file
   * @return argument file which can be passed to a CLI or other JavaExec task
   */
  public static File createArgFile(String prefix, List<String> args, File tempDir)
  {
    File argFile;
    try {
      argFile = File.createTempFile(prefix, "arguments", tempDir);
      Files.write(argFile.toPath(), args);
    } catch (IOException e) {
      throw new GradleException("Unable to create argFile", e);
    }
    return argFile;
  }

  /**
   * Prefixes a File's absolute path with '@', indicating that is in an arg file
   * @param argFile the argFile to be passed to a CLI
   * @return absolute path of the file, prefixed with '@'
   */
  public static String getArgFileSyntax(File argFile)
  {
    return "@" + argFile.getAbsolutePath();
  }
}
