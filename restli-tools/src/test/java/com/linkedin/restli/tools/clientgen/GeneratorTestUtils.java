/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.restli.tools.clientgen;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;


/**
 * @author Min Chen
 */
public class GeneratorTestUtils {
  public static void compareFiles(String actualFileName, String expectedFileName)
      throws Exception {
    String actual = readFile(actualFileName);
    // exclude the header comment with timestamp
    String actualContent = actual.substring(actual.indexOf("import"));
    String expected = readFile(expectedFileName);
    // exclude the header comment with timestamp
    String expectedContent = expected.substring(expected.indexOf("import"));
    if (!actualContent.trim().equals(expectedContent.trim())) {
      PrintStream actualStdout = new PrintStream(new FileOutputStream(FileDescriptor.out));
      actualStdout.println("ERROR " + actualFileName + " does not match " + expectedFileName + " . Printing diff...");
      try {
        ProcessBuilder pb = new ProcessBuilder("diff", expectedFileName, actualFileName);
        pb.redirectErrorStream();
        Process p = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line = null;

        while ((line = reader.readLine()) != null) {
          actualStdout.println(line);
        }
      }
      catch (Exception e) {
        actualStdout.println("Error printing diff: " + e.getMessage());
      }
      fail(actualFileName + " does not match " + expectedFileName);
    }
  }

  private static String readFile(String fileName)
      throws IOException {
    File file = new File(fileName);
    assertTrue(file.exists() && file.canRead(), "Cannot find file: " + fileName);
    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

    StringBuilder sb = new StringBuilder();
    String line = null;
    try {
      while ((line = reader.readLine()) != null) {
        sb.append(line);
      }
    }
    finally {
      reader.close();
    }
    return sb.toString();
  }
}