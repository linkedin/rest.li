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

package com.linkedin.data.schema.generator;


import com.linkedin.data.Data;
import com.linkedin.data.DataMap;
import com.linkedin.data.TestUtil;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.util.FileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.Test;

import static com.linkedin.data.TestUtil.asMap;
import static com.linkedin.data.TestUtil.computePathFromRelativePaths;
import static com.linkedin.data.TestUtil.createJarsFromRelativePaths;
import static com.linkedin.data.TestUtil.dataMapFromString;
import static com.linkedin.data.TestUtil.ensureEmptyOutputDir;
import static com.linkedin.data.TestUtil.out;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class TestAbstractGenerator
{
  public static class TestGenerator extends AbstractGenerator
  {
    private boolean _debug = false;
    private final Config _config;

    public static void main(String[] args, boolean debug) throws IOException
    {
      if (args.length < 2)
      {
        throw new IllegalArgumentException();
      }

      run(System.getProperty(GENERATOR_RESOLVER_PATH), args[0], Arrays.copyOfRange(args, 1, args.length));
    }

    public static void run(String resolverPath, String targetDirectoryPath, String[] sources) throws IOException
    {
      final TestGenerator generator = new TestGenerator(new Config(resolverPath));

      generator.generate(targetDirectoryPath, sources);
    }

    public TestGenerator(Config config)
    {
      super();
      _config = config;
    }

    @Override
    protected Config getConfig()
    {
      return _config;
    }

    @Override
    protected void handleSchema(DataSchema schema)
    {
    }

    private void generate(String targetDirectoryPath, String sources[]) throws IOException
    {
      initSchemaResolver();

      List<File> sourceFiles = parseSources(sources);

      if (getMessage().length() > 0)
      {
        throw new IOException(getMessage().toString());
      }

      File targetDirectory = new File(targetDirectoryPath);
      List<File> targetFiles = targetFiles(targetDirectory);

      if (FileUtil.upToDate(sourceFiles, targetFiles))
      {
        if (_debug) out.println("Avro schemas are up-to-date: " + Arrays.toString(sources));
        return;
      }

      if (_debug) out.println("Generating " + targetFiles.size() + " files: " + Arrays.toString(sources));
      outputSchemas(targetDirectory);
    }

    protected List<File> targetFiles(File targetDirectory)
    {
      Collection<NamedDataSchema> schemas = getSchemaResolver().bindings().values();
      ArrayList<File> generatedFiles = new ArrayList<File>(schemas.size());
      for (DataSchema schema : schemas)
      {
        if (schema instanceof NamedDataSchema)
        {
          NamedDataSchema namedDataSchema = (NamedDataSchema) schema;
          String fullName = namedDataSchema.getFullName();
          File generatedFile = fileForAvroSchema(fullName, targetDirectory);
          generatedFiles.add(generatedFile);
        }
      }
      return generatedFiles;
    }

    protected File fileForAvroSchema(String fullName, File targetDirectory)
    {
      return new File(targetDirectory, fullName.replace('.', File.separatorChar) + ".out");
    }

    protected void outputSchemas(File targetDirectory) throws IOException
    {
      for (DataSchema schema : getSchemaResolver().bindings().values())
      {
        if (schema instanceof NamedDataSchema)
        {
          NamedDataSchema namedDataSchema = (NamedDataSchema) schema;
          String fullName = namedDataSchema.getFullName();
          File generatedFile = fileForAvroSchema(fullName, targetDirectory);
          generatedFile.getParentFile().mkdirs();
          if (_debug) out.println((generatedFile.exists() ? "exists " : "does not exist ") + generatedFile + " lastModified " + generatedFile.lastModified());
          FileOutputStream os = new FileOutputStream(generatedFile);
          os.write(schema.toString().getBytes(Data.UTF_8_CHARSET));
          os.close();
          if (_debug) out.println("generated " + generatedFile);
        }
      }
    }
  }

  Map<String,String> _testSchemas = asMap
  (
    "/a1/foo.pdsc",       "{ \"name\" : \"foo\", \"type\" : \"fixed\", \"size\" : 3 }",
    "/a1/x/y/z.pdsc",     "{ \"name\" : \"x.y.z\", \"type\" : \"enum\", \"symbols\" : [ \"X\", \"Y\", \"Z\" ], \"symbolDocs\" : { \"X\" : \"doc X\", \"Z\" : \"doc Z\" } }",
    "/a2/b/bar.pdsc",     "{ \"name\" : \"bar\", \"type\" : \"record\", \"fields\" : [] }",
    "/a3/b/c/baz.pdsc",   "{ \"name\" : \"baz\", \"type\" : \"record\", \"fields\" : [] }",
    "/a3/b/c/referrer.pdsc", "{ \"name\" : \"referrer\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"referree\", \"type\" : \"referree\" } ] }",
    "/a3/b/c/referree.pdsc", "{ \"name\" : \"referree\", \"type\" : \"enum\", \"symbols\" : [ \"good\", \"bad\", \"ugly\" ] }",
    "/a3/b/c/circular1.pdsc", "{ \"name\" : \"circular1\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"member\", \"type\" : \"circular2\" } ] }",
    "/a3/b/c/circular2.pdsc", "{ \"name\" : \"circular2\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"member\", \"type\" : \"circular1\" } ] }",
    "/a3/b/c/redefine2.pdsc", "{ \"name\" : \"redefine2\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"member\", \"type\" : { \"type\" : \"fixed\", \"name\" : \"redefine1\", \"size\" : 8 } } ] }",
    "/error/b/c/error.pdsc", "{ \"name\" : \"error\", \"type\" : \"fixed\", \"size\" : -1 }",
    "/error/b/c/redefine1.pdsc", "{ \"name\" : \"redefine1\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"member\", \"type\" : \"redefine2\" } ] }",
    "/error/b/c/enumValueDocError.pdsc", "{ \"name\" : \"enumValueDocError\", \"type\" : \"enum\", \"symbols\" : [ \"X\", \"Y\", \"Z\" ], \"symbolDocs\" : { \"X\" : \"doc X\", \"Y\" : 1 } }"
  );

  List<String> _testPaths = Arrays.asList(
    "/a1",
    "/a2/b",
    "/a3/b/c",
    "/error/b/c"
  );

  Set<String> _expectedSchemas = new HashSet<String>(Arrays.asList(
    "/a1/foo.pdsc",
    "/a1/x/y/z.pdsc",
    "/a3/b/c/baz.pdsc",
    "/a3/b/c/referrer.pdsc",
    "/a3/b/c/circular1.pdsc",
    "/a3/b/c/circular2.pdsc",
    "/a3/b/c/redefine2.pdsc"
  ));

  Map<String, String> _badPegasusSchemas = asMap(
    "/error/b/c/error.pdsc", "size must not be negative",
    "/error/b/c/redefine1.pdsc", "already defined as",
    "/error/b/c/enumValueDocError.pdsc", "symbol has an invalid documentation value"
  );

  private final static String _sourceDirName = "testAbstractGenerator/pegasus";
  private final static String _targetDirName = "testAbstractGenerator/codegen/out";

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
    return new File(targetDir + File.separator + fullName.replace(".", File.separator) + ".out");
  }

  private File setup(Collection<String> paths, boolean debug) throws IOException
  {
    String path = TestUtil.pathsToString(paths);
    System.setProperty("generator.resolver.path", path);

    File targetDir = TestUtil.testDir(_targetDirName, debug);
    ensureEmptyOutputDir(targetDir, debug);
    return targetDir;
  }

  private void testSourceChange(String arg, String[] sources, String[] targets, boolean debug) throws IOException
  {
    File sourceDir = TestUtil.testDir(_sourceDirName, false);
    File targetDir = TestUtil.testDir(_targetDirName, false);
    for (String source : sources)
    {
      File sourceFile = new File(sourceDir.getCanonicalPath() + source.replace("/", File.separator));
      assertTrue(sourceFile.exists());
      long now = System.currentTimeMillis();
      sourceFile.setLastModified(now);
      try
      {
        Thread.sleep(1000);
      }
      catch (Exception e)
      {
      }
      if (debug) out.println("touched " + sourceFile.getCanonicalPath() + " now " + now);
      String[] args = { targetDir.getCanonicalPath(), arg };
      TestGenerator.main(args, debug);
      for (String target : targets)
      {
        File targetFile = new File(targetDir + target.replace("/", File.separator));
        if (debug) out.println("check " + targetFile + " lastModified " + targetFile.lastModified());
        assertTrue(targetFile.exists());
        assertTrue(targetFile.lastModified() > sourceFile.lastModified());
      }
    }
  }

  private void verify(String[] args, Map.Entry<File, Map.Entry<String, String>> entry, Exception exc, File targetDir, boolean debug) throws IOException
  {
    if (debug)
    {
      out.println(entry);
      if (exc != null)
        out.println(exc);
    }

    String pdscFileName = (entry.getValue().getKey());
    if (_expectedSchemas.contains(pdscFileName))
    {
      File expectedOutputFile = schemaOutputFile(targetDir.getCanonicalPath(), entry.getValue().getValue());
      assertTrue(expectedOutputFile.exists());
      FileInputStream is = new FileInputStream(expectedOutputFile);
      byte[] bytes = new byte[is.available()];
      try
      {
        is.read(bytes);
      }
      finally
      {
        is.close();
      }
      String fileSchemaText = new String(bytes);
      DataSchema fileSchema = DataTemplateUtil.parseSchema(fileSchemaText);
      assertTrue(fileSchema instanceof NamedDataSchema);

      // run the generator again
      // verify that output file has not changed
      // test up-to-date
      long beforeLastModified = expectedOutputFile.lastModified();
      TestGenerator.main(args, debug);
      long afterLastModified = expectedOutputFile.lastModified();
      assertEquals(beforeLastModified, afterLastModified, expectedOutputFile.getPath());
    }
    else if (_badPegasusSchemas.containsKey(pdscFileName))
    {
      assertTrue(exc != null);
      String message = exc.getMessage();
      assertTrue(message.contains(_badPegasusSchemas.get(pdscFileName)));
    }
  }

  private void run(String[] args, Map<File, Map.Entry<String, String>> expected, File targetDir, boolean debug) throws IOException
  {
    Exception exc = null;
    try
    {
      TestGenerator.main(args, debug);
    }
    catch (Exception e)
    {
      exc = e;
    }
    for (Map.Entry<File, Map.Entry<String, String>> entry : expected.entrySet())
    {
      verify(args, entry, exc, targetDir, debug);
    }
  }

  private void run(String[] args, Map.Entry<File, Map.Entry<String, String>> entry, File targetDir, boolean debug) throws IOException
  {
    Exception exc = null;
    try
    {
      TestGenerator.main(args, debug);
    }
    catch (Exception e)
    {
      exc = e;
    }
    verify(args, entry, exc, targetDir, debug);
  }

  private void sleep(long roundMillisecs)
  {
    // make sure file last modified times for generated files is at least 1 sec later
    long t = System.currentTimeMillis();
    long remainder = roundMillisecs - (t % roundMillisecs);
    try
    {
      Thread.sleep(remainder + roundMillisecs / 10);
    }
    catch (Exception e)
    {
    }
  }

  @Test
  public void testAbstractGenerator() throws IOException
  {
    boolean debug = false;

    File testDir = TestUtil.testDir(_sourceDirName, debug);
    Map<File, Map.Entry<String,String>> files = TestUtil.createSchemaFiles(testDir, _testSchemas, debug);

    // make sure file last modified times for generated files is at least 1 sec later
    // this is required before file last modified time is second granularity,
    // if file modified time is same as source file time, it is assumed to be
    // younger than the source file and will result in files being regenerated
    // when the generator is executed a second time to make sure the second run
    // does not modify the output file.
    sleep(1000);

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

    // test sources are directories
    targetDir = setup(testPaths, debug);
    List<String> argList = new ArrayList<String>();
    argList.add(targetDir.getCanonicalPath());
    for (File f : testDir.listFiles())
    {
      if (f.getName().equals("error") == false)
      {
        argList.add(f.getCanonicalPath());
      }
    }
    Map<File, Map.Entry<String,String>> a1A2Files = new HashMap<File, Map.Entry<String, String>>();
    for (Map.Entry<File, Map.Entry<String,String>> e : files.entrySet())
    {
      String pdscFileName = e.getValue().getKey();
      if (pdscFileName.startsWith("/a1") || pdscFileName.startsWith("/a2"))
      {
        a1A2Files.put(e.getKey(), e.getValue());
      }
    }
    run(argList.toArray(new String[0]), a1A2Files, targetDir, debug);

    // test source file changes triggering re-generation of target files.
    Object[][] sourceChangeInputs =
      {
        {
          "referrer",
          new String[] {
            "/a3/b/c/referree.pdsc"
          },
          new String[] {
            "/referrer.out",
            "/referree.out"
          }
        }
      };
    for (Object[] sourceChangeInput : sourceChangeInputs)
    {
      testSourceChange((String) sourceChangeInput[0],
                       (String[]) sourceChangeInput[1],
                       (String[]) sourceChangeInput[2],
                       debug);
    }

    // jar files in path, create jar files
    testPaths = createJarsFromRelativePaths(testDir, _testSchemas, _testPaths, debug);

    // make sure file last modified times for generated files is at least 1 sec later
    // source change above changed the last modified time of the source file
    // see comments above regarding the need for sleep
    sleep(1000);

    // test source is a fully qualified name
    targetDir = setup(testPaths, debug);
    for (Map.Entry<File, Map.Entry<String, String>> entry : files.entrySet())
    {
      String schemaText = entry.getValue().getValue();
      String schemaName = schemaFullName(schemaText);
      if (debug) out.println("test name " + schemaName);
      String[] args = { targetDir.getCanonicalPath(), schemaName };
      run(args, entry, targetDir, debug);
    }

    // test source file changes triggering re-generation of target files.
    Object[][] sourceChangeInputs2 =
      {
        {
          "referrer",
          new String[] {
            "/a3/b/c.jar",
          },
          new String[] {
            "/referrer.out",
            "/referree.out"
          }
        }
      };
    for (Object[] sourceChangeInput : sourceChangeInputs2)
    {
      testSourceChange((String) sourceChangeInput[0],
                       (String[]) sourceChangeInput[1],
                       (String[]) sourceChangeInput[2],
                       debug);
    }

    // cleanup
    TestUtil.deleteRecursive(testDir, debug);
  }

}
