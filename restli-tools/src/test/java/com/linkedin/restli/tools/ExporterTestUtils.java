/*
   Copyright (c) 2018 LinkedIn Corp.

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

package com.linkedin.restli.tools;

import com.linkedin.data.DataMap;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.restli.tools.idlgen.RestLiResourceModelExporter;
import com.linkedin.restli.tools.snapshot.gen.RestLiSnapshotExporter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import org.testng.Assert;


/**
 * Utility class to aid testing in {@link RestLiResourceModelExporter}, {@link RestLiSnapshotExporter}, as well as
 * other test classes in restli-tools.
 *
 * @author Evan Williams
 */
public class ExporterTestUtils
{
  /**
   * Compares two JSON files and throws an AssertionError if the files are semantically different. Assumes that the
   * root data object in the file is a map. Used mainly to compare the content of generated IDL and snapshot files.
   *
   * @param actualFileName filename of the generated JSON file.
   * @param expectedFileName filename of the reference JSON file.
   * @throws IOException if file read or file parse fails.
   */
  public static void compareFiles(String actualFileName, String expectedFileName) throws IOException
  {
    String actualContent = ExporterTestUtils.readFile(actualFileName);
    String expectedContent = ExporterTestUtils.readFile(expectedFileName);

    //Compare using a map as opposed to line by line
    final JacksonDataCodec jacksonDataCodec = new JacksonDataCodec();
    final DataMap actualContentMap = jacksonDataCodec.stringToMap(actualContent);
    final DataMap expectedContentMap = jacksonDataCodec.stringToMap(expectedContent);

    if(!actualContentMap.equals(expectedContentMap))
    {
      // Ugh... gradle
      PrintStream actualStdout = new PrintStream(new FileOutputStream(FileDescriptor.out));
      actualStdout.println("ERROR " + actualFileName + " does not match " + expectedFileName + " . Printing diff...");
      try
      {
        // TODO environment dependent, not cross platform
        ProcessBuilder pb = new ProcessBuilder("diff", expectedFileName, actualFileName);
        pb.redirectErrorStream();
        Process p = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line = null;

        while ((line = reader.readLine()) != null)
        {
          actualStdout.println(line);
        }
      }
      catch (Exception e)
      {
        // TODO Setup log4j, find appropriate test harness used in R2D2
        actualStdout.println("Error printing diff: " + e.getMessage());
      }
      Assert.fail(actualFileName + " does not match " + expectedFileName);
    }
  }

  public static File createTmpDir() throws IOException
  {
    File temp = File.createTempFile("temp", Long.toString(System.nanoTime()));
    if(! temp.delete())
    {
      throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
    }

    temp = new File(temp.getAbsolutePath() + ".d");

    if(! temp.mkdir())
    {
      throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
    }

    return temp;
  }

  public static void rmdir(File dir)
  {
    if (dir.listFiles() != null)
    {
      for (File f : dir.listFiles())
      {
        f.delete();
      }
    }
    dir.delete();
  }

  private static String readFile(String fileName) throws IOException
  {
    File file = new File(fileName);
    Assert.assertTrue(file.exists() && file.canRead(), "Cannot find file: " + fileName);
    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

    StringBuilder sb = new StringBuilder();
    String line;
    try
    {
      while ((line = reader.readLine()) != null)
      {
        sb.append(line);
      }
    }
    finally
    {
      reader.close();
    }
    return sb.toString();
  }
}
