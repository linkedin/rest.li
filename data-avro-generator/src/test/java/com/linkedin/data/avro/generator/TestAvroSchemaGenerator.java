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

package com.linkedin.data.avro.generator;

import com.linkedin.data.DataMap;
import com.linkedin.data.TestUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Parser;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.data.TestUtil.*;
import static com.linkedin.data.avro.generator.AvroSchemaGenerator.GENERATOR_AVRO_NAMESPACE_OVERRIDE;
import static com.linkedin.data.avro.SchemaTranslator.AVRO_PREFIX;
import static com.linkedin.data.schema.generator.AbstractGenerator.GENERATOR_RESOLVER_PATH;
import static com.linkedin.util.FileUtil.buildSystemIndependentPath;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;


public class TestAvroSchemaGenerator
{
  private boolean _debug = false;
  private File _testDir;
  private Schema.Parser parser = new Parser();

  @BeforeClass
  public void setupSchemaFiles() throws IOException
  {
    _testDir = TestUtil.testDir("testAvroSchemaGenerator/pegasus", _debug);
  }

  @DataProvider
  public Object[][] toAvroSchemaData()
  {
    return new Object[][]
      {
        {
          asMap(buildSystemIndependentPath("a1", "foo.pdsc"), "{ \"name\" : \"foo\", \"type\" : \"record\", \"fields\" : [] }"),
          asMap(buildSystemIndependentPath(AVRO_PREFIX, "a1", "foo.pdsc"), "{\"type\":\"record\",\"name\":\"foo\",\"namespace\":\"avro\",\"fields\":[]}"),
          asList(buildSystemIndependentPath("a1")),
          true
        },
        {
          asMap(buildSystemIndependentPath("a1", "x", "y", "z.pdsc"),     "{ \"name\" : \"x.y.z\", \"type\" : \"record\", \"fields\" : [] }"),
          asMap(buildSystemIndependentPath(AVRO_PREFIX, "a1", "x", "y", "z.pdsc"), "{\"type\":\"record\",\"name\":\"z\",\"namespace\":\"avro.x.y\",\"fields\":[]}"),
          asList(buildSystemIndependentPath("a1")),
          true
        },
        {
          asMap(buildSystemIndependentPath("a2", "b", "bar.pdsc"),     "{ \"name\" : \"bar\", \"type\" : \"fixed\", \"size\" : 34 }"),
          asMap(buildSystemIndependentPath(AVRO_PREFIX, "a2", "b", "baz.pdsc"), "{\"type\":\"record\",\"name\":\"baz\",\"namespace\":\"avro\",\"fields\":[]}"),
          asList(buildSystemIndependentPath("a2", "b")),
          true
        },
        {
          asMap(buildSystemIndependentPath("a3", "b", "c", "baz.pdsc"),   "{ \"name\" : \"baz\", \"type\" : \"record\", \"fields\" : [] }"),
          asMap(buildSystemIndependentPath(AVRO_PREFIX, "a3", "b", "c", "baz.pdsc"), "{\"type\":\"record\",\"name\":\"baz\",\"namespace\":\"avro\",\"fields\":[]}"),
          asList(buildSystemIndependentPath("a3", "b", "c")),
          true
        },
        {
          asMap(buildSystemIndependentPath("a3", "b", "c", "referrer1.pdsc"), "{ \"name\" : \"b.c.referrer1\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"referree\", \"type\" : \"referree1\" } ] }",
              buildSystemIndependentPath("a3", "b", "c", "referree1.pdsc"), "{ \"name\" : \"b.c.referree1\", \"type\" : \"enum\", \"symbols\" : [ \"good\", \"bad\", \"ugly\" ] }"),
          asMap(buildSystemIndependentPath(AVRO_PREFIX, "a3", "b", "c", "referrer1.pdsc"), "{\"type\":\"record\",\"name\":\"referrer1\",\"namespace\":\"avro.b.c\",\"fields\":[{\"name\":\"referree\",\"type\":{\"type\":\"enum\",\"name\":\"referree1\",\"symbols\":[\"good\",\"bad\",\"ugly\"]}}]}",
              buildSystemIndependentPath(AVRO_PREFIX, "a3", "b", "c", "referree1.pdsc"), "{ \"name\" : \"b.c.referree1\", \"type\" : \"enum\", \"symbols\" : [ \"good\", \"bad\", \"ugly\" ] }"),
          asList(buildSystemIndependentPath("a3", "b", "c")),
          true
        },
        {
          asMap(buildSystemIndependentPath("a3", "b", "c", "referrer2.pdsc"), "{ \"name\" : \"referrer2\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"referree\", \"type\" : \"referree2\" } ] }",
              buildSystemIndependentPath("a3", "b", "c", "referree2.pdsc"), "{ \"name\" : \"referree2\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f\", \"type\" : \"string\" } ] }"),
          asMap(buildSystemIndependentPath(AVRO_PREFIX, "a3", "b", "c", "referrer2.pdsc"), "{\"type\":\"record\",\"name\":\"referrer2\",\"namespace\":\"avro\",\"fields\":[{\"name\":\"referree\",\"type\":{\"type\":\"record\",\"name\":\"referree2\",\"fields\":[{\"name\":\"f\",\"type\":\"string\"}]}}]}",
              buildSystemIndependentPath(AVRO_PREFIX, "a3", "b", "c", "referree2.pdsc"), "{\"type\":\"record\",\"name\":\"referree2\",\"namespace\":\"avro\",\"fields\":[{\"name\":\"f\",\"type\":\"string\"}]}"),
          asList(buildSystemIndependentPath("a3", "b", "c")),
          true
        },
        {
          asMap(buildSystemIndependentPath("a3", "b", "c", "referrer3.pdsc"), "{ \"name\" : \"referrer2\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"referree\", \"type\" : \"referree3\" } ] }",
              buildSystemIndependentPath("a3", "b", "d", "referree3.pdsc"), "{ \"name\" : \"referree3\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f\", \"type\" : \"string\" } ] }"),
          asMap(buildSystemIndependentPath(AVRO_PREFIX, "a3", "b", "c", "referrer3.pdsc"), "{\"type\":\"record\",\"name\":\"referrer3\",\"namespace\":\"avro\",\"fields\":[{\"name\":\"referree\",\"type\":{\"type\":\"record\",\"name\":\"referree3\",\"fields\":[{\"name\":\"f\",\"type\":\"string\"}]}}]}",
              buildSystemIndependentPath(AVRO_PREFIX, "a3", "b", "d", "referree3.pdsc"), "{\"type\":\"record\",\"name\":\"referree3\",\"namespace\":\"avro\",\"fields\":[{\"name\":\"f\",\"type\":\"string\"}]}"),
          asList(buildSystemIndependentPath("a3", "b", "c"), buildSystemIndependentPath("a3", "b", "d")),
          true
        },
        {
          asMap(buildSystemIndependentPath("a3", "b", "c", "circular1.pdsc"), "{ \"name\" : \"circular1\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"member\", \"type\" : \"circular2\" } ] }",
              buildSystemIndependentPath("a3", "b", "c", "circular2.pdsc"), "{ \"name\" : \"circular2\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"member\", \"type\" : \"circular1\" } ] }"),
          asMap(buildSystemIndependentPath(AVRO_PREFIX, "a3", "b", "c", "circular1.pdsc"), "{\"type\":\"record\",\"name\":\"circular1\",\"namespace\":\"avro\",\"fields\":[{\"name\":\"member\",\"type\":{\"type\":\"record\",\"name\":\"circular2\",\"fields\":[{\"name\":\"member\",\"type\":\"circular1\"}]}}]}",
              buildSystemIndependentPath(AVRO_PREFIX, "a3", "b", "c", "circular2.pdsc"), "{\"type\":\"record\",\"name\":\"circular2\",\"namespace\":\"avro\",\"fields\":[{\"name\":\"member\",\"type\":{\"type\":\"record\",\"name\":\"circular1\",\"fields\":[{\"name\":\"member\",\"type\":\"circular2\"}]}}]}"),
          asList(buildSystemIndependentPath("a3", "b", "c")),
          true
        },

        // without override
        {
          asMap(buildSystemIndependentPath("a4", "foo.pdsc"), "{ \"name\" : \"foo\", \"type\" : \"record\", \"fields\" : [] }"),
          asMap(buildSystemIndependentPath("a4", "foo.pdsc"), "{\"type\":\"record\",\"name\":\"foo\",\"fields\":[]}"),
          asList(buildSystemIndependentPath("a4")),
          false
        },
        {
          asMap(buildSystemIndependentPath("a5", "x", "y", "z.pdsc"), "{ \"name\" : \"x.y.z\", \"type\" : \"record\", \"fields\" : [] }"),
          asMap(buildSystemIndependentPath("a5", "x", "y", "z.pdsc"), "{\"type\":\"record\",\"name\":\"z\",\"namespace\":\"x.y\",\"fields\":[]}"),
          asList(buildSystemIndependentPath("a5")),
          false
        },
        {
          asMap(buildSystemIndependentPath("a6", "b", "bar.pdsc"), "{ \"name\" : \"bar\", \"type\" : \"fixed\", \"size\" : 34 }"),
          asMap(buildSystemIndependentPath("a6", "b", "baz.pdsc"), "{\"type\":\"record\",\"name\":\"baz\",\"fields\":[]}"),
          asList(buildSystemIndependentPath("a6", "b")),
          false
        },
        {
          asMap(buildSystemIndependentPath("a7", "b", "c", "baz.pdsc"), "{ \"name\" : \"baz\", \"type\" : \"record\", \"fields\" : [] }"),
          asMap(buildSystemIndependentPath("a7", "b", "c", "baz.pdsc"), "{\"type\":\"record\",\"name\":\"baz\",\"fields\":[]}"),
          asList(buildSystemIndependentPath("a7", "b", "c")),
          false
        },
        {
          asMap(buildSystemIndependentPath("a8", "b", "c", "referrer1.pdsc"), "{ \"name\" : \"referrer1\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"referree\", \"type\" : \"referree1\" } ] }",
              buildSystemIndependentPath("a8", "b", "c", "referree1.pdsc"), "{ \"name\" : \"referree1\", \"type\" : \"enum\", \"symbols\" : [ \"good\", \"bad\", \"ugly\" ] }"),
          asMap(buildSystemIndependentPath("a8", "b", "c", "referrer1.pdsc"), "{\"type\":\"record\",\"name\":\"referrer1\",\"fields\":[{\"name\":\"referree\",\"type\":{\"type\":\"enum\",\"name\":\"referree1\",\"symbols\":[\"good\",\"bad\",\"ugly\"]}}]}"),
          asList(buildSystemIndependentPath("a8", "b", "c")),
          false
        },
        {
          asMap(buildSystemIndependentPath("a9", "b", "c", "referrer2.pdsc"), "{ \"name\" : \"referrer2\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"referree\", \"type\" : \"referree2\" } ] }",
              buildSystemIndependentPath("a9", "b", "c", "referree2.pdsc"), "{ \"name\" : \"referree2\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f\", \"type\" : \"string\" } ] }"),
          asMap(buildSystemIndependentPath("a9", "b", "c", "referrer2.pdsc"), "{\"type\":\"record\",\"name\":\"referrer2\",\"fields\":[{\"name\":\"referree\",\"type\":{\"type\":\"record\",\"name\":\"referree2\",\"fields\":[{\"name\":\"f\",\"type\":\"string\"}]}}]}",
              buildSystemIndependentPath("a9", "b", "c", "referree2.pdsc"), "{\"type\":\"record\",\"name\":\"referree2\",\"fields\":[{\"name\":\"f\",\"type\":\"string\"}]}"),
          asList(buildSystemIndependentPath("a9", "b", "c")),
          false
        },
        {
          asMap(buildSystemIndependentPath("a10", "b", "c", "circular1.pdsc"), "{ \"name\" : \"circular1\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"member\", \"type\" : \"circular2\" } ] }",
              buildSystemIndependentPath("a10", "b", "c", "circular2.pdsc"), "{ \"name\" : \"circular2\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"member\", \"type\" : \"circular1\" } ] }"),
          asMap(buildSystemIndependentPath("a10", "b", "c", "circular1.pdsc"), "{\"type\":\"record\",\"name\":\"circular1\",\"fields\":[{\"name\":\"member\",\"type\":{\"type\":\"record\",\"name\":\"circular2\",\"fields\":[{\"name\":\"member\",\"type\":\"circular1\"}]}}]}",
              buildSystemIndependentPath("a10", "b", "c", "circular2.pdsc"), "{\"type\":\"record\",\"name\":\"circular2\",\"fields\":[{\"name\":\"member\",\"type\":{\"type\":\"record\",\"name\":\"circular1\",\"fields\":[{\"name\":\"member\",\"type\":\"circular2\"}]}}]}"),
          asList(buildSystemIndependentPath("a10", "b", "c")),
          false
        }
      };
  }

