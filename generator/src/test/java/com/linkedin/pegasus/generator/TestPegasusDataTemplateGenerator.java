package com.linkedin.pegasus.generator;

import com.google.common.io.Files;
import com.linkedin.data.codec.symbol.InMemorySymbolTable;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
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
    File generated = generatePegasusDataTemplate(pegasusFilename, generatedFilename);
    assertTrue(generated.exists());
    String generatedSource = FileUtils.readFileToString(generated);
    assertTrue(generatedSource.contains("class " + pegasusTypeName));
    assertTrue(generatedSource.contains("Generated from " + pegasusDir + FS + pegasusFilename));

    File symbolFileName = new File(generated.getParentFile(), "symbols");
    assertTrue(symbolFileName.exists());
    InMemorySymbolTable symbolTable = new InMemorySymbolTable(symbolFileName.getAbsolutePath());
    assertEquals(0, symbolTable.getSymbolId("reference"));
    assertEquals(1, symbolTable.getSymbolId("inlineRecord"));
  }

  @Test(dataProvider = "withoutResolverCases")
  public void testRunGeneratorWithoutResolverWithRootPath(String pegasusFilename, String generatedFilename,
      String pegasusTypeName) throws Exception {
    System.setProperty("root.path", testDir);

    File generated = generatePegasusDataTemplate(pegasusFilename, generatedFilename);
    String generatedSource = FileUtils.readFileToString(generated);
    assertTrue(generatedSource.contains("class " + pegasusTypeName));
    assertTrue(generatedSource.contains("Generated from resources" + FS + "generator" + FS + pegasusFilename));

    File symbolFileName = new File(generated.getParentFile(), "symbols");
    assertTrue(symbolFileName.exists());
    InMemorySymbolTable symbolTable = new InMemorySymbolTable(symbolFileName.getAbsolutePath());
    assertEquals(0, symbolTable.getSymbolId("reference"));
    assertEquals(1, symbolTable.getSymbolId("inlineRecord"));
  }

  @BeforeTest
  public void beforeTest() {
    System.setProperty(PegasusDataTemplateGenerator.GENERATOR_GENERATE_SYMBOL_TABLE, String.valueOf(true));
  }

  @AfterTest
  public void afterTest() {
    System.clearProperty("root.path");
    System.clearProperty(PegasusDataTemplateGenerator.GENERATOR_GENERATE_SYMBOL_TABLE);
  }

  private File generatePegasusDataTemplate(String pegasusFilename, String generatedFilename) throws IOException {
    String temp = Files.createTempDir().getAbsolutePath();
    File pegasusFile = new File(pegasusDir + FS + pegasusFilename);
    PegasusDataTemplateGenerator.main(new String[] {temp, pegasusFile.getAbsolutePath()});
    return new File(temp, generatedFilename);
  }
}
