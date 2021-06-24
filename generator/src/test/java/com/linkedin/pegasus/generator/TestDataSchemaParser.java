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

import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaLocation;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.resolver.SchemaDirectoryName;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.pegasus.generator.FileFormatDataSchemaParser.*;
import static org.testng.Assert.*;


public class TestDataSchemaParser
{
  private static final String FS = File.separator;
  private static final String testDir = System.getProperty("testDir", new File("src/test").getAbsolutePath());
  private static final String TEST_RESOURCES_DIR = testDir + FS + "resources" + FS + "generator";

  private File _tempDir;
  private File _dataTemplateTargetDir;

  @BeforeMethod
  private void beforeMethod() throws IOException
  {
    _tempDir = Files.createTempDirectory(this.getClass().getSimpleName() + System.currentTimeMillis()).toFile();
    _dataTemplateTargetDir = Files.createTempDirectory(this.getClass().getSimpleName() + System.currentTimeMillis()).toFile();
  }

  @AfterMethod
  private void afterMethod() throws IOException
  {
    FileUtils.forceDelete(_tempDir);
    FileUtils.forceDelete(_dataTemplateTargetDir);
  }

  @DataProvider(name = "inputFiles")
  private Object[][] createWithoutResolverCases()
  {
    return new Object[][]
        {
            {"WithoutResolverExample.pdsc", new String[] {"WithoutResolverExample", "InlineRecord" }},
            {"WithoutResolverExamplePdl.pdl", new String[] {"WithoutResolverExamplePdl", "InlineRecord" }}
        };
  }

  @Test(dataProvider = "inputFiles")
  public void testParseFromJarFile(String pegasusFilename, String[] expectedSchemas) throws Exception
  {
    String tempDirectoryPath = _tempDir.getAbsolutePath();
    String jarFile = tempDirectoryPath + FS + "test.jar";

    String pegasusFile = TEST_RESOURCES_DIR + FS + pegasusFilename;
    String pegasusFileInJar = SCHEMA_PATH_PREFIX + pegasusFilename;
    createTempJarFile(Collections.singletonMap(pegasusFile, pegasusFileInJar), jarFile);

    DataSchemaParser parser = new DataSchemaParser.Builder(tempDirectoryPath).build();
    DataSchemaParser.ParseResult parseResult = parser.parseSources(new String[]{jarFile});
    // Two schemas, WithoutResolverExample and InlineRecord (defined inline in WithoutResolverExample)
    assertEquals(parseResult.getSchemaAndLocations().size(), expectedSchemas.length);
    Set<String> schemaNames = parseResult.getSchemaAndLocations().keySet().stream().map(DataSchema::getUnionMemberKey).collect(
        Collectors.toSet());
    for (String schema : expectedSchemas)
    {
      assertTrue(schemaNames.contains(schema));
    }
    parseResult.getSchemaAndLocations().values().forEach(loc -> assertEquals(loc.getSourceFile().getAbsolutePath(), jarFile));
  }

  @Test(dataProvider = "inputFiles")
  public void testCustomSourceSchemaDirectory(String pegasusFilename, String[] expectedSchemas) throws Exception
  {
    String tempDirectoryPath = _tempDir.getAbsolutePath();
    String jarFile = tempDirectoryPath + FS + "test.jar";
    SchemaDirectoryName customSchemaDirectory = () -> "custom";
    String pegasusFile = TEST_RESOURCES_DIR + FS + pegasusFilename;
    String pegasusFileInJar = customSchemaDirectory.getName() + "/" + pegasusFilename;
    createTempJarFile(Collections.singletonMap(pegasusFile, pegasusFileInJar), jarFile);

    // Load with default parser, this will return zero scheams.
    DataSchemaParser parser = new DataSchemaParser.Builder(tempDirectoryPath).build();
    DataSchemaParser.ParseResult parseResult = parser.parseSources(new String[]{jarFile});
    assertEquals(parseResult.getSchemaAndLocations().size(), 0);

    // Now create a parser with custom directory as source
    parser = new DataSchemaParser.Builder(tempDirectoryPath)
        .setSourceDirectories(Collections.singletonList(customSchemaDirectory))
        .build();
    parseResult = parser.parseSources(new String[]{jarFile});
    assertEquals(parseResult.getSchemaAndLocations().size(), expectedSchemas.length);
    Set<String> schemaNames = parseResult.getSchemaAndLocations().keySet().stream().map(DataSchema::getUnionMemberKey).collect(
        Collectors.toSet());
    for (String schema : expectedSchemas)
    {
      assertTrue(schemaNames.contains(schema));
    }
    parseResult.getSchemaAndLocations().values().forEach(loc -> assertEquals(loc.getSourceFile().getAbsolutePath(), jarFile));
  }

