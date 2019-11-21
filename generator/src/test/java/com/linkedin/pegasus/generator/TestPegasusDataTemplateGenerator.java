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
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class TestPegasusDataTemplateGenerator
{
  private static final String FS = File.separator;
  private static final String testDir = System.getProperty("testDir", new File("src/test").getAbsolutePath());
  private static final String resourcesDir = "resources" + FS + "generator";
  private static final String pegasusDir = testDir + FS + resourcesDir;

  private File _tempDir;

  @AfterTest
  public void afterTest()
  {
    System.clearProperty("root.path");
  }

  @BeforeMethod
  private void beforeMethod() throws IOException
  {
    _tempDir = Files.createTempDirectory(this.getClass().getSimpleName() + System.currentTimeMillis()).toFile();
  }

  @AfterMethod
  private void afterMethod() throws IOException
  {
    FileUtils.forceDelete(_tempDir);
  }

  @DataProvider(name = "withoutResolverCases")
  private Object[][] createWithoutResolverCases()
  {
    return new Object[][]
    {
        { "WithoutResolverExample.pdsc", new String[] { "WithoutResolverExample", "InlineRecord" }},
        { "WithoutResolverExample.pdl", new String[] { "WithoutResolverExample", "InlineRecord" }}
    };
  }

  @Test(dataProvider = "withoutResolverCases")
  public void testRunGeneratorWithoutResolver(String pegasusFilename, String[] expectedTypeNames)
      throws Exception
  {
    testRunGenerator(pegasusFilename, expectedTypeNames, pegasusDir);
  }

  @Test(dataProvider = "withoutResolverCases")
  public void testRunGeneratorWithoutResolverWithRootPath(String pegasusFilename, String[] expectedTypeNames)
      throws Exception
  {
    System.setProperty("root.path", testDir);
    testRunGenerator(pegasusFilename, expectedTypeNames, resourcesDir);
  }

  private void testRunGenerator(String pegasusFilename, String[] expectedTypeNames, String expectedGeneratedDir)
      throws Exception
  {
    SchemaFormatType schemaFormatType = SchemaFormatType.fromFilename(pegasusFilename);
    Assert.assertNotNull(schemaFormatType, "Indeterminable schema format type.");

    Map<String, File> generatedFiles = generatePegasusDataTemplates(pegasusFilename);
    Assert.assertEquals(generatedFiles.keySet(), new HashSet<>(Arrays.asList(expectedTypeNames)),
        "Set of generated files does not match what's expected.");

    for (Map.Entry<String, File> entry : generatedFiles.entrySet())
    {
      String pegasusTypeName = entry.getKey();
      File generated = entry.getValue();

      Assert.assertTrue(generated.exists());
      String generatedSource = FileUtils.readFileToString(generated);
      Assert.assertTrue(generatedSource.contains("class " + pegasusTypeName),
          "Incorrect generated class name.");
      Assert.assertTrue(generatedSource.contains("Generated from " + expectedGeneratedDir + FS + pegasusFilename),
          "Incorrect @Generated annotation.");

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
}
