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
import org.apache.avro.Schema;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.linkedin.data.TestUtil.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class TestAvroSchemaGenerator
{
  private boolean _debug = false;

  private Map<String,String> _testSchemas = asMap
  (
    "/a1/foo.pdsc",       "{ \"name\" : \"foo\", \"type\" : \"record\", \"fields\" : [] }",
    "/a1/x/y/z.pdsc",     "{ \"name\" : \"x.y.z\", \"type\" : \"record\", \"fields\" : [] }",
    "/a2/b/bar.pdsc",     "{ \"name\" : \"bar\", \"type\" : \"fixed\", \"size\" : 34 }",
    "/a3/b/c/baz.pdsc",   "{ \"name\" : \"baz\", \"type\" : \"record\", \"fields\" : [] }",
    "/a3/b/c/referrer1.pdsc", "{ \"name\" : \"referrer1\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"referree\", \"type\" : \"referree1\" } ] }",
    "/a3/b/c/referree1.pdsc", "{ \"name\" : \"referree1\", \"type\" : \"enum\", \"symbols\" : [ \"good\", \"bad\", \"ugly\" ] }",
    "/a3/b/c/referrer2.pdsc", "{ \"name\" : \"referrer2\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"referree\", \"type\" : \"referree2\" } ] }",
    "/a3/b/c/referree2.pdsc", "{ \"name\" : \"referree2\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f\", \"type\" : \"string\" } ] }",
    "/a3/b/c/circular1.pdsc", "{ \"name\" : \"circular1\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"member\", \"type\" : \"circular2\" } ] }",
    "/a3/b/c/circular2.pdsc", "{ \"name\" : \"circular2\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"member\", \"type\" : \"circular1\" } ] }"
  );

  private List<String> _testPaths = Arrays.asList("/a1", "/a2/b", "/a3/b/c");

  private Map<String, String> _expectedAvroSchemas = asMap(
    "/a1/foo.pdsc", "{\"type\":\"record\",\"name\":\"foo\",\"fields\":[]}",
    "/a1/x/y/z.pdsc", "{\"type\":\"record\",\"name\":\"z\",\"namespace\":\"x.y\",\"fields\":[]}",
    "/a3/b/c/baz.pdsc", "{\"type\":\"record\",\"name\":\"baz\",\"fields\":[]}",
    "/a3/b/c/referrer1.pdsc", "{\"type\":\"record\",\"name\":\"referrer1\",\"fields\":[{\"name\":\"referree\",\"type\":{\"type\":\"enum\",\"name\":\"referree1\",\"symbols\":[\"good\",\"bad\",\"ugly\"]}}]}",
    "/a3/b/c/referrer2.pdsc", "{\"type\":\"record\",\"name\":\"referrer2\",\"fields\":[{\"name\":\"referree\",\"type\":{\"type\":\"record\",\"name\":\"referree2\",\"fields\":[{\"name\":\"f\",\"type\":\"string\"}]}}]}",
    "/a3/b/c/referree2.pdsc", "{\"type\":\"record\",\"name\":\"referree2\",\"fields\":[{\"name\":\"f\",\"type\":\"string\"}]}",
    "/a3/b/c/circular1.pdsc", "{\"type\":\"record\",\"name\":\"circular1\",\"fields\":[{\"name\":\"member\",\"type\":{\"type\":\"record\",\"name\":\"circular2\",\"fields\":[{\"name\":\"member\",\"type\":\"circular1\"}]}}]}",
    "/a3/b/c/circular2.pdsc", "{\"type\":\"record\",\"name\":\"circular2\",\"fields\":[{\"name\":\"member\",\"type\":{\"type\":\"record\",\"name\":\"circular1\",\"fields\":[{\"name\":\"member\",\"type\":\"circular2\"}]}}]}"
  );

  private File _testDir;
  private Map<File, Map.Entry<String,String>> _files;

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

  private File setup(Collection<String> paths) throws IOException
  {
    String path = TestUtil.pathsToString(paths);
    System.setProperty("generator.resolver.path", path);

    File targetDir = TestUtil.testDir("testAvroSchemaGenerator/codegen/avro", _debug);
    ensureEmptyOutputDir(targetDir, _debug);
    return targetDir;
  }

  private void run(String[] args, Map.Entry<File, Map.Entry<String, String>> entry, File targetDir) throws IOException
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
    if (_expectedAvroSchemas.containsKey(pdscFileName))
    {
      assertNull(exc);
      File expectedOutputFile = schemaOutputFile(targetDir.getCanonicalPath(), entry.getValue().getValue());
      assertTrue(expectedOutputFile.exists());
      Schema avroSchema = Schema.parse(expectedOutputFile);
      assertFalse(avroSchema.isError());
      String avroSchemaText = avroSchema.toString();
      if (_debug) out.println(avroSchemaText);
      assertEquals(avroSchemaText, _expectedAvroSchemas.get(pdscFileName));
    }
  }

  @BeforeClass
  public void setupSchemaFiles() throws IOException
  {
    _testDir = TestUtil.testDir("testAvroSchemaGenerator/pegasus", _debug);
    _files = TestUtil.createSchemaFiles(_testDir, _testSchemas, _debug);
  }

  @Test
  public void testFileNameAsArgs() throws IOException
  {
    // directory in path
    Collection<String> testPaths = computePathFromRelativePaths(_testDir, _testPaths);

    // test source is a file name
    File targetDir = setup(testPaths);
    for (Map.Entry<File, Map.Entry<String, String>> entry : _files.entrySet())
    {
      if (_debug) out.println("test file " + entry.getKey());
      String fileName = entry.getKey().getCanonicalPath();
      String args[] = { targetDir.getCanonicalPath(), fileName };
      run(args, entry, targetDir);
    }
  }

  @Test
  public void testFullNameAsArgsWithJarInPath() throws IOException
  {
    // jar files in path, create jar files
    Collection<String> testPaths = createJarsFromRelativePaths(_testDir, _testSchemas, _testPaths, _debug);

    // test source is a fully qualified name
    File targetDir = setup(testPaths);
    for (Map.Entry<File, Map.Entry<String, String>> entry : _files.entrySet())
    {
      String schemaText = entry.getValue().getValue();
      String schemaName = schemaFullName(schemaText);
      if (_debug) out.println("test name " + schemaName);
      String args[] = { targetDir.getCanonicalPath(), schemaName };
      run(args, entry, targetDir);
    }
  }

  @Test
  public void testReferrerBeforeReferreeInArgs() throws IOException
  {
    Collection<String> testPaths = computePathFromRelativePaths(_testDir, _testPaths);
    Map.Entry<File, Map.Entry<String,String>> referrer2 = findEntryForPdsc("/a3/b/c/referrer2.pdsc", _files);
    Map.Entry<File, Map.Entry<String,String>> referree2 = findEntryForPdsc("/a3/b/c/referree2.pdsc", _files);

    File targetDir = setup(testPaths);

    File[] expectedOutputFiles =
      {
        schemaOutputFile(targetDir.getCanonicalPath(), referrer2.getValue().getValue()),
        schemaOutputFile(targetDir.getCanonicalPath(), referree2.getValue().getValue())
      };

    // make sure files do not exists
    for (File f : expectedOutputFiles)
    {
      assertFalse(f.exists());
    }

    // referrer before referree in arg list
    String args[] = {
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

    // no other file generated
    assertEquals(targetDir.listFiles().length, 0);
  }

  @AfterClass
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

}
