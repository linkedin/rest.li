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

import com.linkedin.data.codec.symbol.InMemorySymbolTable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


public class TestPegasusDataTemplateGenerator
{
  private static final String FS = File.separator;
  private static final String testDir = System.getProperty("testDir");
  private static final String pegasusDir = testDir + FS + "resources" + FS + "generator";

  private File _tempDir;

  @BeforeTest
  public void beforeTest()
  {
    System.setProperty(PegasusDataTemplateGenerator.GENERATOR_GENERATE_SYMBOL_TABLE, String.valueOf(true));
  }

  @AfterTest
  public void afterTest()
  {
    System.clearProperty("root.path");
    System.clearProperty(PegasusDataTemplateGenerator.GENERATOR_GENERATE_SYMBOL_TABLE);
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
    return new String[][] {
        new String[] {"WithoutResolverExample.pdsc", "WithoutResolverExample.java", "WithoutResolverExample"},
        new String[] {"WithoutResolverExample.pdl", "WithoutResolverExample.java", "WithoutResolverExample"}
    };
  }

  @Test(dataProvider = "withoutResolverCases")
  public void testRunGeneratorWithoutResolver(String pegasusFilename, String generatedFilename,
      String pegasusTypeName) throws Exception
  {
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
      String pegasusTypeName) throws Exception
  {
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

  private File generatePegasusDataTemplate(String pegasusFilename, String generatedFilename) throws IOException
  {
    String tempDirectoryPath = _tempDir.getAbsolutePath();
    File pegasusFile = new File(pegasusDir + FS + pegasusFilename);
    PegasusDataTemplateGenerator.main(new String[] {tempDirectoryPath, pegasusFile.getAbsolutePath()});
    return new File(tempDirectoryPath, generatedFilename);
  }
}
