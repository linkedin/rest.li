package com.linkedin.restli.tools.clientgen;


import com.linkedin.restli.tools.idlgen.TestRestLiResourceModelExporter;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
    outdir2 = TestRestLiResourceModelExporter.createTmpDir();
    moduleDir = System.getProperty("user.dir");
  }

  @AfterClass
  public void tearDown() throws IOException
  {
    TestRestLiResourceModelExporter.rmdir(outdir);
    TestRestLiResourceModelExporter.rmdir(outdir2);
  }

  @Test
  public void test() throws Exception
  {
    final String pegasus_dir = moduleDir + FS + RESOURCES_DIR + FS + "pegasus";
    final String outPath = outdir.getPath();
    RestRequestBuilderGenerator.run(pegasus_dir,
                                    null,
                                    null,
                                    false,
                                    outPath,
                                    new String[] { moduleDir + FS + RESOURCES_DIR + FS + "idls" + FS + "arrayDuplicateA.restspec.json" });
    RestRequestBuilderGenerator.run(pegasus_dir,
                                    null,
                                    null,
                                    false,
                                    outPath,
                                    new String[] { moduleDir + FS + RESOURCES_DIR + FS + "idls" + FS + "arrayDuplicateB.restspec.json" });

    final File aBuilderFile = new File(outPath + FS + "ArrayDuplicateABuilders.java");
    final File bBuilderFile = new File(outPath + FS + "ArrayDuplicateBBuilders.java");
    Assert.assertTrue(aBuilderFile.exists());
    Assert.assertTrue(bBuilderFile.exists());
  }

  @Test
  public void testOldStylePathIDL() throws Exception
  {
    final String pegasus_dir = moduleDir + FS + RESOURCES_DIR + FS + "pegasus";
    final String outPath = outdir.getPath();
    final String outPath2 = outdir2.getPath();
    RestRequestBuilderGenerator.run(pegasus_dir,
                                    null,
                                    null,
                                    false,
                                    outPath,
                                    new String[] { moduleDir + FS + RESOURCES_DIR + FS + "idls" + FS + "oldStyleAssocKeysPath.restspec.json" });
    RestRequestBuilderGenerator.run(pegasus_dir,
                                    null,
                                    null,
                                    false,
                                    outPath2,
                                    new String[] { moduleDir + FS + RESOURCES_DIR + FS + "idls" + FS + "newStyleAssocKeysPath.restspec.json" });

    final File oldStyleSuperBuilderFile = new File(outPath + FS + "AssocKeysPathBuilders.java");
    final File oldStyleSubBuilderFile = new File(outPath + FS + "SubBuilders.java");
    final File oldStyleSubGetBuilderFile = new File(outPath + FS + "SubGetBuilder.java");
    Assert.assertTrue(oldStyleSuperBuilderFile.exists());
    Assert.assertTrue(oldStyleSubBuilderFile.exists());
    Assert.assertTrue(oldStyleSubGetBuilderFile.exists());

    final File newStyleSubGetBuilderFile = new File(outPath2 + FS + "SubGetBuilder.java");
    Assert.assertTrue(newStyleSubGetBuilderFile.exists());

    BufferedReader oldStyleReader = new BufferedReader(new FileReader(oldStyleSubGetBuilderFile));
    BufferedReader newStyleReader = new BufferedReader(new FileReader(newStyleSubGetBuilderFile));

    String oldLine = oldStyleReader.readLine();
    String newLine = newStyleReader.readLine();

    while(!(oldLine == null || newLine == null))
    {
      if (!oldLine.startsWith("@Generated")) // the Generated line contains a time stamp, which could differ between the two files.
      {
        Assert.assertEquals(oldLine, newLine);
      }
      oldLine = oldStyleReader.readLine();
      newLine = newStyleReader.readLine();
    }

    Assert.assertTrue(oldLine == null && newLine == null);

    oldStyleReader.close();
    newStyleReader.close();
  }

  private static final String FS = File.separator;
  private static final String RESOURCES_DIR = "src" + FS + "test" + FS + "resources";

  private File outdir;
  private File outdir2;
  private String moduleDir;
}
