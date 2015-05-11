package com.linkedin.restli.tools.clientgen;


import com.linkedin.restli.internal.common.RestliVersion;
import com.linkedin.restli.tools.idlgen.TestRestLiResourceModelExporter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
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
  public void testGeneration(RestliVersion version, String ABuildersName, String BBuildersName) throws Exception
  {
    final String pegasusDir = moduleDir + FS + RESOURCES_DIR + FS + "pegasus";
    final String outPath = outdir.getPath();
    RestRequestBuilderGenerator.run(pegasusDir,
                                    null,
                                    true,
                                    false,
                                    version,
                                    null,
                                    outPath,
                                    new String[] { moduleDir + FS + RESOURCES_DIR + FS + "idls" + FS + "arrayDuplicateA.restspec.json" });
    RestRequestBuilderGenerator.run(pegasusDir,
                                    null,
                                    true,
                                    false,
                                    version,
                                    null,
                                    outPath,
                                    new String[] { moduleDir + FS + RESOURCES_DIR + FS + "idls" + FS + "arrayDuplicateB.restspec.json" });

    final File aBuilderFile = new File(outPath + FS + ABuildersName);
    final File bBuilderFile = new File(outPath + FS + BBuildersName);
    Assert.assertTrue(aBuilderFile.exists());
    Assert.assertTrue(bBuilderFile.exists());
  }

  @Test(dataProvider = "deprecatedByVersionDataProvider")
  public void testDeprecatedByVersion(String idlName, String buildersName, String substituteClassName) throws Exception
  {
    final String pegasusDir = moduleDir + FS + RESOURCES_DIR + FS + "pegasus";
    final String outPath = outdir.getPath();
    RestRequestBuilderGenerator.run(pegasusDir,
                                    null,
                                    true,
                                    false,
                                    RestliVersion.RESTLI_1_0_0,
                                    RestliVersion.RESTLI_2_0_0,
                                    outPath,
                                    new String[] { moduleDir + FS + RESOURCES_DIR + FS + "idls" + FS + idlName });

    final File builderFile = new File(outPath + FS + buildersName);
    Assert.assertTrue(builderFile.exists());

    final String fileContent = IOUtils.toString(new FileInputStream(builderFile));
    final Pattern regex = Pattern.compile(".*@deprecated$.*\\{@link " + substituteClassName + "\\}.*^@Deprecated$\n^public class .*", Pattern.MULTILINE | Pattern.DOTALL);
    Assert.assertTrue(regex.matcher(fileContent).matches());
  }

  @Test(dataProvider = "oldNewStyleDataProvider")
  public void testOldStylePathIDL(RestliVersion version, String AssocKeysPathBuildersName, String SubBuildersName, String SubGetBuilderName) throws Exception
  {
    final String pegasusDir = moduleDir + FS + RESOURCES_DIR + FS + "pegasus";
    final String outPath = outdir.getPath();
    final String outPath2 = outdir2.getPath();
    RestRequestBuilderGenerator.run(pegasusDir,
                                    null,
                                    true,
                                    false,
                                    version,
                                    null,
                                    outPath,
                                    new String[] { moduleDir + FS + RESOURCES_DIR + FS + "idls" + FS + "oldStyleAssocKeysPath.restspec.json" });
    RestRequestBuilderGenerator.run(pegasusDir,
                                    null,
                                    true,
                                    false,
                                    version,
                                    null,
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
      { RestliVersion.RESTLI_1_0_0, "ArrayDuplicateABuilders.java", "ArrayDuplicateBBuilders.java" },
      { RestliVersion.RESTLI_2_0_0, "ArrayDuplicateARequestBuilders.java", "ArrayDuplicateBRequestBuilders.java" }
    };
  }

  @DataProvider
  private static Object[][] deprecatedByVersionDataProvider()
  {
    return new Object[][] {
        { "arrayDuplicateA.restspec.json", "ArrayDuplicateABuilders.java", "ArrayDuplicateARequestBuilders" },
        { "arrayDuplicateB.restspec.json", "ArrayDuplicateBBuilders.java", "ArrayDuplicateBRequestBuilders" }
    };
  }

  @DataProvider
  private static Object[][] oldNewStyleDataProvider()
  {
    return new Object[][] {
      { RestliVersion.RESTLI_1_0_0, "AssocKeysPathBuilders.java", "SubBuilders.java", "SubGetBuilder.java" },
      { RestliVersion.RESTLI_2_0_0, "AssocKeysPathRequestBuilders.java", "SubRequestBuilders.java", "SubGetRequestBuilder.java", }
    };
  }

  private static final String FS = File.separator;
  private static final String RESOURCES_DIR = "src" + FS + "test" + FS + "resources";

  private File outdir;
  private File outdir2;
  private String moduleDir;
}
