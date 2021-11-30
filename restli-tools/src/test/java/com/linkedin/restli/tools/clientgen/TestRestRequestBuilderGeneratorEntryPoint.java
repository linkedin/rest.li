/*
   Copyright (c) 2020 LinkedIn Corp.

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

package com.linkedin.restli.tools.clientgen;

import com.linkedin.data.schema.generator.AbstractGenerator;
import com.linkedin.restli.internal.common.RestliVersion;
import com.linkedin.restli.tools.ExporterTestUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;

public class TestRestRequestBuilderGeneratorEntryPoint
{

  private static final String FS = File.separator;
  private static final String RESOURCES_DIR = "src" + FS + "test" + FS + "resources";

  private File outdir;
  private File outdir2;
  private String moduleDir;

  private String originalGeneratorResolverPath;
  private String originalGenerateImported;
  private String originalGenerateDataTemplates;
  private String originalVersionString;

  @BeforeClass
  public void setUp() throws IOException
  {
    outdir = ExporterTestUtils.createTmpDir();
    outdir2 = ExporterTestUtils.createTmpDir();
    moduleDir = System.getProperty("user.dir");

    // backup original system properties
    originalGeneratorResolverPath = backupOriginalValueAndOverride(AbstractGenerator.GENERATOR_RESOLVER_PATH, "");
    originalGenerateImported = backupOriginalValueAndOverride(RestRequestBuilderGenerator.GENERATOR_GENERATE_IMPORTED, "true");
    originalGenerateDataTemplates = backupOriginalValueAndOverride(RestRequestBuilderGenerator.GENERATOR_REST_GENERATE_DATATEMPLATES, "false");
    originalVersionString = System.clearProperty(RestRequestBuilderGenerator.GENERATOR_REST_GENERATE_VERSION);
  }

  @AfterClass
  public void tearDown()
  {
    ExporterTestUtils.rmdir(outdir);
    ExporterTestUtils.rmdir(outdir2);

    restoreOriginalValue(AbstractGenerator.GENERATOR_RESOLVER_PATH, originalGeneratorResolverPath);
    restoreOriginalValue(RestRequestBuilderGenerator.GENERATOR_GENERATE_IMPORTED, originalGenerateImported);
    restoreOriginalValue(RestRequestBuilderGenerator.GENERATOR_REST_GENERATE_DATATEMPLATES, originalGenerateDataTemplates);
    restoreOriginalValue(RestRequestBuilderGenerator.GENERATOR_REST_GENERATE_VERSION, originalVersionString);
  }

  private String backupOriginalValueAndOverride(String key, String newValue)
  {
    String originalValue = System.clearProperty(key);
    System.setProperty(key, newValue);
    return originalValue;
  }

  private void restoreOriginalValue(String key, String originalValue)
  {
    System.clearProperty(key);
    if (originalValue != null) {
      System.setProperty(key, originalValue);
    }
  }

  /**
   * This is a hastily-copied clone of {@link TestRestRequestBuilderGenerator#testOldStylePathIDL(RestliVersion, String, String, String)}
   *
   * This test works-around the decision to communicate state using sysprops instead of CLI arguments, and adds
   * coverage for {@link RestRequestBuilderGenerator#main(String[])}, which previously had none.
   */
  @Test(dataProvider = "oldNewStyleDataProvider")
  public void testMainEntryPointCanHandleArgFile(String version, String AssocKeysPathBuildersName, String SubBuildersName, String SubGetBuilderName) throws Exception
  {
    String pegasusDir = moduleDir + FS + RESOURCES_DIR + FS + "pegasus";

    final String outPath = outdir.getPath();
    final String outPath2 = outdir2.getPath();
    final File argFileDir = Files.createTempDirectory("").toFile(); //new File(moduleDir + FS + "argFile");
    final File oldStyleArgFile = new File(argFileDir, "oldStyle.txt");
    final File newStyleArgFile = new File(argFileDir, "newStyle.txt");
    final File resolverPathArgFile = new File(argFileDir, "resolverPath.txt");

    Files.write(oldStyleArgFile.toPath(), Collections.singletonList(moduleDir + FS + RESOURCES_DIR + FS + "idls" + FS + "oldStyleAssocKeysPath.restspec.json"));
    Files.write(newStyleArgFile.toPath(), Collections.singletonList(moduleDir + FS + RESOURCES_DIR + FS + "idls" + FS + "newStyleAssocKeysPath.restspec.json"));
    Files.write(resolverPathArgFile.toPath(), Collections.singletonList(pegasusDir));

    final String[] oldStyleMainArgs = {outPath, String.format("@%s", oldStyleArgFile.getAbsolutePath())};
    final String[] newStyleMainArgs = {outPath2, String.format("@%s", newStyleArgFile.getAbsolutePath())};

    System.setProperty(RestRequestBuilderGenerator.GENERATOR_REST_GENERATE_VERSION, version);
    System.setProperty(AbstractGenerator.GENERATOR_RESOLVER_PATH, String.format("@%s", resolverPathArgFile.getAbsolutePath()));

    RestRequestBuilderGenerator.main(oldStyleMainArgs);
    RestRequestBuilderGenerator.main(newStyleMainArgs);

    final File oldStyleSuperBuilderFile = new File(outPath + FS + AssocKeysPathBuildersName);
    final File oldStyleSubBuilderFile = new File(outPath + FS + SubBuildersName);
    final File oldStyleSubGetBuilderFile = new File(outPath + FS + SubGetBuilderName);
    Assert.assertTrue(oldStyleSuperBuilderFile.exists());
    Assert.assertTrue(oldStyleSubBuilderFile.exists());
    Assert.assertTrue(oldStyleSubGetBuilderFile.exists());

    final File newStyleSubGetBuilderFile = new File(outPath2 + FS + SubGetBuilderName);
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
  private static Object[][] oldNewStyleDataProvider()
  {
    return new Object[][] {
            { "1.0.0", "AssocKeysPathBuilders.java", "SubBuilders.java", "SubGetBuilder.java" },
            { "2.0.0", "AssocKeysPathRequestBuilders.java", "SubRequestBuilders.java", "SubGetRequestBuilder.java", }
    };
  }

}
