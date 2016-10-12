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

package com.linkedin.restli.tools.snapshot.gen;


import com.linkedin.data.DataMap;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.schema.generator.AbstractGenerator;
import com.linkedin.pegasus.generator.GeneratorResult;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;


/**
 * @author Moira Tagle
 * @version $Revision: $
 */

public class TestRestLiSnapshotExporter
{
  // TODO These should be passed in as test config
  private static final String FS = File.separator;
  private static final String TEST_DIR = "src" + FS + "test" + FS + "java";
  private static final String SNAPSHOTS_DIR = "src" + FS + "test" + FS + "resources" + FS + "snapshots";

  private static final String STATUSES_FILE = "twitter-statuses.snapshot.json";
  private static final String STATUSES_PARAMS_FILE = "twitter-statusesParams.snapshot.json";
  private static final String FOLLOWS_FILE = "twitter-follows.snapshot.json";
  private static final String ACCOUNTS_FILE = "twitter-accounts.snapshot.json";
  private static final String TRENDING_FILE = "twitter-trending.snapshot.json";

  private static final String CIRCULAR_FILE = "circular-circular.snapshot.json";

  private static final String GREETINGS_FILE = "sample-com.linkedin.restli.tools.sample.greetings.snapshot.json";

  private File outdir;
  // Gradle by default will use the module directory as the working directory
  // IDE such as IntelliJ IDEA may use the project directory instead
  // If you create test in IDE, make sure the working directory is always the module directory
  private String moduleDir;
  private String resolverPath;

  @BeforeTest
  public void setUpTest()
  {
    moduleDir = System.getProperty("user.dir");

    // set generator.resolver.path...

    final String resourcesDir = moduleDir + File.separator + RESOURCES_SUFFIX;

    resolverPath = System.getProperty(AbstractGenerator.GENERATOR_RESOLVER_PATH);
    if (resolverPath == null)
    {
      resolverPath = resourcesDir + PEGASUS_SUFFIX;
    }
  }

  @BeforeMethod
  public void setUpMethod() throws IOException
  {
    outdir = createTmpDir();
  }

  @AfterMethod
  public void tearDownMethod()
  {
    rmdir(outdir);
  }

  @AfterTest
  public void tearDownTest() throws IOException
  {
    System.clearProperty(AbstractGenerator.GENERATOR_RESOLVER_PATH);
  }

  @Test
  public void testSimpleSnapshot() throws Exception
  {
    RestLiSnapshotExporter exporter = new RestLiSnapshotExporter();
    exporter.setResolverPath(resolverPath);

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
      String expectedFile = SNAPSHOTS_DIR + FS + file;

      compareFiles(actualFile, expectedFile);
      assertTrue(result.getModifiedFiles().contains(new File(actualFile)));
      assertTrue(result.getTargetFiles().contains(new File(actualFile)));
    }
  }

  @Test
  public void testCircularSnapshot() throws Exception
  {
    RestLiSnapshotExporter exporter = new RestLiSnapshotExporter();
    exporter.setResolverPath(resolverPath);

    assertEquals(outdir.list().length, 0);
    GeneratorResult result = exporter.export("circular",
                                             null,
                                             new String[] {moduleDir + FS + TEST_DIR + FS + "snapshot"},
                                             new String[] {"com.linkedin.restli.tools.snapshot.circular"},
                                             null,
                                             outdir.getAbsolutePath());

    String[] expectedFiles = {CIRCULAR_FILE};

    assertEquals(outdir.list().length, expectedFiles.length);
    assertEquals(result.getModifiedFiles().size(), expectedFiles.length);
    assertEquals(result.getTargetFiles().size(), expectedFiles.length);

    for (String file : expectedFiles)
    {
      String actualFile = outdir + FS + file;
      String expectedFile = SNAPSHOTS_DIR + FS + file;

      compareFiles(actualFile, expectedFile);
      assertTrue(result.getModifiedFiles().contains(new File(actualFile)));
      assertTrue(result.getTargetFiles().contains(new File(actualFile)));
    }

  }

  @Test
  public void testSampleGreetingSnapshot() throws Exception
  {
    RestLiSnapshotExporter exporter = new RestLiSnapshotExporter();
    exporter.setResolverPath(moduleDir + File.separator + "src" + File.separator + "test" + File.separator + PEGASUS_SUFFIX);

    assertEquals(outdir.list().length, 0);
    GeneratorResult result = exporter.export("sample",
            null,
            new String[] {moduleDir + FS + TEST_DIR},
            new String[] {"com.linkedin.restli.tools.sample"},
            null,
            outdir.getAbsolutePath());

    String[] expectedFiles = {GREETINGS_FILE};

    assertEquals(outdir.list().length, expectedFiles.length);
    assertEquals(result.getModifiedFiles().size(), expectedFiles.length);
    assertEquals(result.getTargetFiles().size(), expectedFiles.length);

    for (String file : expectedFiles)
    {
      String actualFile = outdir + FS + file;
      String expectedFile = SNAPSHOTS_DIR + FS + file;

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
      fail(actualFileName + " does not match " + expectedFileName);
    }
  }

  private String readFile(String fileName) throws IOException
  {
    File file = new File(fileName);
    assertTrue(file.exists() && file.canRead(), "Cannot find file: " + fileName);
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

  private void rmdir(File dir)
  {
    if (dir.listFiles() != null)
    {
      for (File f : outdir.listFiles())
      {
        f.delete();
      }
    }
    dir.delete();
  }

  private static File createTmpDir() throws IOException
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

  private static final String PEGASUS_SUFFIX = "pegasus" + File.separator;
  private static final String RESOURCES_SUFFIX = "src" + File.separator + "test" + File.separator + "resources" + File.separator;
}