  @Test
  public void testCustomResolverSchemaDirectory() throws Exception
  {
    String tempDirectoryPath = _tempDir.getAbsolutePath();
    String jarFile = tempDirectoryPath + FS + "test.jar";
    String schemaDir = TEST_RESOURCES_DIR + FS + "extensionSchemas";
    SchemaDirectoryName customSchemaDirectory = () -> "custom";
    Map<String, String> entryToFileMap = new HashMap<>();
    // FooExtensions is in "extensions" directory and references "Foo" from "custom" directory.
    entryToFileMap.put(schemaDir + FS + "pegasus/Foo.pdl", "custom/Foo.pdl");
    entryToFileMap.put(schemaDir + FS + "extensions/FooExtensions.pdl", "extensions/FooExtensions.pdl");
    createTempJarFile(entryToFileMap, jarFile);

    List<SchemaDirectoryName> resolverDirectories = Arrays.asList(
        SchemaDirectoryName.EXTENSIONS, customSchemaDirectory);
    List<SchemaDirectoryName> sourceDirectories = Collections.singletonList(SchemaDirectoryName.EXTENSIONS);
    DataSchemaParser parser = new DataSchemaParser.Builder(jarFile)
        .setResolverDirectories(resolverDirectories)
        .setSourceDirectories(sourceDirectories)
        .build();

    DataSchemaParser.ParseResult parseResult = parser.parseSources(new String[]{jarFile});
    parseResult = parser.parseSources(new String[]{jarFile});
    // Foo and FooExtensions
    assertEquals(parseResult.getSchemaAndLocations().size(), 2);
    Set<String> schemaNames = parseResult.getSchemaAndLocations().keySet().stream().map(DataSchema::getUnionMemberKey).collect(
        Collectors.toSet());
    assertTrue(schemaNames.contains("FooExtensions"));
    assertTrue(schemaNames.contains("Foo"));
    parseResult.getSchemaAndLocations().values().forEach(loc -> assertEquals(loc.getSourceFile().getAbsolutePath(), jarFile));
  }

  @DataProvider(name = "entityRelationshipInputFiles")
  private Object[][] createResolverWithExtensionDirs()
  {
    return new Object[][]
        {
            {
                new String[]{
                    "extensions/BarExtensions.pdl",
                    "extensions/FooExtensions.pdl",
                    "extensions/FuzzExtensions.pdl",
                    "pegasus/Foo.pdl",
                    "pegasus/Bar.pdl",
                    "pegasus/Fuzz.pdsc"
                },
                new String[]{
                    "FuzzExtensions",
                    "FooExtensions",
                    "BarExtensions"
                }
            },
            {
                new String[]{
                    "extensions/BarExtensions.pdl",
                    "extensions/FooExtensions.pdl",
                    "extensions/FuzzExtensions.pdl",
                    "pegasus/Foo.pdl",
                    "pegasus/Bar.pdl",
                    "pegasus/Fuzz.pdsc"
                },
                new String[]{
                    "FooExtensions",
                    "FuzzExtensions",
                    "BarExtensions"
                }
            },
            {
                new String[]{
                    "pegasus/Foo.pdl",
                    "pegasus/Bar.pdl",
                    "pegasus/Fuzz.pdsc"
                },
                new String[]{}
            },
        };
  }

