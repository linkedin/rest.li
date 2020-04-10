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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
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
  private static final String testDir = System.getProperty("testDir");
  private static final String pegasusDir = testDir + FS + "resources" + FS + "generator";

  private File _tempDir;

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

    String pegasusFile = pegasusDir + FS + pegasusFilename;
    String pegasusFileInJar = SCHEMA_PATH_PREFIX + pegasusFile;
    createTempJarFile(Collections.singletonMap(pegasusFile, pegasusFileInJar), jarFile);

    DataSchemaParser parser = new DataSchemaParser(tempDirectoryPath);
    DataSchemaParser.ParseResult parseResult = parser.parseSources(new String[]{jarFile});
    // Two schemas, WithoutResolverExample and InlineRecord (defined inline in WithoutResolverExample)
    assertEquals(parseResult.getSchemaAndLocations().size(), 2);
    Set<String> schemaNames = parseResult.getSchemaAndLocations().keySet().stream().map(DataSchema::getUnionMemberKey).collect(
        Collectors.toSet());
    for (String schema : expectedSchemas)
    {
      assertTrue(schemaNames.contains(schema));
    }
    parseResult.getSchemaAndLocations().values().forEach(loc -> assertEquals(loc.getSourceFile().getAbsolutePath(), jarFile));
  }

  @Test
  public void testParseFromJarFileWithTranslatedSchemas() throws Exception
  {
    String tempDirectoryPath = _tempDir.getAbsolutePath();
    String jarFile = tempDirectoryPath + "/testWithTranslatedSchemas.jar";

    Map<String, String> jarFiles = new HashMap<>();

    // Add the source PDL file to the pegasus directory
    String pdlFile = pegasusDir + FS + "WithoutResolverExamplePdl.pdl";
    String pdlJarDestination = SCHEMA_PATH_PREFIX + "WithoutResolverExamplePdl.pdl";
    jarFiles.put(pdlFile, pdlJarDestination);

    // Translated PDSC files go to "legacyPegasusSchemas", which should be ignored by parser.
    String translatedPegasusFile = pegasusDir + FS + "WithoutResolverExample.pdsc";
    String translatedFileDestination = "legacyPegasusSchemas/WithoutResolverExample.pdsc";

    jarFiles.put(translatedPegasusFile, translatedFileDestination);
    createTempJarFile(jarFiles, jarFile);

    DataSchemaParser parser = new DataSchemaParser(tempDirectoryPath);
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
