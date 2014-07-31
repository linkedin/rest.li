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

package com.linkedin.restli.tools.idlgen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

import com.linkedin.pegasus.generator.GeneratorResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @author dellamag
 */
public class TestRestLiResourceModelExporter
{
  private static final String FS = File.separator;
  private static final String TEST_DIR = "src" + FS + "test" + FS + "java";
  private static final String IDLS_DIR = "src" + FS + "test" + FS + "resources" + FS + "idls";

  private static final String STATUSES_FILE = "twitter-statuses.restspec.json";
  private static final String STATUSES_PARAMS_FILE = "twitter-statusesParams.restspec.json";
  private static final String FOLLOWS_FILE = "twitter-follows.restspec.json";
  private static final String ACCOUNTS_FILE = "twitter-accounts.restspec.json";
  private static final String TRENDING_FILE = "twitter-trending.restspec.json";

  private File outdir;
  // Gradle by default will use the module directory as the working directory
  // IDE such as IntelliJ IDEA may use the project directory instead
  // If you create test in IDE, make sure the working directory is always the module directory
  private String moduleDir;

  @BeforeClass
  public void setUp() throws IOException
  {
    outdir = createTmpDir();
    moduleDir = System.getProperty("user.dir");
  }

  @AfterClass
  public void tearDown() throws IOException
  {
    rmdir(outdir);
  }

  @Test
  public void testSimpleModel() throws Exception
  {
    RestLiResourceModelExporter exporter = new RestLiResourceModelExporter();

    assertEquals(outdir.list().length, 0);
    GeneratorResult result = exporter.export("twitter",
                                             null,
                                             new String[] {moduleDir + FS + TEST_DIR},
                                             new String[] {"com.linkedin.restli.tools.twitter"},
                                             null,
                                             outdir.getAbsolutePath());

    String[] expectedFiles = {STATUSES_FILE, FOLLOWS_FILE, ACCOUNTS_FILE, STATUSES_PARAMS_FILE, TRENDING_FILE};

    assertEquals(outdir.list().length, expectedFiles.length);
    assertEquals(result.getModifiedFiles().size(), expectedFiles.length);
    assertEquals(result.getTargetFiles().size(), expectedFiles.length);

    for (String file : expectedFiles)
    {
      String actualFile = outdir + FS + file;
      String expectedFile = moduleDir + FS + IDLS_DIR + FS + file;

      compareFiles(actualFile, expectedFile);
      assertTrue(result.getModifiedFiles().contains(new File(actualFile)));
      assertTrue(result.getTargetFiles().contains(new File(actualFile)));
    }
  }

  private void compareFiles(String actualFileName, String expectedFileName)
      throws Exception
  {
    String actualContent = readFile(actualFileName);
    String expectedContent = readFile(expectedFileName);
    if (! actualContent.trim().equals(expectedContent.trim()))
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
//          System.out.println(line);
        }
      }
      catch (Exception e)
      {
        // TODO Setup log4j, find appropriate test harness used in R2D2
        actualStdout.println("Error printing diff: " + e.getMessage());
      }
      fail(actualFileName + " does not match " + expectedFileName);
    }
  }

  private String readFile(String fileName) throws IOException
  {
    File file = new File(fileName);
    assertTrue(file.exists() && file.canRead(), "Cannot find file: " + fileName);
    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

    StringBuilder sb = new StringBuilder();
    String line = null;
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
}
