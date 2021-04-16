package com.linkedin.restli.tools.clientgen;


import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.linkedin.pegasus.generator.GeneratorResult;
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
  private boolean isFileSystemCaseSensitive;

  @BeforeClass
  public void setUp() throws IOException
  {
    outdir = ExporterTestUtils.createTmpDir();
    outdir2 = ExporterTestUtils.createTmpDir();
    moduleDir = System.getProperty("user.dir");
    isFileSystemCaseSensitive = isFileSystemCaseSensitive();
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
  @Test
  public void testDeterministicMethodOrder() throws Exception
  {
    final String pegasusDir = moduleDir + FS + RESOURCES_DIR + FS + "pegasus";
    final String outPath = outdir.getPath();
    RestRequestBuilderGenerator.run(pegasusDir,
            null,
            moduleDir,
            true,
            false,
            outPath,
            new String[] { moduleDir + FS + RESOURCES_DIR + FS + "idls" + FS + "testSimple.restspec.json" });

    final File builderFile = new File(outPath + FS + "com" + FS + "linkedin" + FS + "restli" + FS + "swift" + FS + "integration" + FS + "TestSimpleRequestBuilders.java");
    Assert.assertTrue(builderFile.exists());

    final String builderFileContent = IOUtils.toString(new FileInputStream(builderFile));
    Assert.assertTrue(builderFileContent.contains("Generated from " + RESOURCES_DIR + FS + "idls" + FS + "testSimple.restspec.json"));

    List<String> actualMethodNames = StaticJavaParser.parse(builderFileContent)
            .findAll(MethodDeclaration.class).stream()
            .map(MethodDeclaration::getNameAsString)
            .collect(Collectors.toList());
    List<String> expectedMethodNames = Lists.newArrayList(
            "getPrimaryResource",
            "options",
            "get",
            "update",
            "delete");
    Assert.assertEquals(actualMethodNames, expectedMethodNames, "Expected method names to be generated in explicit order.");
  }

  @Test
  public void testGeneration() throws Exception
  {
    final String pegasusDir = moduleDir + FS + RESOURCES_DIR + FS + "pegasus";
    final String outPath = outdir.getPath();
    RestRequestBuilderGenerator.run(pegasusDir,
                                    null,
                                    moduleDir,
                                    true,
                                    false,
                                    outPath,
                                    new String[] { moduleDir + FS + RESOURCES_DIR + FS + "idls" + FS + "arrayDuplicateA.restspec.json" });
    RestRequestBuilderGenerator.run(pegasusDir,
                                    null,
                                    moduleDir,
                                    true,
                                    false,
                                    outPath,
                                    new String[] { moduleDir + FS + RESOURCES_DIR + FS + "idls" + FS + "arrayDuplicateB.restspec.json" });

    final File aBuilderFile = new File(outPath + FS + "ArrayDuplicateARequestBuilders.java");
    final File bBuilderFile = new File(outPath + FS + "ArrayDuplicateBRequestBuilders.java");
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
   *   file1: namespace = com.astro
   *   file2: namespace = com.ASTRO
   *
   *   The following files would be generated with the path specified.
   *   1) Case insensitive (if file1 is generated first):
   *       com/astro/file1
   *                /file2
   *   2) Case insensitive (if file2 is generated first):
   *       com/ASTRO/file1
   *                /file2
   *   3) Case sensitive:
   *       com/astro/file1
   *           ASTRO/file2
   *
   * @param restspec1 First restli spec to generate
   * @param restspec2 Second restli spec to generate
   * @param generateLowercasePath True, generate path lowercase; False, generate path as spec specifies.
   */
  @Test(dataProvider = "arrayDuplicateDataProvider2")
  public void testGenerationPathOrder(String restspec1, String restspec2, boolean generateLowercasePath) throws Exception
  {
    // Given: RestLi version and spec files.
    File tmpDir = ExporterTestUtils.createTmpDir();
    final String pegasusDir = moduleDir + FS + RESOURCES_DIR + FS + "pegasus";
    final String tmpPath = tmpDir.getPath();
    final String file1 = moduleDir + FS + RESOURCES_DIR + FS + "idls" + FS + restspec1;
    final String file2 = moduleDir + FS + RESOURCES_DIR + FS + "idls" + FS + restspec2;

    // When: Generate the files defined by spec.
    GeneratorResult r = RestRequestBuilderGenerator.run(pegasusDir,
            null,
            moduleDir,
            true,
            false,
            tmpPath,
            new String[] { file1 },
            generateLowercasePath);
    GeneratorResult r2 = RestRequestBuilderGenerator.run(pegasusDir,
            null,
            moduleDir,
            true,
            false,
            tmpPath,
            new String[] { file2 },
            generateLowercasePath);

    int c = tmpDir.getCanonicalPath().length();

    // Then: Validate the Builder files were created with the correct paths.
    ArrayList<File> files = new ArrayList<>(r.getModifiedFiles());
    files.addAll(r2.getModifiedFiles());
    Assert.assertTrue(files.size() > 0);
    for (File f : files) {
      Assert.assertTrue(f.exists());
      if (!isFileSystemCaseSensitive && !generateLowercasePath) {
        // Do not validate path case since we would need to read paths from files.
        continue;
      } else if (generateLowercasePath) {
        // Validate path is lowercase.
        String path = f.getCanonicalPath().substring(c);
        int idx = path.lastIndexOf("/") + 1;
        path = path.substring(0, idx);
        Matcher matcher = LOWERCASE_PATH_PATTERN.matcher(path);
        Assert.assertTrue(matcher.find());
      }
      Assert.assertTrue(f.getCanonicalPath().endsWith(f.getAbsolutePath()));
    }

    // Clean up.
    ExporterTestUtils.rmdir(tmpDir);
  }

  /**
   * Validate the lowercase path creation does not effect the target path.
   */
  @Test
  public void testLowercasePathForGeneratedFileDoesNotEffectTargetDirectory() throws IOException
  {
    if (!isFileSystemCaseSensitive) {
        // If system is case insensitive, then this test is a NOP.
        return;
    }

    // Given: Path with upper case letters as part of the target directory's path.
    final File root = ExporterTestUtils.createTmpDir();
    final String pathWithUpperCase = "mainGenerated";
    final String tmpPath = root.getPath() + FS + pathWithUpperCase;
    final File tmpDir = new File(tmpPath);
    tmpDir.mkdir();

    // Given: spec files.
    final String pegasusDir = moduleDir + FS + RESOURCES_DIR + FS + "pegasus";
    final String restspec = "arrayDuplicateB.namespace.restspec.json";
    final String file1 = moduleDir + FS + RESOURCES_DIR + FS + "idls" + FS + restspec;

    // When: Generate the files defined by spec.
    GeneratorResult r = RestRequestBuilderGenerator.run(pegasusDir,
            null,
            moduleDir,
            true,
            false,
            tmpPath,
            new String[] { file1 },
            true);

    // Then: Validate generated files are created in the path without modifying the root path's case.
    Assert.assertTrue(r.getModifiedFiles().size() > 0);
    for (File f : r.getModifiedFiles()) {
      Assert.assertTrue(f.getCanonicalPath().contains(pathWithUpperCase));
      Assert.assertTrue(f.getAbsolutePath().contains(pathWithUpperCase));
    }

    // Clean up.
    ExporterTestUtils.rmdir(root);
  }

  @Test
  public void testOldStylePathIDL() throws Exception
  {
    final String pegasusDir = moduleDir + FS + RESOURCES_DIR + FS + "pegasus";
    final String outPath = outdir.getPath();
    final String outPath2 = outdir2.getPath();
    RestRequestBuilderGenerator.run(pegasusDir,
                                    null,
                                    true,
                                    false,
                                    outPath,
                                    new String[] { moduleDir + FS + RESOURCES_DIR + FS + "idls" + FS + "oldStyleAssocKeysPath.restspec.json" });
    RestRequestBuilderGenerator.run(pegasusDir,
                                    null,
                                    true,
                                    false,
                                    outPath2,
                                    new String[] { moduleDir + FS + RESOURCES_DIR + FS + "idls" + FS + "newStyleAssocKeysPath.restspec.json" });

    final File oldStyleSuperBuilderFile = new File(outPath + FS + "AssocKeysPathRequestBuilders.java");
    final File oldStyleSubBuilderFile = new File(outPath + FS + "SubRequestBuilders.java");
    final File oldStyleSubGetBuilderFile = new File(outPath + FS + "SubGetRequestBuilder.java");
    Assert.assertTrue(oldStyleSuperBuilderFile.exists());
    Assert.assertTrue(oldStyleSubBuilderFile.exists());
    Assert.assertTrue(oldStyleSubGetBuilderFile.exists());

    final File newStyleSubGetBuilderFile = new File(outPath2 + FS + "SubGetRequestBuilder.java");
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
  private static Object[][] arrayDuplicateDataProvider2() {
    return new Object[][] {
      { "arrayDuplicateA.namespace.restspec.json", "arrayDuplicateB.namespace.restspec.json", true},
      { "arrayDuplicateA.namespace.restspec.json", "arrayDuplicateB.namespace.restspec.json", false},
      { "arrayDuplicateB.namespace.restspec.json", "arrayDuplicateA.namespace.restspec.json", true},
      { "arrayDuplicateB.namespace.restspec.json", "arrayDuplicateA.namespace.restspec.json", false},
    };
  }

  /**
   * @return typically false for mac/windows; true for linux
   */
  private static boolean isFileSystemCaseSensitive() throws IOException
  {
    File tmpDir = ExporterTestUtils.createTmpDir();
    File caseSensitiveTestFile = new File(tmpDir + FS + "random_file");
    caseSensitiveTestFile.createNewFile();
    boolean caseSensitive = !new File(tmpDir + FS + "RANDOM_FILE" ).exists();
    caseSensitiveTestFile.delete();
    return caseSensitive;
  }
}
