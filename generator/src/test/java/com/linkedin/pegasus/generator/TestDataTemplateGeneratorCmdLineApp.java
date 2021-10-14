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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


@Test(singleThreaded = true)
public class TestDataTemplateGeneratorCmdLineApp
{
  private static final String FS = File.separator;
  private static final String TEST_DIR = System.getProperty("testDir", new File("src/test").getAbsolutePath());
  private static final String RESOURCES_DIR = "resources" + FS + "generator";
  private static final String PEGASUS_DIR = TEST_DIR + FS + RESOURCES_DIR;

  private static final Pattern GENERATED_ANNOTATION_PATTERN = Pattern.compile("@Generated\\(value\\s*=\\s*\"[^\"]+\","
      + "\\s*comments\\s*=\\s*\"Rest\\.li Data Template\\. (Generated from [^\"]+)\\.\"\\)");

  private File _tempDir;
  private File _dataTemplateTargetDir1;
  private File _dataTemplateTargetDir2;

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
    expectedTypeNamesToSourceFileMap.put("WithoutResolverExample", PEGASUS_DIR + FS + "WithoutResolverExample.pdsc");
    expectedTypeNamesToSourceFileMap.put("InlineRecord", PEGASUS_DIR + FS + "WithoutResolverExample.pdsc");

