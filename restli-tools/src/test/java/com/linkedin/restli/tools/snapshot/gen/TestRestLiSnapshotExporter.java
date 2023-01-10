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

package com.linkedin.restli.tools.snapshot.gen;


import com.linkedin.data.schema.generator.AbstractGenerator;
import com.linkedin.pegasus.generator.GeneratorResult;

import com.linkedin.restli.tools.ExporterTestUtils;
import java.io.File;
import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.data.schema.resolver.FileDataSchemaResolver.DEFAULT_PATH_SEPARATOR;


/**
 * Tests to ensure that {@link RestLiSnapshotExporter} generates snapshot files correctly.
 *
 * @author Moira Tagle
 * @version $Revision: $
 */

public class TestRestLiSnapshotExporter
{
  // TODO These should be passed in as test config
  private static final String FS = File.separator;
  private static final String TEST_DIR = "src" + FS + "test" + FS + "java";
  private static final String SNAPSHOTS_DIR = "src" + FS + "test" + FS + "resources" + FS + "snapshots";

  private File outdir;
  // Gradle by default will use the module directory as the working directory
  // IDE such as IntelliJ IDEA may use the project directory instead
  // If you create test in IDE, make sure the working directory is always the module directory
  private String moduleDir;
  private String resolverPath;

  @BeforeTest
  public void setUpTest()
  {
    moduleDir = System.getProperty("user.dir");

    // set generator.resolver.path...

    final String resourcesDir = moduleDir + File.separator + RESOURCES_SUFFIX;

    resolverPath = System.getProperty(AbstractGenerator.GENERATOR_RESOLVER_PATH);
    if (resolverPath == null)
    {
      resolverPath = resourcesDir + PEGASUS_SUFFIX;
    }
  }

  @BeforeMethod
  public void setUpMethod() throws IOException
  {
    outdir = ExporterTestUtils.createTmpDir();
  }

  @AfterMethod
  public void tearDownMethod()
  {
    ExporterTestUtils.rmdir(outdir);
  }

  @AfterTest
  public void tearDownTest()
  {
    System.clearProperty(AbstractGenerator.GENERATOR_RESOLVER_PATH);
  }

  @DataProvider(name = "exportSnapshotData")
  public Object[][] provideExportSnapshotData()
  {
    return new Object[][]
        {
            // We want to test if the snapshot exporter can run with empty package name and ignore hidden files
            { "all", null, null },
            // The rest are normal snapshot test cases
            { "circular", new String[] { "com.linkedin.restli.tools.snapshot.circular" }, new String[] {
                "circular-circular.snapshot.json" } },
            { "twitter", new String[] { "com.linkedin.restli.tools.twitter" }, new String[] {
                "twitter-statuses.snapshot.json",
                "twitter-statusesWrapped.snapshot.json",
                "twitter-statusesAsync.snapshot.json",
                "twitter-statusesAsyncWrapped.snapshot.json",
                "twitter-statusPromises.snapshot.json",
                "twitter-statusPromisesWrapped.snapshot.json",
                "twitter-statusTasks.snapshot.json",
                "twitter-statusTasksWrapped.snapshot.json",
                "twitter-statusesParams.snapshot.json",
                "twitter-follows.snapshot.json",
                "twitter-accounts.snapshot.json",
                "twitter-trending.snapshot.json" } },
            { "sample", new String[] { "com.linkedin.restli.tools.sample" }, new String[] {
                "sample-com.linkedin.restli.tools.sample.greetings.snapshot.json",
                "sample-com.linkedin.restli.tools.sample.customKeyAssociation.snapshot.json"} },
            { "returnEntity", new String[] { "com.linkedin.restli.tools.returnentity" }, new String[] {
                "returnEntity-annotation.snapshot.json"} },
            { "serviceErrors", new String[] { "com.linkedin.restli.tools.errors" }, new String[] {
                "serviceErrors-collection.snapshot.json",
                "serviceErrors-simple.snapshot.json",
                "serviceErrors-association.snapshot.json",
                "serviceErrors-actions.snapshot.json" } }
        };
  }

  @Test(dataProvider = "exportSnapshotData")
  @SuppressWarnings("Duplicates")
  public void testExportSnapshot(String apiName, String[] resourcePackages, String[] expectedFiles) throws Exception
  {
    RestLiSnapshotExporter exporter = new RestLiSnapshotExporter();
    exporter.setResolverPath(resolverPath
        + DEFAULT_PATH_SEPARATOR
        + moduleDir + File.separator + "src" + File.separator + "test" + File.separator + PEGASUS_SUFFIX);

    Assert.assertEquals(outdir.list().length, 0);
    GeneratorResult result = exporter.export(apiName,
        null,
        // For some reason, the "circular" resource was placed in the "snapshot" subdirectory; include both
        new String[] { moduleDir + FS + TEST_DIR, moduleDir + FS + TEST_DIR + FS + "snapshot" },
        resourcePackages,
        null,
        outdir.getAbsolutePath());

    if (expectedFiles == null)
    {
      return;
    }

    Assert.assertEquals(outdir.list().length, expectedFiles.length);
    Assert.assertEquals(result.getModifiedFiles().size(), expectedFiles.length);
    Assert.assertEquals(result.getTargetFiles().size(), expectedFiles.length);

    for (String file : expectedFiles)
    {
      String actualFile = outdir + FS + file;
      String expectedFile = SNAPSHOTS_DIR + FS + file;

      ExporterTestUtils.compareFiles(actualFile, expectedFile);
      Assert.assertTrue(result.getModifiedFiles().contains(new File(actualFile)));
      Assert.assertTrue(result.getTargetFiles().contains(new File(actualFile)));
    }
  }

  private static final String PEGASUS_SUFFIX = "pegasus" + File.separator;
  private static final String RESOURCES_SUFFIX = "src" + File.separator + "test" + File.separator + "resources" + File.separator;
}
