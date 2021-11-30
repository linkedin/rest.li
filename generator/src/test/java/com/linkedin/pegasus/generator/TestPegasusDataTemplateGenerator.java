/*
   Copyright (c) 2017 LinkedIn Corp.

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

package com.linkedin.pegasus.generator;

import com.linkedin.data.schema.SchemaFormatType;
import com.linkedin.data.schema.generator.AbstractGenerator;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(singleThreaded = true)
@SuppressWarnings("deprecation")
public class TestPegasusDataTemplateGenerator
{
  private static final String FS = File.separator;
  private static final String testDir = System.getProperty("testDir", new File("src/test").getAbsolutePath());
  private static final String resourcesDir = "resources" + FS + "generator";
  private static final String pegasusDir = testDir + FS + resourcesDir;
  private static final String pegasusDirGenerated = testDir + FS + "resources" + FS + "referenceJava";

  private File _tempDir;
  private File _dataTemplateTargetDir1;
  private File _dataTemplateTargetDir2;
  private String _resolverPath;

  @BeforeClass
  public void setUp()
  {
    _resolverPath = System.clearProperty(AbstractGenerator.GENERATOR_RESOLVER_PATH);
  }

  @AfterClass
  public void tearDown()
  {
    System.clearProperty(AbstractGenerator.GENERATOR_RESOLVER_PATH);
    if (_resolverPath != null)
    {
      System.setProperty(AbstractGenerator.GENERATOR_RESOLVER_PATH, _resolverPath);
    }
  }

  @AfterTest
  public void afterTest()
  {
    System.clearProperty("root.path");
  }

  @BeforeMethod
  private void beforeMethod() throws IOException
  {
    _tempDir = Files.createTempDirectory(this.getClass().getSimpleName() + System.currentTimeMillis()).toFile();
    _dataTemplateTargetDir1 = Files.createTempDirectory(this.getClass().getSimpleName() + System.currentTimeMillis() + "-a").toFile();
    _dataTemplateTargetDir2 = Files.createTempDirectory(this.getClass().getSimpleName() + System.currentTimeMillis() + "-b").toFile();
  }

  @AfterMethod
  private void afterMethod() throws IOException
  {
    FileUtils.forceDelete(_tempDir);
    FileUtils.forceDelete(_dataTemplateTargetDir1);
    FileUtils.forceDelete(_dataTemplateTargetDir2);
  }

  @DataProvider(name = "withoutResolverCases")
  private Object[][] createWithoutResolverCases()
  {
    Map<String, String> expectedTypeNamesToSourceFileMap = new HashMap<>();
    expectedTypeNamesToSourceFileMap.put("WithoutResolverExample", "WithoutResolverExample.pdsc");
    expectedTypeNamesToSourceFileMap.put("InlineRecord", "WithoutResolverExample.pdsc");

    Map<String, String> expectedTypeNamesToSourceFileMapPdl = new HashMap<>();
    expectedTypeNamesToSourceFileMapPdl.put("WithoutResolverExamplePdl", "WithoutResolverExamplePdl.pdl");
    expectedTypeNamesToSourceFileMapPdl.put("InlineRecord", "WithoutResolverExamplePdl.pdl");

    return new Object[][]
    {
        { "WithoutResolverExample.pdsc", expectedTypeNamesToSourceFileMap },
        { "WithoutResolverExamplePdl.pdl", expectedTypeNamesToSourceFileMapPdl }
    };
  }

  @Test(dataProvider = "withoutResolverCases")
  public void testRunGeneratorWithoutResolver(
      String pegasusFilename, Map<String, String> expectedTypeNamesToSourceFileMap) throws Exception
  {
    testRunGenerator(pegasusFilename, expectedTypeNamesToSourceFileMap, pegasusDir);
  }

  @Test(dataProvider = "withoutResolverCases")
  public void testRunGeneratorWithoutResolverWithRootPath(String pegasusFilename,
      Map<String, String> expectedTypeNamesToSourceFileMap) throws Exception
  {
    System.setProperty("root.path", testDir);
    testRunGenerator(pegasusFilename, expectedTypeNamesToSourceFileMap, resourcesDir);
  }

  @DataProvider(name = "withResolverCases")
  private Object[][] createWithResolverCases()
  {
    Map<String, String> expectedTypeNamesToSourceFileMap = new HashMap<>();
    expectedTypeNamesToSourceFileMap.put("WithoutResolverExamplePdl", "WithoutResolverExamplePdl.pdl");
    expectedTypeNamesToSourceFileMap.put("InlineRecord", "WithoutResolverExamplePdl.pdl");
    expectedTypeNamesToSourceFileMap.put("WithResolverExample", "WithResolverExample.pdl");
    return new Object[][]
        {
            { "WithResolverExample.pdl", expectedTypeNamesToSourceFileMap }
        };
  }

  @Test(dataProvider = "withResolverCases")
  public void testRunGeneratorWithResolver(String pegasusFilename, Map<String, String> expectedTypeNamesToSourceFileMap)
      throws Exception
  {
    System.setProperty(AbstractGenerator.GENERATOR_RESOLVER_PATH, pegasusDir);
    testRunGenerator(pegasusFilename, expectedTypeNamesToSourceFileMap, pegasusDir);
  }

  @Test(dataProvider = "withResolverCases")
  public void testRunGeneratorWithResolverUsingArgFile(String pegasusFilename,
      Map<String, String> expectedTypeNamesToSourceFileMap) throws Exception
  {
    File tempDir = Files.createTempDirectory("restli").toFile();
    File argFile = new File(tempDir, "resolverPath");
    Files.write(argFile.toPath(), Collections.singletonList(pegasusDir));
    System.setProperty(AbstractGenerator.GENERATOR_RESOLVER_PATH, String.format("@%s", argFile.toPath()));
    testRunGenerator(pegasusFilename, expectedTypeNamesToSourceFileMap, pegasusDir);
  }

  private void testRunGenerator(String pegasusFilename, Map<String, String> expectedTypeNamesToSourceFileMap,
      String expectedGeneratedDir) throws Exception
  {
    Map<String, File> generatedFiles = generatePegasusDataTemplates(pegasusFilename);
    Assert.assertEquals(generatedFiles.keySet(), expectedTypeNamesToSourceFileMap.keySet(),
        "Set of generated files does not match what's expected.");

    for (Map.Entry<String, File> entry : generatedFiles.entrySet())
    {
      String pegasusTypeName = entry.getKey();
      File generated = entry.getValue();

      Assert.assertTrue(generated.exists());
      String generatedSource = FileUtils.readFileToString(generated);
      Assert.assertTrue(generatedSource.contains("class " + pegasusTypeName),
          "Incorrect generated class name.");
      String expectedGeneratedAnnotation = "Generated from " + expectedGeneratedDir + FS
          + expectedTypeNamesToSourceFileMap.get(pegasusTypeName);
      Assert.assertTrue(generatedSource.contains(expectedGeneratedAnnotation),
          "Incorrect @Generated annotation, expected: " + expectedGeneratedAnnotation);

      SchemaFormatType schemaFormatType = SchemaFormatType.fromFilename(
          expectedTypeNamesToSourceFileMap.get(pegasusTypeName));
      Assert.assertNotNull(schemaFormatType, "Indeterminable schema format type.");

      // TODO: Collapse into one assertion once the codegen logic uses #parseSchema(String, SchemaFormatType) for PDSC.
      if (schemaFormatType == SchemaFormatType.PDSC)
      {
        Assert.assertFalse(generatedSource.contains("SchemaFormatType.PDSC"),
            "Expected no reference to 'SchemaFormatType.PDSC' in schema field initialization.");
      }
      else
      {
        Assert.assertTrue(generatedSource.contains("SchemaFormatType." + schemaFormatType.name()),
            String.format("Expected reference to 'SchemaFormatType.%s' in schema field initialization.",
                schemaFormatType.name()));
      }
    }
  }

  /**
   * Given a source schema filename, generate Java data templates for all types within this schema.
   * @param pegasusFilename source schema filename
   * @return mapping from generated type name to generated file
   */
  private Map<String, File> generatePegasusDataTemplates(String pegasusFilename) throws IOException
  {
    String tempDirectoryPath = _tempDir.getAbsolutePath();
    File pegasusFile = new File(pegasusDir + FS + pegasusFilename);
    PegasusDataTemplateGenerator.main(new String[] {tempDirectoryPath, pegasusFile.getAbsolutePath()});

    File[] generatedFiles = _tempDir.listFiles((File dir, String name) -> name.endsWith(".java"));
    Assert.assertNotNull(generatedFiles, "Found no generated Java files.");
    return Arrays.stream(generatedFiles)
        .collect(Collectors.toMap(
            file -> file.getName().replace(".java", ""),
            Function.identity()));
  }

  /**
   *
   * @return an array of test cases where each case has two array of test schema file names. Those file names are
   *   in the different permutations of same group of test schema files
   */
  @DataProvider(name = "test_schema_permutation_determinism")
  private Object[][] createPermutedDataTemplateCases()
  {
    return new Object[][]
        {
            {new String[]{"ATypeRef.pdsc", "Service.pdsc"}, new String[]{"Service.pdsc", "ATypeRef.pdsc"}},
            {new String[]{"AField.pdl", "ARecord.pdl"}, new String[]{"ARecord.pdl", "AField.pdl"}},
            {new String[]{"BRecord.pdl", "BField.pdl"}, new String[]{"BField.pdl", "BRecord.pdl"}},
            {new String[]{"FooArray1.pdl", "FooArray2.pdl"}, new String[]{"FooArray2.pdl", "FooArray1.pdl"}},
            {new String[]{"FooMap1.pdl", "FooMap2.pdl"}, new String[]{"FooMap2.pdl", "FooMap1.pdl"}},
        };
  }

  @Test(dataProvider = "test_schema_permutation_determinism")
  public void testDataTemplateGenerationDeterminism(String[] schemaFiles1, String[] schemaFiles2)
      throws Exception
  {
    File[] generatedFiles1 = generateDataTemplateFiles(_dataTemplateTargetDir1, schemaFiles1);
    File[] generatedFiles2 = generateDataTemplateFiles(_dataTemplateTargetDir2, schemaFiles2);
    checkGeneratedFilesConsistency(generatedFiles1, generatedFiles2);
  }

  private File[] generateDataTemplateFiles(File targetDir, String[] pegasusFilenames) throws Exception
  {
    File tempDir = Files.createTempDirectory("restli").toFile();
    File argFile = new File(tempDir, "resolverPath");
    Files.write(argFile.toPath(), Collections.singletonList(pegasusDir));
    System.setProperty(AbstractGenerator.GENERATOR_RESOLVER_PATH, String.format("@%s", argFile.toPath()));
    String[] mainArgs = new String[pegasusFilenames.length + 1];
    mainArgs[0] = targetDir.getAbsolutePath();
    for (int i = 0; i < pegasusFilenames.length; i++)
    {
      mainArgs[i+1] = new File(pegasusDir + FS + pegasusFilenames[i]).getAbsolutePath();
    }
    PegasusDataTemplateGenerator.main(mainArgs);
    File[] generatedFiles = targetDir.listFiles((File dir, String name) -> name.endsWith(".java"));
    Assert.assertNotNull(generatedFiles, "Found no generated Java files.");
    return generatedFiles;
  }

  private void checkGeneratedFilesConsistency(File[] generatedFiles1, File[] generatedFiles2) throws IOException
  {
    Arrays.sort(generatedFiles1, Comparator.comparing(File::getAbsolutePath));
    Arrays.sort(generatedFiles2, Comparator.comparing(File::getAbsolutePath));
    Assert.assertEquals(generatedFiles1.length, generatedFiles2.length);
    for (int i = 0; i < generatedFiles1.length; i++)
    {
      Assert.assertTrue(compareTwoFiles(generatedFiles1[i], generatedFiles2[i]));
    }
  }

  private boolean compareTwoFiles(File file1, File file2) throws IOException
  {
    byte[] content1 = Files.readAllBytes(file1.toPath());
    byte[] content2 = Files.readAllBytes(file2.toPath());
    return Arrays.equals(content1, content2);
  }
}
