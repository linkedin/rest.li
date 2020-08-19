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
import com.linkedin.data.schema.DataSchemaParserFactory;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.PegasusSchemaParser;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.SchemaFormatType;
import com.linkedin.data.schema.SchemaParser;
import com.linkedin.data.schema.SchemaParserFactory;
import com.linkedin.data.schema.grammar.PdlSchemaParser;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.RecordTemplate;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import org.mockito.Mockito;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.data.TestUtil.asMap;
import static com.linkedin.data.TestUtil.out;
import static com.linkedin.util.FileUtil.buildSystemIndependentPath;
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
  static final SchemaFormatType PDL = SchemaFormatType.PDL;
  static final SchemaFormatType PDSC = SchemaFormatType.PDSC;

  @BeforeTest
  public void setup()
  {
  }

  public static File buildTempJar(Map<String, String> fileEntries) throws IOException
  {
    // Write a temp JAR file using the entries defined above
    File tempJar = File.createTempFile(TestDataSchemaResolver.class.getCanonicalName(), ".jar");
    tempJar.deleteOnExit();
    JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(tempJar));
    for (Map.Entry<String, String> entry : fileEntries.entrySet())
    {
      jarOutputStream.putNextEntry(new ZipEntry(entry.getKey()));
      jarOutputStream.write(entry.getValue().getBytes(Charset.defaultCharset()));
      jarOutputStream.closeEntry();
    }
    jarOutputStream.finish();
    jarOutputStream.close();
    return tempJar;
  }

  public static class MapDataSchemaResolver extends AbstractDataSchemaResolver
  {
    public MapDataSchemaResolver(DataSchemaParserFactory parserFactory, List<String> paths, Map<String, String> map)
    {
      super(parserFactory);
      _paths = paths;
      _extension = "." + parserFactory.getLanguageExtension();
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
    buildSystemIndependentPath("a1"),
    buildSystemIndependentPath("a2", "b"),
    buildSystemIndependentPath("a3", "b", "c")
  );

  Map<String,String> _testSchemas = asMap
  (
    buildSystemIndependentPath("a1", "foo.pdsc"),       "{ \"name\" : \"foo\", \"type\" : \"fixed\", \"size\" : 4 }",
    buildSystemIndependentPath("a1", "x", "y", "z.pdsc"),     "{ \"name\" : \"x.y.z\", \"type\" : \"fixed\", \"size\" : 7 }",
    buildSystemIndependentPath("a2", "b", "bar.pdsc"),     "{ \"name\" : \"bar\", \"type\" : \"fixed\", \"size\" : 5 }",
    buildSystemIndependentPath("a3", "b", "c", "baz.pdsc"),   "{ \"name\" : \"baz\", \"type\" : \"fixed\", \"size\" : 6 }",
    buildSystemIndependentPath("a3", "b", "c", "error.pdsc"), "{ \"name\" : \"error\", \"type\" : \"fixed\", \"size\" : -1 }",
    buildSystemIndependentPath("a3", "b", "c", "referrer.pdsc"), "{ \"name\" : \"referrer\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"referree\", \"type\" : \"referree\" } ] }",
    buildSystemIndependentPath("a3", "b", "c", "referree.pdsc"), "{ \"name\" : \"referree\", \"type\" : \"enum\", \"symbols\" : [ \"good\", \"bad\", \"ugly\" ] }",
    buildSystemIndependentPath("a3", "b", "c", "circular1.pdsc"), "{ \"name\" : \"circular1\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"member\", \"type\" : \"circular2\" } ] }",
    buildSystemIndependentPath("a3", "b", "c", "circular2.pdsc"), "{ \"name\" : \"circular2\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"member\", \"type\" : \"circular1\" } ] }",
    buildSystemIndependentPath("a3", "b", "c", "redefine1.pdsc"), "{ \"name\" : \"redefine1\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"member\", \"type\" : \"redefine2\" } ] }",
    buildSystemIndependentPath("a3", "b", "c", "redefine2.pdsc"), "{ \"name\" : \"redefine2\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"member\", \"type\" : { \"type\" : \"fixed\", \"name\" : \"redefine1\", \"size\" : 8 } } ] }"
  );

  String[][] _testLookupAndExpectedResults = {
      {
        "referrer",
        FOUND,
        "\"name\" : \"referrer\"",
        buildSystemIndependentPath("referrer.pdsc").toString()
      },
      {
        "x.y.z",
        FOUND,
        "\"size\" : 7",
        buildSystemIndependentPath("x", "y", "z.pdsc").toString()
      },
      {
        "foo",
        FOUND,
        "\"size\" : 4",
        buildSystemIndependentPath("foo.pdsc").toString()
      },
      {
        "bar",
        FOUND,
        "\"size\" : 5",
        buildSystemIndependentPath("bar.pdsc").toString()
      },
      {
        "baz",
        FOUND,
        "\"size\" : 6",
        buildSystemIndependentPath("baz.pdsc").toString()
      },
      {
        "circular1",
        FOUND,
        "\"name\" : \"circular1\"",
        buildSystemIndependentPath("circular1.pdsc").toString()
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

    DataSchemaResolver resolver = new MapDataSchemaResolver(SchemaParserFactory.instance(), _testPaths, _testSchemas);
    lookup(resolver, _testLookupAndExpectedResults, File.separatorChar, debug);
  }

  @DataProvider
  public Object[][] circularReferenceData()
  {
    return new Object[][]
    {
        {
          "Two records including each other",
            PDSC,
            asMap(buildSystemIndependentPath("a1", "include1.pdsc"), "{ \"name\" : \"include1\", \"type\" : \"record\", \"include\": [\"include2\"], \"fields\" : [ { \"name\" : \"member1\", \"type\" : \"string\" } ] }",
                  buildSystemIndependentPath("a1", "include2.pdsc"), "{ \"name\" : \"include2\", \"type\" : \"record\", \"include\": [\"include1\"], \"fields\" : [ { \"name\" : \"member2\", \"type\" : \"string\" } ] }"
            ),
            new String[][]
            {
                {
                  "include1",
                    ERROR,
                    "circular reference involving includes"
                },
                { "include2",
                    ERROR,
                    "circular reference involving includes"
                }
            }
        },
        {
            "Two records including each other",
            PDL,
            asMap(buildSystemIndependentPath("a1", "include1.pdl"), "record include1 includes include2 {\n member1: string\n}",
                  buildSystemIndependentPath("a1", "include2.pdl"), "record include2 includes include1 {\n member2: string\n }"
                 ),
            new String[][]
                {
                    {
                        "include1",
                        ERROR,
                        "circular reference involving includes"
                    },
                    { "include2",
                        ERROR,
                        "circular reference involving includes"
                    }
                }
        },
        {
            "Two records including each other using aliases",
            PDSC,
            asMap(buildSystemIndependentPath("a1", "include1.pdsc"), "{ \"name\" : \"include1\", \"aliases\" : [\"includeAlias1\"], \"type\" : \"record\", \"include\": [\"include2\"], \"fields\" : [ { \"name\" : \"member1\", \"type\" : \"string\" } ] }",
                buildSystemIndependentPath("a1", "include2.pdsc"), "{ \"name\" : \"include2\", \"type\" : \"record\", \"include\": [\"includeAlias1\"], \"fields\" : [ { \"name\" : \"member2\", \"type\" : \"string\" } ] }"
            ),
            new String[][]
                {
                    {
                        "include1",
                        ERROR,
                        "circular reference involving includes"
                    }
                }
        },
        {
            "Two records including each other using aliases",
            PDL,
            asMap(buildSystemIndependentPath("a1", "include1.pdl"), "@aliases = [\"includeAlias1\"] record include1 includes include2 { member1: string }",
                  buildSystemIndependentPath("a1", "include2.pdl"), "record include2 includes includeAlias1 {member2: string}"),
            new String[][]
                {
                    {
                        "include1",
                        ERROR,
                        "circular reference involving includes"
                    }
                }
        },
        {
            "First record has a record field, and that record includes the first record",
            PDSC,
            asMap(buildSystemIndependentPath("a1", "record1.pdsc"), "{ \"name\" : \"record1\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"member1\", \"type\" : \"include1\" } ] }",
                buildSystemIndependentPath("a1", "include1.pdsc"), "{ \"name\" : \"include1\", \"type\" : \"record\", \"include\": [\"record1\"], \"fields\" : [ { \"name\" : \"member2\", \"type\" : \"string\" } ] }"
            ),
            new String[][]
            {
                {
                    "record1",
                    ERROR,
                    "circular reference involving includes"
                },
                {
                    "include1",
                    ERROR,
                    "circular reference involving includes"
                }
            }
        },
        {
            "First record has a record field, and that record includes the first record",
            PDL,
            asMap(buildSystemIndependentPath("a1", "record1.pdl"), "record record1 { member1: include1 }",
                  buildSystemIndependentPath("a1", "include1.pdl"), "record include1 includes record1 { member2: string } "
                 ),
            new String[][]
                {
                    {
                        "record1",
                        ERROR,
                        "circular reference involving includes"
                    },
                    {
                        "include1",
                        ERROR,
                        "circular reference involving includes"
                    }
                }
        },
        {
            "Circular reference involving only fields",
            PDSC,
            asMap(buildSystemIndependentPath("a1", "record1.pdsc"), "{ \"name\" : \"record1\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"member1\", \"type\" : \"record2\" } ] }",
                buildSystemIndependentPath("a1", "record2.pdsc"), "{ \"name\" : \"record2\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"member2\", \"type\" : \"record1\" } ] }"
            ),
            new String[][]
            {
                {
                    "record1",
                    FOUND,
                    "\"name\" : \"record1\"",
                    buildSystemIndependentPath("a1", "record1.pdsc")
                },
                {
                    "record2",
                    FOUND,
                    "\"name\" : \"record2\"",
                    buildSystemIndependentPath("a1", "record2.pdsc")
                }
            }
        },
        {
            "Circular reference involving only fields",
            PDL,
            asMap(buildSystemIndependentPath("a1", "record1.pdl"), "record record1 { member1: record2 }",
                  buildSystemIndependentPath("a1", "record2.pdl"), "record record2 { member2: record1 }"
                 ),
            new String[][]
                {
                    {
                        "record1",
                        FOUND,
                        "\"name\" : \"record1\"",
                        buildSystemIndependentPath("a1", "record1.pdl")
                    },
                    {
                        "record2",
                        FOUND,
                        "\"name\" : \"record2\"",
                        buildSystemIndependentPath("a1", "record2.pdl")
                    }
                }
        },
        {
            "Three records with one include in the cycle",
            PDSC,
            asMap(buildSystemIndependentPath("a1", "include1.pdsc"), "{ \"name\" : \"include1\", \"type\" : \"record\", \"include\": [\"record1\"], \"fields\" : [ { \"name\" : \"member2\", \"type\" : \"string\" } ] }",
                buildSystemIndependentPath("a1", "record1.pdsc"), "{ \"name\" : \"record1\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"member1\", \"type\" : \"record2\" } ] }",
                buildSystemIndependentPath("a1", "record2.pdsc"), "{ \"name\" : \"record2\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"member1\", \"type\" : \"include1\" } ] }"
            ),
            new String[][]
                {
                    {
                        "include1",
                        ERROR,
                        "circular reference involving includes"
                    },
                    {
                        "record1",
                        ERROR,
                        "circular reference involving includes"
                    },
                    {
                        "record2",
                        ERROR,
                        "circular reference involving includes"
                    }
                }
        },

        {
            "Three records with one include in the cycle",
            PDL,
            asMap(buildSystemIndependentPath("a1", "include1.pdl"), "record include1 includes record1 { member2: string }",
                  buildSystemIndependentPath("a1", "record1.pdl"), "record record1 { member1: record2 }",
                  buildSystemIndependentPath("a1", "record2.pdl"), "record record2 { member1: include1 }"
                 ),
            new String[][]
                {
                    {
                        "include1",
                        ERROR,
                        "circular reference involving includes"
                    },
                    {
                        "record1",
                        ERROR,
                        "circular reference involving includes"
                    },
                    {
                        "record2",
                        ERROR,
                        "circular reference involving includes"
                    }
                }
        },
        {
            "Three records with one include not in the cycle",
            PDSC,
            asMap(buildSystemIndependentPath("a1", "include1.pdsc"), "{ \"name\" : \"include1\", \"type\" : \"record\", \"include\": [\"record1\"], \"fields\" : [ { \"name\" : \"member2\", \"type\" : \"string\" } ] }",
                buildSystemIndependentPath("a1", "record1.pdsc"), "{ \"name\" : \"record1\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"member1\", \"type\" : \"record2\" } ] }",
                buildSystemIndependentPath("a1", "record2.pdsc"), "{ \"name\" : \"record2\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"member1\", \"type\" : \"record1\" } ] }"
            ),
            new String[][]
                {
                    {
                        "include1",
                        FOUND,
                        "\"name\" : \"include1\"",
                        buildSystemIndependentPath("a1", "include1.pdsc")
                    },
                    {
                        "record1",
                        FOUND,
                        "\"name\" : \"record1\"",
                        buildSystemIndependentPath("a1", "record1.pdsc")
                    },
                    {
                        "record2",
                        FOUND,
                        "\"name\" : \"record2\"",
                        buildSystemIndependentPath("a1", "record2.pdsc")
                    }
                }
        },
        {
            "Three records with one include not in the cycle",
            PDL,
            asMap(buildSystemIndependentPath("a1", "include1.pdl"), "record include1 includes record1 { member2: string }",
                  buildSystemIndependentPath("a1", "record1.pdl"), "record record1 { member1: record2 }",
                  buildSystemIndependentPath("a1", "record2.pdl"), "record record2 { member1: record1 }"
                 ),
            new String[][]
                {
                    {
                        "include1",
                        FOUND,
                        "\"name\" : \"include1\"",
                        buildSystemIndependentPath("a1", "include1.pdl")
                    },
                    {
                        "record1",
                        FOUND,
                        "\"name\" : \"record1\"",
                        buildSystemIndependentPath("a1", "record1.pdl")
                    },
                    {
                        "record2",
                        FOUND,
                        "\"name\" : \"record2\"",
                        buildSystemIndependentPath("a1", "record2.pdl")
                    }
                }
        },
        {
            "Self including record",
            PDSC,
            asMap(buildSystemIndependentPath("a1", "include.pdsc"), "{ \"name\" : \"include\", \"type\" : \"record\", \"include\": [\"include\"], \"fields\" : [ { \"name\" : \"member2\", \"type\" : \"string\" } ] }"
            ),
            new String[][]
                {
                    {
                        "include",
                        ERROR,
                        "circular reference involving includes"
                    }
                }
        },
        {
            "Self including record",
            PDL,
            asMap(buildSystemIndependentPath("a1", "include.pdl"), "record include includes include { member2: string }"
                 ),
            new String[][]
                {
                    {
                        "include",
                        ERROR,
                        "circular reference involving includes"
                    }
                }
        },
        {
            "Circular reference involving include, typeref and a record",
            PDSC,
            asMap(buildSystemIndependentPath("a1", "include1.pdsc"), "{ \"name\" : \"include1\", \"type\" : \"record\", \"include\": [\"record1\"], \"fields\" : [ { \"name\" : \"member2\", \"type\" : \"string\" } ] }",
                buildSystemIndependentPath("a1", "record1.pdsc"), "{ \"name\" : \"record1\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"member1\", \"type\" : \"typeref1\" } ] }",
                buildSystemIndependentPath("a1", "typeref1.pdsc"), "{ \"name\" : \"typeref1\", \"type\" : \"typeref\", \"ref\" : \"include1\" }"
            ),
            new String[][]
                {
                    {
                        "include1",
                        ERROR,
                        "circular reference involving includes"
                    },
                    {
                        "typeref1",
                        ERROR,
                        "circular reference involving includes"
                    },
                    {
                        "record1",
                        ERROR,
                        "circular reference involving includes"
                    }
                }
        },
        {
            "Circular reference involving include, typeref and a record",
            PDL,
            asMap(buildSystemIndependentPath("a1", "include1.pdl"), "record include1 includes record1 { member2: string }",
                  buildSystemIndependentPath("a1", "record1.pdl"), "record record1 { member1: typeref1 }",
                  buildSystemIndependentPath("a1", "typeref1.pdl"), "typeref typeref1 = include1"
                 ),
            new String[][]
                {
                    {
                        "include1",
                        ERROR,
                        "circular reference involving includes"
                    },
                    {
                        "typeref1",
                        ERROR,
                        "circular reference involving includes"
                    },
                    {
                        "record1",
                        ERROR,
                        "circular reference involving includes"
                    }
                }
        },
        {
            "Circular reference involving typerefs",
            PDSC,
            asMap(buildSystemIndependentPath("a1", "typeref1.pdsc"), "{ \"name\" : \"typeref1\", \"type\" : \"typeref\", \"ref\": \"typeref2\" }",
                buildSystemIndependentPath("a1", "typeref2.pdsc"), "{ \"name\" : \"typeref2\", \"type\" : \"typeref\", \"ref\" : \"typeref3\" }",
                buildSystemIndependentPath("a1", "typeref3.pdsc"), "{ \"name\" : \"typeref3\", \"type\" : \"typeref\", \"ref\" : { \"type\" : \"array\", \"items\" : \"typeref1\" } }"
            ),
            new String[][]
                {
                    {
                        "typeref1",
                        ERROR,
                        "typeref has a circular reference to itself"
                    },
                    {
                        "typeref2",
                        ERROR,
                        "typeref has a circular reference to itself"
                    },
                    {
                        "typeref3",
                        ERROR,
                        "typeref has a circular reference to itself"
                    }
                }
        },
        {
            "Circular reference involving typerefs",
            PDL,
            asMap(buildSystemIndependentPath("a1", "typeref1.pdl"), "typeref typeref1 = typeref2",
                  buildSystemIndependentPath("a1", "typeref2.pdl"), "typeref typeref2 = typeref3",
                  buildSystemIndependentPath("a1", "typeref3.pdl"), "typeref typeref3 = array[typeref1]"
                 ),
            new String[][]
                {
                    {
                        "typeref1",
                        ERROR,
                        "typeref has a circular reference to itself"
                    },
                    {
                        "typeref2",
                        ERROR,
                        "typeref has a circular reference to itself"
                    },
                    {
                        "typeref3",
                        ERROR,
                        "typeref has a circular reference to itself"
                    }
                }
        },
        {
            "Circular reference involving typerefs, with a record outside cycle",
            PDSC,
            asMap(buildSystemIndependentPath("a1", "record1.pdsc"), "{ \"name\" : \"record1\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"member1\", \"type\" : \"typeref1\" } ] }",
                buildSystemIndependentPath("a1", "typeref1.pdsc"), "{ \"name\" : \"typeref1\", \"type\" : \"typeref\", \"ref\" : \"typeref2\" }",
                buildSystemIndependentPath("a1", "typeref2.pdsc"), "{ \"name\" : \"typeref2\", \"type\" : \"typeref\", \"ref\" : \"typeref1\" }"
            ),
            new String[][]
                {
                    {
                        "record1",
                        ERROR,
                        "typeref has a circular reference to itself"
                    },
                    {
                        "typeref1",
                        ERROR,
                        "typeref has a circular reference to itself"
                    },
                    {
                        "typeref2",
                        ERROR,
                        "typeref has a circular reference to itself"
                    }
                }
        },

        {
            "Circular reference involving typerefs, with a record outside cycle",
            PDL,
            asMap(buildSystemIndependentPath("a1", "record1.pdl"), "record record1 { member1: typeref1 }",
                  buildSystemIndependentPath("a1", "typeref1.pdl"), "typeref typeref1 = typeref2",
                  buildSystemIndependentPath("a1", "typeref2.pdl"), "typeref typeref2 = typeref1"
                 ),
            new String[][]
                {
                    {
                        "record1",
                        ERROR,
                        "typeref has a circular reference to itself"
                    },
                    {
                        "typeref1",
                        ERROR,
                        "typeref has a circular reference to itself"
                    },
                    {
                        "typeref2",
                        ERROR,
                        "typeref has a circular reference to itself"
                    }
                }
        },
        {
            "Circular reference involving typerefs, using alias",
            PDSC,
            asMap(buildSystemIndependentPath("a1", "typeref1.pdsc"), "{ \"name\" : \"typeref1\", \"aliases\" : [\"typerefAlias1\"], \"type\" : \"typeref\", \"ref\" : \"typeref2\" }",
                buildSystemIndependentPath("a1", "typeref2.pdsc"), "{ \"name\" : \"typeref2\", \"type\" : \"typeref\", \"ref\" : \"typerefAlias1\" }"
            ),
            new String[][]
                {
                    {
                        "typeref1",
                        ERROR,
                        "typeref has a circular reference to itself"
                    }
                }
        },
        {
            "Circular reference involving typerefs, using alias",
            PDL,
            asMap(buildSystemIndependentPath("a1", "typeref1.pdl"), "@aliases = [\"typerefAlias1\"] typeref typeref1 = typeref2",
                  buildSystemIndependentPath("a1", "typeref2.pdl"), "typeref typeref2 = typerefAlias1"
                 ),
            new String[][]
                {
                    {
                        "typeref1",
                        ERROR,
                        "typeref has a circular reference to itself"
                    }
                }
        }
    };
  }

  @Test(dataProvider = "circularReferenceData")
  public void testCircularReferences(String desc, SchemaFormatType extension, Map<String, String> testSchemas, String[][] testLookupAndExpectedResults)
  {
    boolean debug = false;

    for (String[] testLookupAndExpectedResult : testLookupAndExpectedResults)
    {
      DataSchemaResolver schemaResolver =
          new MapDataSchemaResolver(extension.getSchemaParserFactory(), Arrays.asList(buildSystemIndependentPath("a1")),
                                    testSchemas);
      lookup(schemaResolver, new String[][]{testLookupAndExpectedResult}, File.separatorChar, debug, extension);
    }
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
    @SuppressWarnings("deprecation")
    final ClassNameDataSchemaResolver resolver = new ClassNameDataSchemaResolver();
    final PegasusSchemaParser parser = new SchemaParser(resolver);

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

  @Test
  public void testClasspathResourceDataSchemaResolver()
  {
    final ClasspathResourceDataSchemaResolver resolver = new ClasspathResourceDataSchemaResolver();
    final PegasusSchemaParser parser = new SchemaParser(resolver);

    final List<String> existingSchemas = new ArrayList<>();
    Collections.addAll(existingSchemas, "com.linkedin.data.schema.ValidationDemo",
        "com.linkedin.restli.example.Album",
        "com.linkedin.restli.example.FruitsPdl",
        "com.linkedin.data.schema.RecordWithPdlReference");
    final String nonExistSchemaName = "Non-Existing Schema";

    for (String existingSchemaName : existingSchemas)
    {
      final DataSchema existSchema = parser.lookupName(existingSchemaName);
      assertNotNull(existSchema, "Failed parsing : " + existingSchemaName);
      assertEquals(((NamedDataSchema) existSchema).getFullName(), existingSchemaName);
    }

    final DataSchema nonExistSchema = parser.lookupName(nonExistSchemaName);
    assertNull(nonExistSchema);
  }

  @Test
  public void testAddBadLocation()
  {
    MapDataSchemaResolver resolver = new MapDataSchemaResolver(SchemaParserFactory.instance(), _testPaths, _testSchemas);
    JarFile jarFile = Mockito.mock(JarFile.class);
    Mockito.when(jarFile.getName()).thenReturn("jarFile");
    InJarFileDataSchemaLocation location = new InJarFileDataSchemaLocation(jarFile, "samplePath");
    InJarFileDataSchemaLocation otherLocation = new InJarFileDataSchemaLocation(jarFile, "otherPath");
    resolver.addBadLocation(location);
    assertTrue(resolver.isBadLocation(location));
    assertTrue(resolver.isBadLocation(location.getLightweightRepresentation()));
    assertFalse(resolver.isBadLocation(otherLocation));
    assertFalse(resolver.isBadLocation(otherLocation.getLightweightRepresentation()));
  }

  public void lookup(DataSchemaResolver resolver, String[][] lookups, char separator, boolean debug)
  {
    lookup(resolver, lookups, separator, debug, PDSC);
  }

  public void lookup(DataSchemaResolver resolver, String[][] lookups, char separator, boolean debug, SchemaFormatType extension)
  {
    PegasusSchemaParser parser = extension.equals(PDSC) ? new SchemaParser(resolver): new PdlSchemaParser(resolver);

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
        assertTrue(expected == null || errorMessage.contains(expected), "Expected: " + expected +"\n Actual: " + errorMessage);
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
        DataSchemaLocation namedSchemalocation = resolver.nameToDataSchemaLocations().get(name);
        String locationNorm;
        if (namedSchemalocation.toString().contains(".jar"))
        {
          locationNorm = location.replace(separator, '/');
        }
        else
        {
          locationNorm = location.replace('/', separator);
        }
        assertNotNull(namedSchemalocation);
        assertEquals(namedSchemalocation.toString().indexOf(locationNorm),
                     namedSchemalocation.toString().length() - locationNorm.length());
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
