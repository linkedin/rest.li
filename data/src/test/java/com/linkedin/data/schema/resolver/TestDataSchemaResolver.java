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

package com.linkedin.data.schema.resolver;


import com.linkedin.data.Data;
import com.linkedin.data.DataMap;
import com.linkedin.data.TestUtil;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaLocation;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.SchemaParser;
import com.linkedin.data.schema.SchemaParserFactory;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.RecordTemplate;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static com.linkedin.data.TestUtil.asMap;
import static com.linkedin.data.TestUtil.out;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertSame;


public class TestDataSchemaResolver
{
  static final String ERROR = "error";
  static final String FOUND = "found";
  static final String NOT_FOUND = "not found";

  @BeforeTest
  public void setup()
  {
  }

  public static class MapDataSchemaResolver extends AbstractDataSchemaResolver
  {
    public MapDataSchemaResolver(SchemaParserFactory parserFactory,
                                 List<String> paths, String extension, Map<String, String> map)
    {
      super(parserFactory);
      _paths = paths;
      _extension = extension;
      _map = map;
    }

    @Override
    protected Iterator<DataSchemaLocation> possibleLocations(String name)
    {
      final String transformedName = name.replace('.', File.separatorChar) + _extension;

      return new AbstractIterator(_paths)
      {
        @Override
        protected DataSchemaLocation transform(String path)
        {
          return new MapResolverLocation(path + File.separator + transformedName);
        }
      };
    }

    @Override
    protected InputStream locationToInputStream(DataSchemaLocation location,
                                                StringBuilder errorMessageBuilder)
    {
      String input = _map.get(((MapResolverLocation) location).getMapKey());
      return input == null ? null : new ByteArrayInputStream(input.getBytes(Data.UTF_8_CHARSET));
    }

    public static class MapResolverLocation implements DataSchemaLocation
    {
      private final String _mapKey;

      public MapResolverLocation(String mapKey)
      {
        _mapKey = mapKey;
      }

      public String getMapKey()
      {
        return _mapKey;
      }

      @Override
      public String toString()
      {
        return _mapKey;
      }

      @Override
      public boolean equals(Object o)
      {
        return _mapKey.equals(((MapResolverLocation)o)._mapKey);
      }

      @Override
      public int hashCode()
      {
        return _mapKey.hashCode();
      }

      @Override
      public File getSourceFile()
      {
        return null;
      }
    }

    private List<String> _paths;
    private String _extension;
    private Map<String,String> _map;
  };

  List<String> _testPaths = Arrays.asList
  (
   "/a1",
   "/a2/b",
   "/a3/b/c"
  );

  Map<String,String> _testSchemas = asMap
  (
   "/a1/foo.pdsc",       "{ \"name\" : \"foo\", \"type\" : \"fixed\", \"size\" : 4 }",
   "/a1/x/y/z.pdsc",     "{ \"name\" : \"x.y.z\", \"type\" : \"fixed\", \"size\" : 7 }",
   "/a2/b/bar.pdsc",     "{ \"name\" : \"bar\", \"type\" : \"fixed\", \"size\" : 5 }",
   "/a3/b/c/baz.pdsc",   "{ \"name\" : \"baz\", \"type\" : \"fixed\", \"size\" : 6 }",
   "/a3/b/c/error.pdsc", "{ \"name\" : \"error\", \"type\" : \"fixed\", \"size\" : -1 }",
   "/a3/b/c/referrer.pdsc", "{ \"name\" : \"referrer\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"referree\", \"type\" : \"referree\" } ] }",
   "/a3/b/c/referree.pdsc", "{ \"name\" : \"referree\", \"type\" : \"enum\", \"symbols\" : [ \"good\", \"bad\", \"ugly\" ] }",
   "/a3/b/c/circular1.pdsc", "{ \"name\" : \"circular1\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"member\", \"type\" : \"circular2\" } ] }",
   "/a3/b/c/circular2.pdsc", "{ \"name\" : \"circular2\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"member\", \"type\" : \"circular1\" } ] }",
   "/a3/b/c/redefine1.pdsc", "{ \"name\" : \"redefine1\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"member\", \"type\" : \"redefine2\" } ] }",
   "/a3/b/c/redefine2.pdsc", "{ \"name\" : \"redefine2\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"member\", \"type\" : { \"type\" : \"fixed\", \"name\" : \"redefine1\", \"size\" : 8 } } ] }"
  );

  String[][] _testLookupAndExpectedResults = {
      {
        "referrer",
        FOUND,
        "\"name\" : \"referrer\"",
        "/referrer.pdsc"
      },
      {
        "x.y.z",
        FOUND,
        "\"size\" : 7",
        "/x/y/z.pdsc"
      },
      {
        "foo",
        FOUND,
        "\"size\" : 4",
        "/foo.pdsc"
      },
      {
        "bar",
        FOUND,
        "\"size\" : 5",
        "/bar.pdsc"
      },
      {
        "baz",
        FOUND,
        "\"size\" : 6",
        "/baz.pdsc"
      },
      {
        "circular1",
        FOUND,
        "\"name\" : \"circular1\"",
        "/circular1.pdsc"
      },
      {
        "apple",
        NOT_FOUND,
        null
      },
      {
        "error",
        ERROR,
        "is negative"
      },
      {
        "redefine1",
        ERROR,
        "already defined as"
      },
  };

  @Test
  public void testMapDataSchemaResolver()
  {
    boolean debug = false;

    DataSchemaResolver resolver = new MapDataSchemaResolver(SchemaParserFactory.instance(), _testPaths, ".pdsc", _testSchemas);
    lookup(resolver, _testLookupAndExpectedResults, '/', debug);
  }