    Map<String, String> expectedTypeNamesToSourceFileMapPdl = new HashMap<>();
    expectedTypeNamesToSourceFileMapPdl.put("WithoutResolverExamplePdl", PEGASUS_DIR + FS + "WithoutResolverExamplePdl.pdl");
    expectedTypeNamesToSourceFileMapPdl.put("InlineRecord", PEGASUS_DIR + FS + "WithoutResolverExamplePdl.pdl");

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
    testRunGenerator(pegasusFilename, expectedTypeNamesToSourceFileMap, PEGASUS_DIR, null, null);
  }

  @DataProvider(name = "createWithoutResolverWithRootPathCases")
  private Object[][] createWithoutResolverWithRootPathCases()
  {
    Map<String, String> expectedTypeNamesToSourceFileMap = new HashMap<>();
    expectedTypeNamesToSourceFileMap.put("WithoutResolverExample", RESOURCES_DIR + FS + "WithoutResolverExample.pdsc");
    expectedTypeNamesToSourceFileMap.put("InlineRecord", RESOURCES_DIR + FS + "WithoutResolverExample.pdsc");

    Map<String, String> expectedTypeNamesToSourceFileMapPdl = new HashMap<>();
    expectedTypeNamesToSourceFileMapPdl.put("WithoutResolverExamplePdl", RESOURCES_DIR + FS + "WithoutResolverExamplePdl.pdl");
    expectedTypeNamesToSourceFileMapPdl.put("InlineRecord", RESOURCES_DIR + FS + "WithoutResolverExamplePdl.pdl");

    return new Object[][]
        {
            { "WithoutResolverExample.pdsc", expectedTypeNamesToSourceFileMap },
            { "WithoutResolverExamplePdl.pdl", expectedTypeNamesToSourceFileMapPdl }
        };
  }
  @Test(dataProvider = "createWithoutResolverWithRootPathCases")
  public void testRunGeneratorWithoutResolverWithRootPath(String pegasusFilename,
      Map<String, String> expectedTypeNamesToSourceFileMap) throws Exception
  {
    testRunGenerator(pegasusFilename, expectedTypeNamesToSourceFileMap, null, null, TEST_DIR);
  }

  @DataProvider(name = "withResolverCases")
  private Object[][] createWithResolverCases()
  {
    Map<String, String> expectedTypeNamesToSourceFileMap = new HashMap<>();
    expectedTypeNamesToSourceFileMap.put("WithoutResolverExamplePdl", PEGASUS_DIR + FS + "WithoutResolverExamplePdl.pdl");
    expectedTypeNamesToSourceFileMap.put("InlineRecord", PEGASUS_DIR + FS + "WithoutResolverExamplePdl.pdl");
    expectedTypeNamesToSourceFileMap.put("WithResolverExample", PEGASUS_DIR + FS + "WithResolverExample.pdl");
    return new Object[][]
        {
            { "WithResolverExample.pdl", expectedTypeNamesToSourceFileMap }
        };
  }

  @Test(dataProvider = "withResolverCases")
  public void testRunGeneratorWithResolver(String pegasusFilename, Map<String, String> expectedTypeNamesToSourceFileMap)
      throws Exception
  {
    testRunGenerator(pegasusFilename, expectedTypeNamesToSourceFileMap, PEGASUS_DIR);
  }

  @Test(dataProvider = "withResolverCases")
  public void testRunGeneratorWithResolverUsingArgFile(String pegasusFilename,
      Map<String, String> expectedTypeNamesToSourceFileMap) throws Exception
  {
    File tempDir = Files.createTempDirectory("restli").toFile();
    File argFile = new File(tempDir, "resolverPath");
    Files.write(argFile.toPath(), Collections.singletonList(PEGASUS_DIR));
    testRunGenerator(pegasusFilename, expectedTypeNamesToSourceFileMap, String.format("@%s", argFile.toPath()));
  }

  private void testRunGenerator(String pegasusFilename, Map<String, String> expectedTypeNamesToSourceFileMap,
      String resolverPath) throws Exception
  {
    testRunGenerator(pegasusFilename, expectedTypeNamesToSourceFileMap, resolverPath, null, null);
  }

  private void testRunGenerator(String pegasusFilename, Map<String, String> expectedTypeNamesToSourceFileMap,
      String resolverPath, List<String> resolverDirectories, String rootPath) throws Exception
  {
    Map<String, File> generatedFiles = generatePegasusDataTemplates(
        pegasusFilename, resolverPath, resolverDirectories, rootPath);
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

      // Match the generated annotation (use regex since this is error-prone)
      final Matcher generatedAnnotationMatcher = GENERATED_ANNOTATION_PATTERN.matcher(generatedSource);
      Assert.assertTrue(generatedAnnotationMatcher.find(), "Unable to find a valid @Generated annotation in the generated source file.");
      final String expectedGeneratedAnnotation = "Generated from " + expectedTypeNamesToSourceFileMap.get(pegasusTypeName);
      Assert.assertEquals(generatedAnnotationMatcher.group(1), expectedGeneratedAnnotation, "Unexpected @Generated annotation.");

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
  private Map<String, File> generatePegasusDataTemplates(String pegasusFilename,
      String resolverPath, List<String> resolverDirectories, String rootPath) throws IOException
  {
    String tempDirectoryPath = _tempDir.getAbsolutePath();
    File pegasusFile = new File(PEGASUS_DIR + FS + pegasusFilename);
    ArrayList<String> args = new ArrayList<>();
    args.add("-d");
    args.add(tempDirectoryPath);
    if (resolverPath != null)
    {
      args.add("-p");
      args.add(resolverPath);
    }
    if (rootPath != null)
    {
      args.add("-t");
      args.add(rootPath);
    }
    if (resolverDirectories != null)
    {
      args.add("-r");
      args.add(String.join(",", resolverDirectories));
    }
    args.add(pegasusFile.getAbsolutePath());

    DataTemplateGeneratorCmdLineApp.main(args.toArray(new String[0]));

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

  @Test
  public void testGeneratorWithCustomResolverDirectory() throws Exception
  {
    // Add PDL entries to the JAR under multiple resolver directories
    File jarFile = new File(_tempDir, "testWithResolverDirectory.jar");
    Map<String, String> jarEntries = new HashMap<>();
    jarEntries.put("custom/CustomResolverFoo.pdl", "record CustomResolverFoo {}");
    createJarFile(jarFile, jarEntries);

    // Define the expected output
    Map<String, String> expectedTypeNamesToSourceFileMap = new HashMap<>();
    expectedTypeNamesToSourceFileMap.put("NeedsCustomResolver", PEGASUS_DIR + FS + "NeedsCustomResolver.pdl");
    expectedTypeNamesToSourceFileMap.put("CustomResolverFoo", jarFile + ":custom/CustomResolverFoo.pdl");

    testRunGenerator("NeedsCustomResolver.pdl", expectedTypeNamesToSourceFileMap, jarFile.getCanonicalPath(),
        Collections.singletonList("custom"), null);
  }

  @Test
  public void testGeneratorWithMultipleCustomResolverDirectories() throws Exception
  {
    // Add PDL entries to the JAR under multiple resolver directories
    File jarFile = new File(_tempDir, "testWithResolverDirectories.jar");
    Map<String, String> jarEntries = new HashMap<>();
    jarEntries.put("custom1/CustomResolverFoo.pdl", "record CustomResolverFoo { ref: CustomResolverBar }");
    jarEntries.put("custom2/CustomResolverBar.pdl", "record CustomResolverBar { ref: CustomTransitive }");
    // This entry is transitively referenced (not referenced by the source model)
    jarEntries.put("custom3/CustomTransitive.pdl", "record CustomTransitive {}");
    createJarFile(jarFile, jarEntries);

    // Define the expected output
    Map<String, String> expectedTypeNamesToSourceFileMap = new HashMap<>();
    expectedTypeNamesToSourceFileMap.put("NeedsCustomResolvers", PEGASUS_DIR + FS + "NeedsCustomResolvers.pdl");
    expectedTypeNamesToSourceFileMap.put("CustomResolverFoo", jarFile + ":custom1/CustomResolverFoo.pdl");
    expectedTypeNamesToSourceFileMap.put("CustomResolverBar", jarFile + ":custom2/CustomResolverBar.pdl");
    expectedTypeNamesToSourceFileMap.put("CustomTransitive", jarFile + ":custom3/CustomTransitive.pdl");

    testRunGenerator("NeedsCustomResolvers.pdl", expectedTypeNamesToSourceFileMap, jarFile.getCanonicalPath(),
        Arrays.asList("custom1", "custom2", "custom3"), null);
  }

  private File[] generateDataTemplateFiles(File targetDir, String[] pegasusFilenames) throws Exception
  {
    File tempDir = Files.createTempDirectory("restli").toFile();
    File argFile = new File(tempDir, "resolverPath");
    Files.write(argFile.toPath(), Collections.singletonList(PEGASUS_DIR));
    String[] mainArgs = new String[pegasusFilenames.length + 4];
    mainArgs[0] = "-d";
    mainArgs[1] = targetDir.getAbsolutePath();
    mainArgs[2] = "-p";
    mainArgs[3] = String.format("@%s", argFile.toPath());
    for (int i = 0; i < pegasusFilenames.length; i++)
    {
      mainArgs[i+4] = new File(PEGASUS_DIR + FS + pegasusFilenames[i]).getAbsolutePath();
    }
    DataTemplateGeneratorCmdLineApp.main(mainArgs);
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

  /**
   * Creates the specified JAR file containing the given text entries.
   * @param target file to write to
   * @param jarEntries entries to write in plaintext, keyed by entry name
   */
  private void createJarFile(File target, Map<String, String> jarEntries) throws IOException
  {
    if (!target.exists())
    {
      target.createNewFile();
    }

    // Create the ZIP file
    try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(target)))
    {
      for (String entryName : jarEntries.keySet())
      {
        // Add ZIP entry to output stream at the given location.
        out.putNextEntry(new ZipEntry(entryName));
        // Write the file contents to this entry and close
        out.write(jarEntries.get(entryName).getBytes(Charset.defaultCharset()));
        out.closeEntry();
      }
    }
  }
}
