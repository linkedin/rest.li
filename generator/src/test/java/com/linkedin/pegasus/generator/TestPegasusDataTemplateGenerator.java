package com.linkedin.pegasus.generator;

import com.google.common.io.Files;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.resolver.MultiFormatDataSchemaResolver;
import java.io.File;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


public class TestPegasusDataTemplateGenerator {
  private static final String FS = File.separator;
  private static final String testDir = System.getProperty("testDir");
  private static final String pegasusDir = testDir + FS + "resources" + FS + "generator";

  @DataProvider(name = "withoutResolverCases")
  private Object[][] createWithoutResolverCases() {
    return new String[][] {
        new String[] {"WithoutResolverExample.pdsc", "WithoutResolverExample.java", "WithoutResolverExample"},
        new String[] {"WithoutResolverExample.pdl", "WithoutResolverExample.java", "WithoutResolverExample"}
    };
  }

  @Test(dataProvider = "withoutResolverCases")
  public void testRunGeneratorWithoutResolver(String pegasusFilename, String generatedFilename,
      String pegasusTypeName) throws Exception {
    String temp = Files.createTempDir().getAbsolutePath();
    File pegasusFile = new File(pegasusDir + FS + pegasusFilename);
    PegasusDataTemplateGenerator.main(new String[] {temp, pegasusFile.getAbsolutePath()});
    File generated = new File(temp, generatedFilename);
    assertTrue(generated.exists());
    String generatedSource = FileUtils.readFileToString(generated);
    assertTrue(generatedSource.contains("class " + pegasusTypeName));
  }
}
