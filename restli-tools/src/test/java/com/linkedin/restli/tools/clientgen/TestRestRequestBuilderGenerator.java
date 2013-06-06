package com.linkedin.restli.tools.clientgen;


import com.linkedin.restli.tools.idlgen.TestRestLiResourceModelExporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;


/**
 * @author Keren Jin
 */
public class TestRestRequestBuilderGenerator
{
  @BeforeClass
  public void setUp() throws IOException
  {
    outdir = TestRestLiResourceModelExporter.createTmpDir();
    moduleDir = System.getProperty("user.dir");
  }

  @AfterClass
  public void tearDown() throws IOException
  {
    TestRestLiResourceModelExporter.rmdir(outdir);
  }

  @Test
  public void test() throws Exception
  {
    final String pegasus_dir = moduleDir + FS + RESOURCES_DIR + FS + "pegasus";
    RestRequestBuilderGenerator.run(pegasus_dir,
                                    null,
                                    null,
                                    false,
                                    outdir.getPath(),
                                    new String[]{moduleDir + FS + RESOURCES_DIR + FS + "idls" + FS + "arrayDuplicateA.restspec.json"});
    RestRequestBuilderGenerator.run(pegasus_dir,
                                    null,
                                    null,
                                    false,
                                    outdir.getPath(),
                                    new String[]{moduleDir + FS + RESOURCES_DIR + FS + "idls" + FS + "arrayDuplicateB.restspec.json"});
  }

  private static final String FS = File.separator;
  private static final String RESOURCES_DIR = "src" + FS + "test" + FS + "resources";

  private File outdir;
  private String moduleDir;
}
