package com.linkedin.restli.tools.clientgen;


import com.linkedin.restli.tools.idlgen.TestRestLiResourceModelExporter;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;


/**
 * @author Keren Jin
 */
public class TestRestRequestBuilderGenerator
{
  @BeforeTest
  public void setUp() throws IOException
  {
    outdir = TestRestLiResourceModelExporter.createTmpDir();

    // we use relative path when running the test from IDE, absolute path from gradle
    String projectDir = System.getProperty(PROJECT_DIR_PROP);
    if (projectDir != null)
    {
      RESOURCES_DIR = projectDir + FS + RESOURCES_DIR;
    }
  }

  @AfterTest
  public void tearDown() throws IOException
  {
    TestRestLiResourceModelExporter.rmdir(outdir);
  }

  @Test
  public void test() throws Exception
  {
    final String pegasus_dir = RESOURCES_DIR + "pegasus";
    RestRequestBuilderGenerator.run(pegasus_dir, null, null, false, outdir.getPath(), new String[] { RESOURCES_DIR + "idls" + FS + "arrayDuplicateA.restspec.json" });
    RestRequestBuilderGenerator.run(pegasus_dir, null, null, false, outdir.getPath(), new String[] { RESOURCES_DIR + "idls" + FS + "arrayDuplicateB.restspec.json" });
  }

  private static final String PROJECT_DIR_PROP = "test.projectDir";

  private static String FS = File.separator;
  private static String RESOURCES_DIR = "src" + FS + "test" + FS + "resources" + FS;
  private File outdir;
}