  @Test(dataProvider = "entityRelationshipInputFiles")
  public void testSchemaFilesInExtensionPathInJar(String[] files, String[] expectedExtensions) throws Exception
  {
    String tempDirectoryPath = _tempDir.getAbsolutePath();
    String jarFile = tempDirectoryPath + FS + "test.jar";
    String schemaDir = TEST_RESOURCES_DIR + FS + "extensionSchemas";
    Map<String, String> entryToFileMap = Arrays.stream(files).collect(Collectors.toMap(
        filename -> schemaDir + FS + filename,
        filename -> filename));
    createTempJarFile(entryToFileMap, jarFile);

    List<SchemaDirectoryName> resolverDirectories = Arrays.asList(
        SchemaDirectoryName.EXTENSIONS, SchemaDirectoryName.PEGASUS);
    List<SchemaDirectoryName> sourceDirectories = Collections.singletonList(SchemaDirectoryName.EXTENSIONS);
    DataSchemaParser parser = new DataSchemaParser.Builder(jarFile)
        .setResolverDirectories(resolverDirectories)
        .setSourceDirectories(sourceDirectories)
        .build();
    DataSchemaParser.ParseResult parseResult = parser.parseSources(new String[]{jarFile});
    Map<DataSchema, DataSchemaLocation> extensions = parseResult.getExtensionDataSchemaAndLocations();
    assertEquals(extensions.size(), expectedExtensions.length);
    Set<String> actualNames = extensions
        .keySet()
        .stream()
        .map(dataSchema -> (NamedDataSchema) dataSchema)
        .map(NamedDataSchema::getName)
        .collect(Collectors.toSet());
    assertEquals(actualNames, Arrays.stream(expectedExtensions).collect(Collectors.toSet()));
  }


  @Test(dataProvider = "entityRelationshipInputFiles")
  public void testSchemaFilesInExtensionPathInFolder(String[] files, String[] expectedExtensions) throws Exception
  {
    String pegasusWithFS = TEST_RESOURCES_DIR + FS;
    String resolverPath = pegasusWithFS + "extensionSchemas/extensions:"
        + pegasusWithFS + "extensionSchemas/others:"
        + pegasusWithFS + "extensionSchemas/pegasus";
    List<SchemaDirectoryName> resolverDirectories = Arrays.asList(
        SchemaDirectoryName.EXTENSIONS, SchemaDirectoryName.PEGASUS);
    List<SchemaDirectoryName> sourceDirectories = Collections.singletonList(SchemaDirectoryName.EXTENSIONS);
    DataSchemaParser parser = new DataSchemaParser.Builder(resolverPath)
        .setResolverDirectories(resolverDirectories)
        .setSourceDirectories(sourceDirectories)
        .build();
    String[] schemaFiles = Arrays.stream(files).map(casename -> TEST_RESOURCES_DIR + FS + "extensionSchemas" + FS + casename).toArray(String[]::new);
    DataSchemaParser.ParseResult parseResult = parser.parseSources(schemaFiles);
    Map<DataSchema, DataSchemaLocation> extensions = parseResult.getExtensionDataSchemaAndLocations();
    assertEquals(extensions.size(), expectedExtensions.length);
    Set<String> actualNames = extensions
        .keySet()
        .stream()
        .map(dataSchema -> (NamedDataSchema) dataSchema)
        .map(NamedDataSchema::getName)
        .collect(Collectors.toSet());
    assertEquals(actualNames, Arrays.stream(expectedExtensions).collect(Collectors.toSet()));
  }


  @DataProvider(name = "ERFilesForBaseSchema")
  private Object[][] dataSchemaFiles()
  {
    return new Object[][]
        {
            {
                new String[]{
                    "extensions/BarExtensions.pdl",
                    "extensions/FooExtensions.pdl",
                    "extensions/FuzzExtensions.pdl",
                    "pegasus/Foo.pdl",
                    "pegasus/Bar.pdl",
                    "pegasus/Fuzz.pdsc"
                },
                new String[]{
                    "Foo",
                    "Bar",
                    "Fuzz",
                    "InlineRecord"
                }
            }
        };
  }

