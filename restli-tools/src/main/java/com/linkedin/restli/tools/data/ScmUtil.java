/*
   Copyright (c) 2019 LinkedIn Corp.

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

package com.linkedin.restli.tools.data;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic Utilities for Source Code Management.
 * @author ybi
 */
public class ScmUtil
{
  private static final Logger LOGGER = LoggerFactory.getLogger(ScmUtil.class);

  public static final String SOURCE = "$src";

  public static final String DESTINATION = "$dst";

  /**
   * This method is used to preserve source history by running the given command.
   *
   * @param command preserve source history command which is passed by customer,
   * it should contain $src as the name of sourceFile and $dst as the name of destinationFile.
   * For example : "/usr/bin/svn mv \$src \$dst"
   * @param sourceFile source file
   * @param destinationFile destination file
   * @throws IOException
   * @throws InterruptedException
   */
  public static void tryUpdateSourceHistory(String command, File sourceFile, File destinationFile) throws IOException, InterruptedException
  {
    if (isValidPreserveSourceCommand(command))
    {
      command = command.replace(SOURCE, sourceFile.getPath()).replace(DESTINATION, destinationFile.getPath());

      StringBuilder stdout = new StringBuilder();
      StringBuilder stderr = new StringBuilder();

      if (executeWithStandardOutputAndError(command, stdout, stderr) != 0)
      {
        LOGGER.error("Could not run preserve source command : {} successfully. Please check the error message : {}", command, stderr.toString());
        FileUtils.moveFile(sourceFile, destinationFile);
      }
    }
    else
    {
      LOGGER.info("Preserve source command : {} is invalid.", command);
      FileUtils.moveFile(sourceFile, destinationFile);
    }
  }

  private static boolean isValidPreserveSourceCommand(String command)
  {
    return command != null && command.contains(SOURCE) && command.contains(DESTINATION);
  }

  private static int executeWithStandardOutputAndError(String command, StringBuilder stdout, StringBuilder stderr)
      throws IOException, InterruptedException
  {
    Process process = execute(command);
    stdout.append(getInputStreamAsString(process.getInputStream()));
    stderr.append(getInputStreamAsString(process.getErrorStream()));
    return process.exitValue();
  }

  private static Process execute(String command) throws IOException, InterruptedException
  {
    ProcessBuilder processBuilder = new ProcessBuilder(command.split("\\s+"));
    Process process = processBuilder.start();
    process.waitFor();
    return process;
  }

  private static String getInputStreamAsString(InputStream input) throws IOException
  {
    StringBuilder result = new StringBuilder();
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
    boolean firstLine = true;
    String line;
    while ((line = bufferedReader.readLine()) != null)
    {
      if (!firstLine) {
        result.append("\n");
      }
      firstLine = false;
      result.append(line);
    }
    return result.toString();
  }
}