  @Test(dataProvider = "toAvroSchemaData")
  public void testFileNameAsArgs(Map<String, String> testSchemas, Map<String, String> expectedAvroSchemas, List<String> paths, boolean override) throws IOException
  {
    Map<File, Map.Entry<String,String>>  files = TestUtil.createSchemaFiles(_testDir, testSchemas, _debug);
    // directory in path
    Collection<String> testPaths = computePathFromRelativePaths(_testDir, paths);

    // test source is a file name
    File targetDir = setup(testPaths, override);
    for (Map.Entry<File, Map.Entry<String, String>> entry : files.entrySet())
    {
      if (_debug) out.println("test file " + entry.getKey());
      String fileName = entry.getKey().getCanonicalPath();
      String args[] = { targetDir.getCanonicalPath(), fileName };
      run(args, entry, targetDir, expectedAvroSchemas);
    }
  }

  @Test(dataProvider = "toAvroSchemaData")
  public void testFullNameAsArgsWithJarInPath(Map<String, String> testSchemas, Map<String, String> expectedAvroSchemas, List<String> paths, boolean override) throws IOException
  {
    Map<File, Map.Entry<String,String>>  files = TestUtil.createSchemaFiles(_testDir, testSchemas, _debug);
    // jar files in path, create jar files
    Collection<String> testPaths = createJarsFromRelativePaths(_testDir, testSchemas, paths, _debug);

    // test source is a fully qualified name
    File targetDir = setup(testPaths, override);
    for (Map.Entry<File, Map.Entry<String, String>> entry : files.entrySet())
    {
      String schemaText = entry.getValue().getValue();
      String schemaName = schemaFullName(schemaText);
      if (_debug) out.println("test name " + schemaName);
      String args[] = { targetDir.getCanonicalPath(), schemaName };
      run(args, entry, targetDir, expectedAvroSchemas);
    }
  }