  @Test(dataProvider = "ERFilesForBaseSchema")
  public void testParseResultToGetBaseSchemas(String[] files, String[] expectedSchemaNames) throws Exception
  {
    String pegasusWithFS = TEST_RESOURCES_DIR + FS;
    String resolverPath = pegasusWithFS + "extensionSchemas/extensions:"
        + pegasusWithFS + "extensionSchemas/others:"
        + pegasusWithFS + "extensionSchemas/pegasus";
    DataSchemaParser parser = new DataSchemaParser.Builder(resolverPath).build();
    String[] schemaFiles = Arrays.stream(files).map(casename -> TEST_RESOURCES_DIR + FS + "extensionSchemas" + FS + casename).toArray(String[]::new);
    DataSchemaParser.ParseResult parseResult = parser.parseSources(schemaFiles);
    Map<DataSchema, DataSchemaLocation> bases = parseResult.getBaseDataSchemaAndLocations();
    assertEquals(bases.size(), expectedSchemaNames.length);
    Set<String> actualNames = bases
        .keySet()
        .stream()
        .map(dataSchema -> (NamedDataSchema) dataSchema)
        .map(NamedDataSchema::getName)
        .collect(Collectors.toSet());
    assertEquals(actualNames, Arrays.stream(expectedSchemaNames).collect(Collectors.toSet()));
  }

  @Test
  public void testParseFromJarFileWithTranslatedSchemas() throws Exception
  {
    String tempDirectoryPath = _tempDir.getAbsolutePath();
    String jarFile = tempDirectoryPath + "/testWithTranslatedSchemas.jar";

    Map<String, String> jarFiles = new HashMap<>();

    // Add the source PDL file to the pegasus directory
    String pdlFile = TEST_RESOURCES_DIR + FS + "WithoutResolverExamplePdl.pdl";
    String pdlJarDestination = SCHEMA_PATH_PREFIX + "WithoutResolverExamplePdl.pdl";
    jarFiles.put(pdlFile, pdlJarDestination);

    // Translated PDSC files go to "legacyPegasusSchemas", which should be ignored by parser.
    String translatedPegasusFile = TEST_RESOURCES_DIR + FS + "WithoutResolverExample.pdsc";
    String translatedFileDestination = "legacyPegasusSchemas/WithoutResolverExample.pdsc";

    jarFiles.put(translatedPegasusFile, translatedFileDestination);
    createTempJarFile(jarFiles, jarFile);

    DataSchemaParser parser = new DataSchemaParser.Builder(tempDirectoryPath).build();
    DataSchemaParser.ParseResult parseResult = parser.parseSources(new String[]{jarFile});
    // Two schemas, WithoutResolverExample and InlineRecord (defined inline in WithoutResolverExample)
    assertEquals(parseResult.getSchemaAndLocations().size(), 2);
    Set<String> schemaNames = parseResult.getSchemaAndLocations().keySet().stream().map(DataSchema::getUnionMemberKey).collect(
        Collectors.toSet());
    assertTrue(schemaNames.contains("WithoutResolverExamplePdl"));
    assertTrue(schemaNames.contains("InlineRecord"));
    parseResult.getSchemaAndLocations().values().forEach(loc -> assertEquals(loc.getSourceFile().getAbsolutePath(), jarFile));
  }

  private void createTempJarFile(Map<String, String> sourceFileToJarLocationMap, String target) throws Exception
  {
    // Create a buffer for reading the files
    byte[] buf = new byte[1024];

    // Create the ZIP file
    try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(target)))
    {

      // Compress the files
      for (Map.Entry<String, String> sourceFileAndJarLocation : sourceFileToJarLocationMap.entrySet())
      {
        try (FileInputStream in = new FileInputStream(sourceFileAndJarLocation.getKey()))
        {

          // Add ZIP entry to output stream at the given location.
          out.putNextEntry(new ZipEntry(sourceFileAndJarLocation.getValue()));

          // Transfer bytes from the file to the ZIP file
          int len;
          while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
          }

          // Complete the entry
          out.closeEntry();
        }
      }
    }
  }
}
