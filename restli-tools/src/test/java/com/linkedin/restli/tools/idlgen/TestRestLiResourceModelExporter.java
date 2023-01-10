/*
   Copyright (c) 2012 LinkedIn Corp.

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

package com.linkedin.restli.tools.idlgen;

import com.linkedin.pegasus.generator.GeneratorResult;
import com.linkedin.restli.tools.ExporterTestUtils;
import java.io.File;
import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * Tests to ensure that {@link RestLiResourceModelExporter} generates IDL files correctly.
 *
 * @author dellamag
 */
public class TestRestLiResourceModelExporter
{
  private static final String FS = File.separator;
  private static final String TEST_DIR = "src" + FS + "test" + FS + "java";
  private static final String IDLS_DIR = "src" + FS + "test" + FS + "resources" + FS + "idls";
  private static final String IDL_DIR = "src" + FS + "test" + FS + "idl";

  private File outdir;
  // Gradle by default will use the module directory as the working directory
  // IDEs such as IntelliJ IDEA may use the project directory instead
  // If you create test in IDE, make sure the working directory is always the module directory
  private String moduleDir;

  @BeforeMethod
  public void setUp() throws IOException
  {
    outdir = ExporterTestUtils.createTmpDir();
    moduleDir = System.getProperty("user.dir");
  }

  @AfterMethod
  public void tearDown()
  {
    ExporterTestUtils.rmdir(outdir);
  }

  @DataProvider(name = "resourceModelData")
  public Object[][] provideResourceModelData()
  {
    return new Object[][]
        {
            { "twitter", new String[] { "com.linkedin.restli.tools.twitter" }, IDLS_DIR, new String[] {
                "twitter-statuses.restspec.json",
                "twitter-statusesWrapped.restspec.json",
                "twitter-statusesAsync.restspec.json",
                "twitter-statusesAsyncWrapped.restspec.json",
                "twitter-statusPromises.restspec.json",
                "twitter-statusPromisesWrapped.restspec.json",
                "twitter-statusTasks.restspec.json",
                "twitter-statusTasksWrapped.restspec.json",
                "twitter-statusesParams.restspec.json",
                "twitter-follows.restspec.json",
                "twitter-accounts.restspec.json",
                "twitter-trending.restspec.json" } },
            { null, new String[] { "com.linkedin.restli.tools.sample" }, IDL_DIR, new String[] {
                "com.linkedin.restli.tools.sample.greetings.restspec.json",
                "com.linkedin.restli.tools.sample.customKeyAssociation.restspec.json"} },
            { "returnEntity", new String[] { "com.linkedin.restli.tools.returnentity" }, IDLS_DIR, new String[] {
                "returnEntity-annotation.restspec.json"} },
            { "serviceErrors", new String[] { "com.linkedin.restli.tools.errors" }, IDLS_DIR, new String[] {
                "serviceErrors-collection.restspec.json",
                "serviceErrors-simple.restspec.json",
                "serviceErrors-association.restspec.json",
                "serviceErrors-actions.restspec.json" } }
        };
  }

  @Test(dataProvider = "resourceModelData")
  @SuppressWarnings("Duplicates")
  public void testExportResourceModel(String apiName, String[] resourcePackages, String idlPath, String[] expectedFiles) throws Exception
  {
    RestLiResourceModelExporter exporter = new RestLiResourceModelExporter();

    Assert.assertEquals(outdir.list().length, 0);
    GeneratorResult result = exporter.export(apiName,
        null,
        new String[] {moduleDir + FS + TEST_DIR},
        resourcePackages,
        null,
        outdir.getAbsolutePath());

    Assert.assertEquals(outdir.list().length, expectedFiles.length);
    Assert.assertEquals(result.getModifiedFiles().size(), expectedFiles.length);
    Assert.assertEquals(result.getTargetFiles().size(), expectedFiles.length);

    for (String file : expectedFiles)
    {
      String actualFile = outdir + FS + file;
      String expectedFile = moduleDir + FS + idlPath + FS + file;

      ExporterTestUtils.compareFiles(actualFile, expectedFile);
      Assert.assertTrue(result.getModifiedFiles().contains(new File(actualFile)));
      Assert.assertTrue(result.getTargetFiles().contains(new File(actualFile)));
    }
  }
}
