package com.linkedin.restli.tools.clientgen;


import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.linkedin.pegasus.generator.GeneratorResult;
import com.linkedin.restli.internal.common.RestliVersion;
import com.linkedin.restli.tools.ExporterTestUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

/**
 * @author Keren Jin
 */
public class TestRestRequestBuilderGenerator
{
  private static final String FS = File.separator;
  private static final String RESOURCES_DIR = "src" + FS + "test" + FS + "resources";
  private static final Pattern LOWERCASE_PATH_PATTERN = Pattern.compile("^[a-z/]*$");

  private File outdir;
  private File outdir2;
  private String moduleDir;

  @BeforeClass
  public void setUp() throws IOException
  {
    outdir = ExporterTestUtils.createTmpDir();
    outdir2 = ExporterTestUtils.createTmpDir();
    moduleDir = System.getProperty("user.dir");
  }

  @AfterClass
  public void tearDown()
  {
    ExporterTestUtils.rmdir(outdir);
    ExporterTestUtils.rmdir(outdir2);
  }

  /**
   * <p>Verifies that REST method source code is emitted in natural order (the order in which the
   * {@link com.linkedin.restli.common.ResourceMethod} enum constants are declared).
   * <p>Natural enum order is deterministic.
   */
  @Test(dataProvider = "restliVersionsDataProvider")
  public void testDeterministicMethodOrder(RestliVersion version) throws Exception
  {
    final String pegasusDir = moduleDir + FS + RESOURCES_DIR + FS + "pegasus";
    final String outPath = outdir.getPath();
    RestRequestBuilderGenerator.run(pegasusDir,
            null,
            moduleDir,
            true,
            false,
            version,
            null,
            outPath,
            new String[] { moduleDir + FS + RESOURCES_DIR + FS + "idls" + FS + "testSimple.restspec.json" });

    final File builderFile = new File(outPath + FS + "com" + FS + "linkedin" + FS + "restli" + FS + "swift" + FS + "integration" + FS + "TestSimpleBuilders.java");
    Assert.assertTrue(builderFile.exists());

    final String builderFileContent = IOUtils.toString(new FileInputStream(builderFile));
    Assert.assertTrue(builderFileContent.contains("Generated from " + RESOURCES_DIR + FS + "idls" + FS + "testSimple.restspec.json"));

    List<String> actualMethodNames = StaticJavaParser.parse(builderFileContent)
            .findAll(MethodDeclaration.class).stream()
            .map(MethodDeclaration::getNameAsString)
            .collect(Collectors.toList());
    List<String> expectedMethodNames = Lists.newArrayList(
            "getBaseUriTemplate",
            "getRequestOptions",
            "getPathComponents",
            "assignRequestOptions",
            "getPrimaryResource",
            "options",
            "get",
            "update",
            "delete");
    Assert.assertEquals(actualMethodNames, expectedMethodNames, "Expected method names to be generated in explicit order.");
  }

  @Test(dataProvider = "arrayDuplicateDataProvider")
  public void testGeneration(RestliVersion version, String ABuildersName, String BBuildersName) throws Exception
  {
    final String pegasusDir = moduleDir + FS + RESOURCES_DIR + FS + "pegasus";
    final String outPath = outdir.getPath();
    RestRequestBuilderGenerator.run(pegasusDir,
                                    null,
                                    moduleDir,
                                    true,
                                    false,
                                    version,
                                    null,
                                    outPath,
                                    new String[] { moduleDir + FS + RESOURCES_DIR + FS + "idls" + FS + "arrayDuplicateA.restspec.json" });
    RestRequestBuilderGenerator.run(pegasusDir,
                                    null,
                                    moduleDir,
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

    final String aBuilderFileContent = IOUtils.toString(new FileInputStream(aBuilderFile));
    Assert.assertTrue(aBuilderFileContent.contains("Generated from " + RESOURCES_DIR + FS + "idls" + FS + "arrayDuplicateA.restspec.json"));
    final String bBuilderFileContent = IOUtils.toString(new FileInputStream(bBuilderFile));
    Assert.assertTrue(bBuilderFileContent.contains("Generated from " + RESOURCES_DIR + FS + "idls" + FS + "arrayDuplicateB.restspec.json"));
  }

  /**
   * Testing case sensitivity of generated files. Typically a Mac/Windows system will have a case insensitive file
   * system, whereas a Linux system will have a case sensitive file system. For a case insensitive file system,
   * "~/com/astro" and "~/com/ASTRO" point to the same folder. For a case sensitive file system, "~/com/astro" and "~/com/ASTRO"
   * will be different folders.
   *
   * Example:
   *   File1: namespace = com.ASTRO.generator
   *
   *   The following files would be generated with the path specified.
   *   1) if run before changes:
   *       com/ASTRO/file
   *   2) if run after changes:
   *       com/astro/file
   *
   * @param version RestLi version
   * @param restspec restli spec to generate - spec contains namespace with non-lowercase path.
   */
  @Test(dataProvider = "arrayNamespaceCaseDataProvider")
  public void testGenerationPathIsLowercase(RestliVersion version, String restspec) throws Exception
  {
    // Given: RestLi version and spec files.
    File tmpDir = ExporterTestUtils.createTmpDir();
    final String pegasusDir = moduleDir + FS + RESOURCES_DIR + FS + "pegasus";
    final String tmpPath = tmpDir.getPath();
    final String restspecFile = moduleDir + FS + RESOURCES_DIR + FS + "idls" + FS + restspec;

    // When: Generate the files defined by spec.
    GeneratorResult r = RestRequestBuilderGenerator.run(pegasusDir,
            null,
            moduleDir,
            true,
            false,
            version,
            null,
            tmpPath,
            new String[] { restspecFile });

    int c = tmpDir.getCanonicalPath().length();

    // Then: Validate the Builder files were created with the correct paths.
    for (File f : r.getModifiedFiles()) {
      // Validate file exists.
      Assert.assertTrue(f.exists());

      // Validate path is lowercase.
      String path = f.getCanonicalPath().substring(c);
      int idx = path.lastIndexOf("/") + 1;
      path = path.substring(0, idx);
      Matcher matcher = LOWERCASE_PATH_PATTERN.matcher(path);
      Assert.assertTrue(matcher.find());
    }

    // Clean up.
    ExporterTestUtils.rmdir(tmpDir);
  }

  @Test(dataProvider = "deprecatedByVersionDataProvider")
  public void testDeprecatedByVersion(String idlName, String buildersName, String substituteClassName) throws Exception
  {
    final String pegasusDir = moduleDir + FS + RESOURCES_DIR + FS + "pegasus";
    final String outPath = outdir.getPath();
    RestRequestBuilderGenerator.run(pegasusDir,
                                    null,
                                    moduleDir,
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
    Assert.assertTrue(fileContent.contains("Generated from " + RESOURCES_DIR + FS + "idls" + FS + idlName));
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
  private static RestliVersion[] restliVersionsDataProvider()
  {
    return new RestliVersion[] { RestliVersion.RESTLI_1_0_0, RestliVersion.RESTLI_2_0_0  };
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
  private static Object[][] arrayNamespaceCaseDataProvider() {
    return new Object[][] {
      { RestliVersion.RESTLI_1_0_0, "namespace-case.restspec.json" },
      { RestliVersion.RESTLI_2_0_0, "namespace-case.restspec.json" },
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
}