  @DataProvider
  public Object[][] toAvroSchemaDataBeforeReferree()
  {
    return new Object[][]
      {
        {
          asMap(buildSystemIndependentPath("a3", "b", "c", "referrer2.pdsc"), "{ \"name\" : \"referrer2\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"referree\", \"type\" : \"referree2\" } ] }",
              buildSystemIndependentPath("a3", "b", "c", "referree2.pdsc"), "{ \"name\" : \"referree2\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f\", \"type\" : \"string\" } ] }"),
          buildSystemIndependentPath("a3", "b", "c"),
          true
        },
        {
          asMap(buildSystemIndependentPath("a3", "b", "c", "referrer2.pdsc"), "{ \"name\" : \"referrer2\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"referree\", \"type\" : \"referree2\" } ] }",
              buildSystemIndependentPath("a3", "b", "c", "referree2.pdsc"), "{ \"name\" : \"referree2\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f\", \"type\" : \"string\" } ] }"),
          buildSystemIndependentPath("a3", "b", "c"),
          false
        }
      };
  }

  @Test(dataProvider = "toAvroSchemaDataBeforeReferree")
  public void testReferrerBeforeReferreeInArgs(Map<String, String> testSchemas, String testPath, boolean override) throws IOException
  {
    Map<File, Map.Entry<String,String>> files = TestUtil.createSchemaFiles(_testDir, testSchemas, _debug);
    Collection<String> testPaths = computePathFromRelativePaths(_testDir, Arrays.asList(testPath));
    Map.Entry<File, Map.Entry<String,String>> referrer2 = findEntryForPdsc(buildSystemIndependentPath("a3", "b", "c", "referrer2.pdsc"), files);
    Map.Entry<File, Map.Entry<String,String>> referree2 = findEntryForPdsc(buildSystemIndependentPath("a3", "b", "c", "referree2.pdsc"), files);

    File targetDir = setup(testPaths, override);
    String targetPath = targetDir.getCanonicalPath() + (override ? ("/" + AVRO_PREFIX) : "");

    File[] expectedOutputFiles =
    {
      schemaOutputFile(targetPath, referrer2.getValue().getValue()),
      schemaOutputFile(targetPath, referree2.getValue().getValue())
    };

    // make sure files do not exists
    for (File f : expectedOutputFiles)
    {
      assertFalse(f.exists());
    }

    // referrer before referree in arg list
    String args[] =
    {
      targetDir.getAbsolutePath(),
      referrer2.getKey().getCanonicalPath(),
      referree2.getKey().getCanonicalPath(),
    };
    Exception exc = null;
    try
    {
      AvroSchemaGenerator.main(args);
    }
    catch (Exception e)
    {
      exc = e;
    }
    assertNull(exc);

    // make sure expected file is generated
    for (File f : expectedOutputFiles)
    {
      assertTrue(f.exists(), f + " expected to exist");
      f.delete();
    }
  }

