package com.linkedin.restli.tools.clientgen;

import com.linkedin.restli.tools.ExporterTestUtils;
import java.io.File;
import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


/**
 * @author Karthik Balasubramanian
 */
public class TestFluentApiGenerator
{
  private static final String FS = File.separator;
  private static final String RESOURCES_DIR = "src" + FS + "test" + FS + "resources";

  private File outdir;
  private String moduleDir;

  @BeforeClass
  public void setUp() throws IOException
  {
    outdir = ExporterTestUtils.createTmpDir();
    moduleDir = System.getProperty("user.dir");
  }

  @AfterClass
  public void tearDown()
  {
    ExporterTestUtils.rmdir(outdir);
  }

  @Test()
  public void testBasic() throws Exception
  {
    final String pegasusDir = moduleDir + FS + RESOURCES_DIR + FS + "pegasus";
    final String outPath = outdir.getPath();
    FluentApiGenerator.run(pegasusDir,
            moduleDir,
            outPath,
            new String[] { moduleDir + FS + RESOURCES_DIR + FS + "idls" + FS + "testCollection.restspec.json" });

    final File apiFile = new File(outPath + FS + "com" + FS + "linkedin" + FS + "restli" + FS + "swift" + FS + "integration" + FS + "TestCollection.java");
    Assert.assertTrue(apiFile.exists());
  }
}
