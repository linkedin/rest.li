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
import com.linkedin.data.avro.generator.AvroSchemaGenerator;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.avro.Schema;
import org.testng.annotations.Test;

import static com.linkedin.data.TestUtil.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class TestAvroSchemaGenerator
{
  Map<String,String> _testSchemas = asMap
  (
    "/a1/foo.pdsc",       "{ \"name\" : \"foo\", \"type\" : \"record\", \"fields\" : [] }",
    "/a1/x/y/z.pdsc",     "{ \"name\" : \"x.y.z\", \"type\" : \"record\", \"fields\" : [] }",
    "/a2/b/bar.pdsc",     "{ \"name\" : \"bar\", \"type\" : \"fixed\", \"size\" : 34 }",
    "/a3/b/c/baz.pdsc",   "{ \"name\" : \"baz\", \"type\" : \"record\", \"fields\" : [] }",
    "/a3/b/c/referrer.pdsc", "{ \"name\" : \"referrer\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"referree\", \"type\" : \"referree\" } ] }",
    "/a3/b/c/referree.pdsc", "{ \"name\" : \"referree\", \"type\" : \"enum\", \"symbols\" : [ \"good\", \"bad\", \"ugly\" ] }",
    "/a3/b/c/circular1.pdsc", "{ \"name\" : \"circular1\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"member\", \"type\" : \"circular2\" } ] }",
    "/a3/b/c/circular2.pdsc", "{ \"name\" : \"circular2\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"member\", \"type\" : \"circular1\" } ] }"
  );

  List<String> _testPaths = Arrays.asList("/a1", "/a2/b", "/a3/b/c");

  Map<String, String> _expectedAvroSchemas = asMap(
    "/a1/foo.pdsc", "{\"type\":\"record\",\"name\":\"foo\",\"fields\":[]}",
    "/a1/x/y/z.pdsc", "{\"type\":\"record\",\"name\":\"z\",\"namespace\":\"x.y\",\"fields\":[]}",
    "/a3/b/c/baz.pdsc", "{\"type\":\"record\",\"name\":\"baz\",\"fields\":[]}",
    "/a3/b/c/referrer.pdsc", "{\"type\":\"record\",\"name\":\"referrer\",\"fields\":[{\"name\":\"referree\",\"type\":{\"type\":\"enum\",\"name\":\"referree\",\"symbols\":[\"good\",\"bad\",\"ugly\"]}}]}",
    "/a3/b/c/circular1.pdsc", "{\"type\":\"record\",\"name\":\"circular1\",\"fields\":[{\"name\":\"member\",\"type\":{\"type\":\"record\",\"name\":\"circular2\",\"fields\":[{\"name\":\"member\",\"type\":\"circular1\"}]}}]}",
    "/a3/b/c/circular2.pdsc", "{\"type\":\"record\",\"name\":\"circular2\",\"fields\":[{\"name\":\"member\",\"type\":{\"type\":\"record\",\"name\":\"circular1\",\"fields\":[{\"name\":\"member\",\"type\":\"circular2\"}]}}]}"
  );

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

  private File setup(Collection<String> paths, boolean debug) throws IOException
  {
    String path = TestUtil.pathsToString(paths);
    System.setProperty("generator.resolver.path", path);

    File targetDir = TestUtil.testDir("testAvroSchemaGenerator/codegen/avro", debug);
    ensureEmptyOutputDir(targetDir, debug);
    return targetDir;
  }

  private void run(String[] args, Map.Entry<File, Map.Entry<String, String>> entry, File targetDir, boolean debug) throws IOException
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
      if (debug) out.println(avroSchemaText);
      assertEquals(avroSchemaText, _expectedAvroSchemas.get(pdscFileName));
    }
  }

  @Test
  public void testAvroSchemaGenerator() throws IOException
  {
    boolean debug = false;

    File testDir = TestUtil.testDir("testAvroSchemaGenerator/pegasus", debug);
    Map<File, Map.Entry<String,String>> files = TestUtil.createSchemaFiles(testDir, _testSchemas, debug);

    // directory in path
    Collection<String> testPaths = computePathFromRelativePaths(testDir, _testPaths);

    // test source is a file name
    File targetDir = setup(testPaths, debug);
    for (Map.Entry<File, Map.Entry<String, String>> entry : files.entrySet())
    {
      if (debug) out.println("test file " + entry.getKey());
      String fileName = entry.getKey().getCanonicalPath();
      String args[] = { targetDir.getCanonicalPath(), fileName };
      run(args, entry, targetDir, debug);
    }

    // jar files in path, create jar files
    testPaths = createJarsFromRelativePaths(testDir, _testSchemas, _testPaths, debug);

    // test source is a fully qualified name
    targetDir = setup(testPaths, debug);
    for (Map.Entry<File, Map.Entry<String, String>> entry : files.entrySet())
    {
      String schemaText = entry.getValue().getValue();
      String schemaName = schemaFullName(schemaText);
      if (debug) out.println("test name " + schemaName);
      String args[] = { targetDir.getCanonicalPath(), schemaName };
      run(args, entry, targetDir, debug);
    }

    // cleanup
    TestUtil.deleteRecursive(testDir, debug);
  }

}