  @AfterMethod
  public void cleanupSchemaFiles() throws IOException
  {
    TestUtil.deleteRecursive(_testDir, _debug);
  }

  private Map.Entry<File, Map.Entry<String, String>> findEntryForPdsc(String pdsc, Map<File, Map.Entry<String,String>> files)
  {
    for (Map.Entry<File, Map.Entry<String, String>> entry : files.entrySet())
    {
      if (entry.getValue().getKey().equals(pdsc))
      {
        return entry;
      }
    }
    return null;
  }

  private String schemaFullName(String schemaText) throws IOException
  {
    DataMap dataMap = dataMapFromString(schemaText);
    String name = dataMap.getString("name");
    String namespace = dataMap.getString("namespace");
    String fullName = namespace == null ? name : namespace + "." + name;
    return fullName;
  }

  private File schemaOutputFile(String targetDir, String schemaText) throws IOException
  {
    String fullName = schemaFullName(schemaText);
    return new File(targetDir + File.separator + fullName.replace(".", File.separator) + ".avsc");
  }

  private File setup(Collection<String> paths, boolean override) throws IOException
  {
    String path = TestUtil.pathsToString(paths);
    System.setProperty(GENERATOR_RESOLVER_PATH, path);
    System.setProperty(GENERATOR_AVRO_NAMESPACE_OVERRIDE, String.valueOf(override));

    File targetDir = TestUtil.testDir("testAvroSchemaGenerator/codegen", _debug);
    ensureEmptyOutputDir(targetDir, _debug);
    return targetDir;
  }

  private void run(String[] args, Map.Entry<File, Map.Entry<String, String>> entry, File targetDir, Map<String, String> expectedAvroSchemas) throws IOException
  {
    Exception exc = null;
    try
    {
      AvroSchemaGenerator.main(args);
    }
    catch (Exception e)
    {
      exc = e;
    }
    String pdscFileName = (entry.getValue().getKey());
    if (expectedAvroSchemas.containsKey(pdscFileName))
    {
      assertNull(exc);
      File expectedOutputFile = schemaOutputFile(targetDir.getCanonicalPath(), entry.getValue().getValue());
      assertTrue(expectedOutputFile.exists());
      InputStream avroSchemaInputStream = new FileInputStream(expectedOutputFile);
      Schema avroSchema;
      try
      {
        avroSchema = parser.parse(avroSchemaInputStream);
      }
      finally
      {
        avroSchemaInputStream.close();
      }
      assertFalse(avroSchema.isError());
      String avroSchemaText = avroSchema.toString();
      if (_debug) out.println(avroSchemaText);
      assertEquals(avroSchemaText, expectedAvroSchemas.get(pdscFileName));
    }
  }
}