  @Test
  public void testFileDataSchemaResolver() throws IOException
  {
    boolean debug = false;

    File testDir = TestUtil.testDir("testFileDataSchemaResolver", debug);
    Map<File, Map.Entry<String,String>> files = TestUtil.createSchemaFiles(testDir, _testSchemas, debug);

    List<String> testPaths = new ArrayList<String>();
    for (String testPath : _testPaths)
    {
      String dirname = (testDir.getCanonicalPath() + "/" + testPath).replace('/', File.separatorChar);
      testPaths.add((new File(dirname)).getCanonicalPath());
    }

    FileDataSchemaResolver resolver = new FileDataSchemaResolver(SchemaParserFactory.instance());
    resolver.setPaths(testPaths);
    resolver.setExtension(".pdsc");

    lookup(resolver, _testLookupAndExpectedResults, File.separatorChar, debug);

    // create jar files
    testPaths.clear();
    for (String testPath : _testPaths)
    {
      String jarFileName = (testDir.getCanonicalPath() + testPath + ".jar").replace('/', File.separatorChar);
      Map<String,String> jarFileContents = new HashMap<String, String>();
      for (Map.Entry<String,String> entry : _testSchemas.entrySet())
      {
        if (entry.getKey().startsWith(testPath))
        {
          String key = entry.getKey();
          jarFileContents.put(key.substring(testPath.length()), entry.getValue());
        }
      }
      TestUtil.createSchemaJar(jarFileName, jarFileContents, debug);
      testPaths.add(jarFileName);
    }

    FileDataSchemaResolver resolver2 = new FileDataSchemaResolver(SchemaParserFactory.instance());
    resolver2.setPaths(testPaths);
    resolver2.setExtension(".pdsc");

    lookup(resolver2, _testLookupAndExpectedResults, File.separatorChar, debug);

    // cleanup
    TestUtil.deleteRecursive(testDir, debug);
  }

  public static class ClassNameFooRecord extends RecordTemplate
  {
    public static final RecordDataSchema SCHEMA = (RecordDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"record\", \"name\" : \"ClassNameFooRecord\", \"namespace\" : \"com.linkedin.data.schema.resolver.TestDataSchemaResolver\", \"fields\" : [ { \"name\" : \"foo\", \"type\" : \"string\" } ] }");

    public ClassNameFooRecord()
    {
      super(new DataMap(), SCHEMA);
    }
  }

  @Test
  public void testClassNameDataSchemaResolver()
  {
    final ClassNameDataSchemaResolver resolver = new ClassNameDataSchemaResolver();
    final SchemaParser parser = new SchemaParser(resolver);

    final Class<? extends RecordTemplate> testClass = ClassNameFooRecord.class;
    final String nonExistSchemaName = "Non-Existing Schema";

    final DataSchema existSchema = parser.lookupName(testClass.getName());
    assertNotNull(existSchema);
    assertTrue(existSchema instanceof RecordDataSchema);
    assertEquals(((RecordDataSchema) existSchema).getFullName(), testClass.getCanonicalName());
    assertFalse(resolver.isBadLocation(new ClassNameDataSchemaLocation(testClass.getName())));

    final DataSchema nonExistSchema = parser.lookupName(nonExistSchemaName);
    assertNull(nonExistSchema);
    assertTrue(parser.errorMessage().contains(nonExistSchemaName));
    assertTrue(resolver.isBadLocation(new ClassNameDataSchemaLocation(nonExistSchemaName)));
  }

  public void lookup(DataSchemaResolver resolver, String[][] lookups, char separator, boolean debug)
  {
    SchemaParser parser = new SchemaParser(resolver);

    for (String[] entry : lookups)
    {
      String name = entry[0];
      String expectFound = entry[1];
      String expected = entry[2];
      DataSchema schema = parser.lookupName(name);
      if (debug) { out.println("----" + name + "-----"); }
      String errorMessage = parser.errorMessage();
      if (debug && errorMessage.isEmpty() == false) { out.println(errorMessage); }
      if (expectFound == ERROR)
      {
        assertTrue(parser.hasError());
        assertTrue(expected == null || errorMessage.contains(expected));
      }
      else if (expectFound == FOUND)
      {
        assertTrue(schema != null);
        String schemaText = schema.toString();
        if (debug) { out.println(schemaText); }
        assertFalse(parser.hasError());
        assertTrue(schema instanceof NamedDataSchema);
        NamedDataSchema namedSchema = (NamedDataSchema) schema;
        assertEquals(namedSchema.getFullName(), name);
        assertTrue(schemaText.contains(expected));
        assertTrue(resolver.bindings().containsKey(name));
        assertSame(resolver.bindings().get(name), namedSchema);
        String location = entry[3];
        String locationNorm = location.replace('/', separator);
        DataSchemaLocation namedSchemalocation = resolver.nameToDataSchemaLocations().get(name);
        assertNotNull(namedSchemalocation);
        assertEquals(namedSchemalocation.toString().indexOf(locationNorm), namedSchemalocation.toString().length() - locationNorm.length());
        assertTrue(resolver.locationResolved(namedSchemalocation));
      }
      else if (expectFound == NOT_FOUND)
      {
        assertTrue(schema == null);
        assertFalse(parser.hasError());
        assertTrue(expected == null || errorMessage.contains(expected));
        assertFalse(resolver.bindings().containsKey(name));
      }
      else
      {
        assertTrue(false);
      }
    }
  }
}
