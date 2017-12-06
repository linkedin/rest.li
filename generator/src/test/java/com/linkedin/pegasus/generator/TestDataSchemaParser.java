package com.linkedin.pegasus.generator;

import com.google.common.io.Files;
import com.linkedin.data.schema.DataSchema;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


public class TestDataSchemaParser
{
  private static final String FS = File.separator;
  private static final String testDir = System.getProperty("testDir");
  private static final String pegasusDir = testDir + FS + "resources" + FS + "generator";

  @DataProvider(name = "inputFiles")
  private Object[][] createWithoutResolverCases()
  {
    return new String[][]
        {
            {"WithoutResolverExample.pdsc"},
            {"WithoutResolverExample.pdl"}
        };
  }

  @Test(dataProvider = "inputFiles")
  public void testParseFromJarFile(String pegasusFilename) throws Exception
  {
    String temp = Files.createTempDir().getAbsolutePath();
    String jarFile = temp + FS + "test.jar";

    String pegasusFile = pegasusDir + FS + pegasusFilename;
    createTempJarFile(new String[] {pegasusFile}, jarFile);

    DataSchemaParser parser = new DataSchemaParser(temp);
    DataSchemaParser.ParseResult parseResult = parser.parseSources(new String[]{jarFile});
    // Two schemas, WithoutResolverExample and InlineRecord (defined inline in WithoutResolverExample)
    assertEquals(parseResult.getSchemaAndLocations().size(), 2);
    Set<String> schemaNames = parseResult.getSchemaAndLocations().keySet().stream().map(DataSchema::getUnionMemberKey).collect(
        Collectors.toSet());
    assertTrue(schemaNames.contains("WithoutResolverExample"));
    assertTrue(schemaNames.contains("InlineRecord"));
    parseResult.getSchemaAndLocations().values().forEach(loc -> assertEquals(loc.getSourceFile().getAbsolutePath(), jarFile));
  }

  private void createTempJarFile(String[] source, String target) throws  Exception
  {
    // Create a buffer for reading the files
    byte[] buf = new byte[1024];

    // Create the ZIP file
    try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(target)))
    {

      // Compress the files
      for (String aSource : source)
      {
        try (FileInputStream in = new FileInputStream(aSource))
        {

          // Add ZIP entry to output stream.
          out.putNextEntry(new ZipEntry(aSource));

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
