package com.linkedin.restli.tools.clientgen;


import com.linkedin.restli.tools.idlgen.TestRestLiResourceModelExporter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

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

  @Test(dataProvider = "arrayDuplicateDataProvider")
  public void test(boolean isRestli2Format, String ABuildersName, String BBuildersName) throws Exception
  {
    final String pegasus_dir = moduleDir + FS + RESOURCES_DIR + FS + "pegasus";
    final String outPath = outdir.getPath();
    RestRequestBuilderGenerator.run(pegasus_dir,
                                    null,
                                    null,
                                    false,
                                    isRestli2Format,
                                    outPath,
                                    new String[] { moduleDir + FS + RESOURCES_DIR + FS + "idls" + FS + "arrayDuplicateA.restspec.json" });
    RestRequestBuilderGenerator.run(pegasus_dir,
                                    null,
                                    null,
                                    false,
                                    isRestli2Format,
                                    outPath,
                                    new String[] { moduleDir + FS + RESOURCES_DIR + FS + "idls" + FS + "arrayDuplicateB.restspec.json" });

    final File aBuilderFile = new File(outPath + FS + ABuildersName);
    final File bBuilderFile = new File(outPath + FS + BBuildersName);
    Assert.assertTrue(aBuilderFile.exists());
    Assert.assertTrue(bBuilderFile.exists());
  }

  @Test(dataProvider = "oldNewStyleDataProvider")
  public void testOldStylePathIDL(boolean isRestli2Format, String AssocKeysPathBuildersName, String SubBuildersName, String SubGetBuilderName) throws Exception
  {
    final String pegasus_dir = moduleDir + FS + RESOURCES_DIR + FS + "pegasus";
    final String outPath = outdir.getPath();
    final String outPath2 = outdir2.getPath();
    RestRequestBuilderGenerator.run(pegasus_dir,
                                    null,
                                    null,
                                    false,
                                    isRestli2Format,
                                    outPath,
                                    new String[] { moduleDir + FS + RESOURCES_DIR + FS + "idls" + FS + "oldStyleAssocKeysPath.restspec.json" });
    RestRequestBuilderGenerator.run(pegasus_dir,
                                    null,
                                    null,
                                    false,
                                    isRestli2Format,
                                    outPath2,
                                    new String[] { moduleDir + FS + RESOURCES_DIR + FS + "idls" + FS + "newStyleAssocKeysPath.restspec.json" });

    final File oldStyleSuperBuilderFile = new File(outPath + FS + AssocKeysPathBuildersName);
    final File oldStyleSubBuilderFile = new File(outPath + FS + SubBuildersName);
    final File oldStyleSubGetBuilderFile = new File(outPath + FS + SubGetBuilderName);
    Assert.assertTrue(oldStyleSuperBuilderFile.exists());
    Assert.assertTrue(oldStyleSubBuilderFile.exists());
    Assert.assertTrue(oldStyleSubGetBuilderFile.exists());

    final File newStyleSubGetBuilderFile = new File(outPath2 + FS + SubGetBuilderName);
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

  @DataProvider
  private static Object[][] arrayDuplicateDataProvider()
  {
    return new Object[][] {
      { false, "ArrayDuplicateABuilders.java", "ArrayDuplicateBBuilders.java" },
      { true, "ArrayDuplicateARequestBuilders.java", "ArrayDuplicateBRequestBuilders.java" }
    };
  }

  @DataProvider
  private static Object[][] oldNewStyleDataProvider()
  {
    return new Object[][] {
      { false, "AssocKeysPathBuilders.java", "SubBuilders.java", "SubGetBuilder.java" },
      { true, "AssocKeysPathRequestBuilders.java", "SubRequestBuilders.java", "SubGetRequestBuilder.java", }
    };
  }

  private static final String FS = File.separator;
  private static final String RESOURCES_DIR = "src" + FS + "test" + FS + "resources";

  private File outdir;
  private File outdir2;
  private String moduleDir;
}
