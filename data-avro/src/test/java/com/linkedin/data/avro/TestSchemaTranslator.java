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

package com.linkedin.data.avro;


import com.linkedin.data.TestUtil;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.JsonBuilder;
import com.linkedin.data.schema.SchemaParser;
import com.linkedin.data.schema.SchemaToJsonEncoder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


public class TestSchemaTranslator
{
  static public GenericRecord genericRecordFromString(String jsonString, Schema writerSchema, Schema readerSchema) throws IOException
  {
    GenericDatumReader<GenericRecord> reader = new GenericDatumReader<GenericRecord>(writerSchema, readerSchema);
    byte[] bytes = jsonString.getBytes("UTF-8");
    Decoder binaryDecoder = DecoderFactory.defaultFactory().createBinaryDecoder(bytes, null);
    GenericRecord record = reader.read(null, binaryDecoder);
    return record;
  }

  @Test
  public void testTranslateDefaultBackwardsCompatibility()
  {
    DataToAvroSchemaTranslationOptions options = new DataToAvroSchemaTranslationOptions();
    assertSame(options.getOptionalDefaultMode(), OptionalDefaultMode.TRANSLATE_DEFAULT);
    assertSame(options.getPretty(), JsonBuilder.Pretty.COMPACT);

    assertSame(DataToAvroSchemaTranslationOptions.DEFAULT_OPTIONAL_DEFAULT_MODE, OptionalDefaultMode.TRANSLATE_DEFAULT);
  }

  @Test
  public void testToAvroSchema() throws IOException
  {
    boolean debug = false;

    final String emptyFooSchema = "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ ] }";
    final String emptyFooValue = "{}";

    final OptionalDefaultMode allModes[] = { OptionalDefaultMode.TRANSLATE_DEFAULT, OptionalDefaultMode.TRANSLATE_TO_NULL };
    final OptionalDefaultMode translateDefault[] = { OptionalDefaultMode.TRANSLATE_DEFAULT };
    final OptionalDefaultMode translateToNull[] = { OptionalDefaultMode.TRANSLATE_TO_NULL };

    Object[][] inputs =
    {
      // {
      //   1st element is the Pegasus schema in JSON.
      //   Each following element is an Object array,
      //     1st element of this array is an array of OptionalDefaultMode's to be used for default translation.
      //     2nd element is either a string or an Exception.
      //       If it is a string, it is the expected output Avro schema in JSON.
      //         If there are 3rd and 4th elements, then the 3rd element is an Avro schema used to write the 4th element
      //         which is JSON serialized Avro data. Usually, this is used to make sure that the translated default
      //         value is valid for Avro. Avro does not validate the default value in the schema. It will only
      //         de-serialize (and validate) the default value when it is actually used. The writer schema and
      //         the JSON serialized Avro data should not include fields with default values.
      //       If it is an Exception, then the Pegasus schema cannot be translated and this is the exception that
      //         is expected. The 3rd element is a string that should be contained in the message of the exception.
      // }
      {
        // custom properties :
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ], \"version\" : 1 }",
        new Object[] {
          allModes,
           "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ], \"version\" : 1 }"
        }
      },
      {
        // required, optional not specified
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ] }",
        new Object[] {
          allModes,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ] }"
        }
      },
      {
        // required and has default
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\", \"default\" : 42 } ] }",
        new Object[] {
          allModes,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\", \"default\" : 42 } ] }",
          emptyFooSchema,
          emptyFooValue
        }
      },
      {
        // required, optional is false
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\", \"optional\" : false } ] }",
        new Object[] {
          allModes,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ] }"
        }
      },
      {
        // required, optional is false and has default
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\", \"default\" : 42, \"optional\" : false } ] }",
        new Object[] {
          allModes,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\", \"default\" : 42 } ] }",
          emptyFooSchema,
          emptyFooValue
        }
      },
      {
        // optional is true
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\", \"optional\" : true } ] }",
        new Object[] {
          allModes,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\" ], \"default\" : null } ] }",
          emptyFooSchema,
          emptyFooValue
        }
      },
      {
        // optional and has default
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\", \"optional\" : true, \"default\" : 42 } ] }",
        new Object [] {
          translateDefault,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"int\", \"null\" ], \"default\" : 42 } ] }",
          emptyFooSchema,
          emptyFooValue
        },
        new Object [] {
          translateToNull,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\" ], \"default\" : null } ] }",
          emptyFooSchema,
          emptyFooValue
        },
      },
      {
        // optional and has default, enum type
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"enum\", \"name\" : \"fruits\", \"symbols\" : [ \"APPLE\", \"ORANGE\" ] }, \"optional\" : true, \"default\" : \"APPLE\" } ] }",
        new Object [] {
          translateDefault,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ { \"type\" : \"enum\", \"name\" : \"fruits\", \"symbols\" : [ \"APPLE\", \"ORANGE\" ] }, \"null\" ], \"default\" : \"APPLE\" } ] }",
          emptyFooSchema,
          emptyFooValue
        },
        new Object [] {
          translateToNull,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", { \"type\" : \"enum\", \"name\" : \"fruits\", \"symbols\" : [ \"APPLE\", \"ORANGE\" ] } ], \"default\" : null } ] }",
          emptyFooSchema,
          emptyFooValue
        },
      },
      {
        // optional and has default with namespaced type
        "{ \"type\" : \"record\", \"name\" : \"a.b.foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"record\", \"name\" : \"b.c.bar\", \"fields\" : [ ] }, \"default\" : {  }, \"optional\" : true } ] }",
        new Object[] {
          translateDefault,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"namespace\" : \"a.b\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ { \"type\" : \"record\", \"name\" : \"bar\", \"namespace\" : \"b.c\", \"fields\" : [  ] }, \"null\" ], \"default\" : {  } } ] }",
          "{ \"type\" : \"record\", \"name\" : \"a.b.foo\", \"fields\" : [ ] }",
          emptyFooValue
        },
        new Object[] {
          translateToNull,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"namespace\" : \"a.b\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", { \"type\" : \"record\", \"name\" : \"bar\", \"namespace\" : \"b.c\", \"fields\" : [  ] } ], \"default\" : null } ] }",
          "{ \"type\" : \"record\", \"name\" : \"a.b.foo\", \"fields\" : [ ] }",
          emptyFooValue
        },
      },
      {
        // optional and has default value with multi-level nesting
        "{ \"type\" : \"record\", \"name\" : \"a.b.foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"record\", \"name\" : \"b.c.bar\", \"fields\" : [ { \"name\" : \"baz\", \"type\" : { \"type\" : \"record\", \"name\" : \"c.d.baz\", \"fields\" : [ ] } } ] }, \"default\" : { \"baz\" : { } }, \"optional\" : true } ] }",
        new Object[] {
          translateDefault,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"namespace\" : \"a.b\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ { \"type\" : \"record\", \"name\" : \"bar\", \"namespace\" : \"b.c\", \"fields\" : [ { \"name\" : \"baz\", \"type\" : { \"type\" : \"record\", \"name\" : \"baz\", \"namespace\" : \"c.d\", \"fields\" : [  ] } } ] }, \"null\" ], \"default\" : { \"baz\" : {  } } } ] }",
          "{ \"type\" : \"record\", \"name\" : \"a.b.foo\", \"fields\" : [ ] }",
          emptyFooValue
        },
        new Object[] {
          translateToNull,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"namespace\" : \"a.b\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", { \"type\" : \"record\", \"name\" : \"bar\", \"namespace\" : \"b.c\", \"fields\" : [ { \"name\" : \"baz\", \"type\" : { \"type\" : \"record\", \"name\" : \"baz\", \"namespace\" : \"c.d\", \"fields\" : [  ] } } ] } ], \"default\" : null } ] }",
          "{ \"type\" : \"record\", \"name\" : \"a.b.foo\", \"fields\" : [ ] }",
          emptyFooValue
        },
      },
      {
        // optional and has default but with circular references with inconsistent defaults, inconsistent because optional field has default, and also missing (which requires default to be null)
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"foo\", \"default\" : {  }, \"optional\" : true } ] }",
        new Object[] {
          translateDefault,
          IllegalArgumentException.class,
          "cannot translate absent optional field (to have null value) because this field is optional and has a default value"
        },
        new Object[] {
          translateToNull,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"foo\" ], \"default\" : null } ] }",
          emptyFooSchema,
          emptyFooValue
        },
      },
      {
        // optional and has default but with circular references with inconsistent defaults, inconsistent because optional field has default, and also missing (which requires default to be null)
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"foo\", \"default\" : { \"bar\" : {  } }, \"optional\" : true } ] }",
        new Object[] {
          translateDefault,
          IllegalArgumentException.class,
          "cannot translate absent optional field (to have null value) because this field is optional and has a default value"
        },
        new Object[] {
          translateToNull,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"foo\" ], \"default\" : null } ] }",
          "cannot translate absent optional field (to have null value) because this field is optional and has a default value"
        },
      },
      {
        // required union without null
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"int\", \"string\" ] } ] }",
        new Object[] {
          allModes,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"int\", \"string\" ] } ] }"
        }
      },
      {
        // required union with null
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"string\" ] } ] }",
        new Object[] {
          allModes,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"string\" ] } ] }"
        }
      },
      {
        // optional union without null
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"int\", \"string\" ], \"optional\" : true } ] }",
        new Object[] {
          allModes,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\", \"string\" ], \"default\" : null } ] }",
          emptyFooSchema,
          emptyFooValue
        }
      },
      {
        // optional union with null
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\", \"string\" ], \"optional\" : true } ] }",
        new Object[] {
          allModes,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\", \"string\" ], \"default\" : null } ] }",
          emptyFooSchema,
          emptyFooValue
        }
      },
      {
        // optional union without null and default is 1st member type
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"int\", \"string\" ], \"default\" : { \"int\" : 42 }, \"optional\" : true } ] }",
        new Object[] {
          translateDefault,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"int\", \"string\", \"null\" ], \"default\" : 42 } ] }",
          emptyFooSchema,
          emptyFooValue
        },
        new Object[] {
          translateToNull,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\", \"string\" ], \"default\" : null } ] }",
          emptyFooSchema,
          emptyFooValue
        }
      },
      {
        // optional union without null and default is 2nd member type
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"int\", \"string\" ], \"default\" : { \"string\" : \"abc\" }, \"optional\" : true } ] }",
        new Object[] {
          translateDefault,
          IllegalArgumentException.class,
          "cannot translate union value"
        },
        new Object[] {
          translateToNull,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\", \"string\" ], \"default\" : null } ] }",
          emptyFooSchema,
          emptyFooValue
        },
      },
      {
        // optional union with null and non-null default, default is 1st member type
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"int\", \"null\", \"string\" ], \"default\" : { \"int\" : 42 }, \"optional\" : true } ] }",
        new Object[] {
          translateDefault,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"int\", \"null\", \"string\" ], \"default\" : 42 } ] }",
          emptyFooSchema,
          emptyFooValue
        },
        new Object[] {
          translateToNull,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\", \"string\" ], \"default\" : null } ] }",
          emptyFooSchema,
          emptyFooValue
        }
      },
      {
        // optional union with null and non-null default, default is 2nd member type
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"int\", \"null\", \"string\" ], \"default\" : null, \"optional\" : true } ] }",
        new Object[] {
          translateDefault,
          IllegalArgumentException.class,
          "cannot translate union value"
        },
        new Object[] {
          translateToNull,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\", \"string\" ], \"default\" : null } ] }",
          emptyFooSchema,
          emptyFooValue
        },
      },
      {
        // optional union with null and non-null default, default is 3rd member type
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"int\", \"null\", \"string\" ], \"default\" : { \"string\" : \"abc\" }, \"optional\" : true } ] }",
        new Object[] {
          translateDefault,
          IllegalArgumentException.class,
          "cannot translate union value"
        },
        new Object[] {
          translateToNull,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\", \"string\" ], \"default\" : null } ] }",
          emptyFooSchema,
          emptyFooValue
        },
      },
      {
        // optional union with null and null default, default is 1st member type
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\", \"string\" ], \"default\" : null, \"optional\" : true } ] }",
        new Object[] {
          allModes,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\", \"string\" ], \"default\" : null } ] }",
          emptyFooSchema,
          emptyFooValue
        },
      },
      {
        // optional union but with circular references with inconsistent defaults, inconsistent because optional field has default, and also missing (which requires default to be null)
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"foo\", \"string\" ], \"default\" : { \"foo\" : { } }, \"optional\" : true } ] }",
        new Object[] {
          translateDefault,
          IllegalArgumentException.class,
          "cannot translate absent optional field (to have null value) or field with non-null union value because this field is optional and has a non-null default value",
        },
        new Object[] {
          translateToNull,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"foo\", \"string\" ], \"default\" : null } ] }",
          emptyFooSchema,
          emptyFooValue
        },
      },
      {
        // optional union but with circular references with but with consistent defaults (the only default that works is null for circularly referenced unions)
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"foo\" ], \"default\" : null, \"optional\" : true } ] }",
        new Object[] {
          allModes,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"foo\" ], \"default\" : null } ] }",
          emptyFooSchema,
          emptyFooValue
        }
      },
      {
        // typeref in record field
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"typeref\", \"name\" : \"IntRef\", \"ref\" : \"int\" } } ] }",
        new Object[] {
          allModes,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ] }"
        }
      },
      {
        // typeref in record field, optional
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"typeref\", \"name\" : \"IntRef\", \"ref\" : \"int\" }, \"optional\" : true } ] }",
        new Object[] {
          allModes,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\" ], \"default\" : null } ] }",
          emptyFooSchema,
          emptyFooValue
        }
      },
      {
        // typeref in record field, optional and has default value
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"typeref\", \"name\" : \"IntRef\", \"ref\" : \"int\" }, \"optional\" : true, \"default\" : 42 } ] }",
        new Object[] {
          translateDefault,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"int\", \"null\" ], \"default\" : 42 } ] }",
          emptyFooSchema,
          emptyFooValue
        },
        new Object[] {
          translateToNull,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\" ], \"default\" : null } ] }",
          emptyFooSchema,
          emptyFooValue
        }
      },
      {
        // typeref in array
        "{ \"type\" : \"array\", \"items\" : { \"type\" : \"typeref\", \"name\" : \"IntRef\", \"ref\" : \"int\" } }",
        new Object[] {
          allModes,
          "{ \"type\" : \"array\", \"items\" : \"int\" }"
        }
      },
      {
        // typeref in map
        "{ \"type\" : \"map\", \"values\" : { \"type\" : \"typeref\", \"name\" : \"IntRef\", \"ref\" : \"int\" } }",
        new Object[] {
          allModes,
          "{ \"type\" : \"map\", \"values\" : \"int\" }"
        }
      },
      {
        // typeref in union
        "[ \"null\", { \"type\" : \"typeref\", \"name\" : \"IntRef\", \"ref\" : \"int\" } ]",
        new Object[] {
          allModes,
          "[ \"null\", \"int\" ]"
        }
      },
      {
        // record field with union with typeref, without null in record field
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"string\", { \"type\" : \"typeref\", \"name\" : \"IntRef\", \"ref\" : \"int\" } ] } ] }",
        new Object[] {
          allModes,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"string\", \"int\" ] } ] }"
        }
      },
      {
        // record field with union with typeref, without null and default is 1st member type and not typeref-ed
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"string\", { \"type\" : \"typeref\", \"name\" : \"IntRef\", \"ref\" : \"int\" } ], \"default\" : { \"string\" : \"abc\" } } ] }",
        new Object[] {
          allModes,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"string\", \"int\" ], \"default\" : \"abc\" } ] }",
          emptyFooSchema,
          emptyFooValue
        }
      },
      {
        // record field with union with typeref, without null and default is 1st member type and typeref-ed
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ { \"type\" : \"typeref\", \"name\" : \"IntRef\", \"ref\" : \"int\" }, \"string\" ], \"default\" : { \"int\" : 42 } } ] }",
        new Object[] {
          allModes,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"int\", \"string\" ], \"default\" : 42 } ] }",
          emptyFooSchema,
          emptyFooValue
        }
      },
      {
        // record field with union with typeref, without null and default is 2nd member type and not typeref-ed
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ { \"type\" : \"typeref\", \"name\" : \"IntRef\", \"ref\" : \"int\" }, \"string\" ], \"default\" : { \"string\" : \"abc\" } } ] }",
        new Object[] {
          allModes,
          IllegalArgumentException.class,
          "cannot translate union value"
        }
      },
      {
        // record field with union with typeref, without null and default is 2nd member type and typeref-ed
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"string\", { \"type\" : \"typeref\", \"name\" : \"IntRef\", \"ref\" : \"int\" } ], \"default\" : { \"int\" : 42 } } ] }",
        new Object[] {
          allModes,
          IllegalArgumentException.class,
          "cannot translate union value"
        }
      },
      {
        // record field with union with typeref, without null and optional
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"string\", { \"type\" : \"typeref\", \"name\" : \"IntRef\", \"ref\" : \"int\" } ], \"optional\" : true } ] }",
        new Object[] {
          allModes,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"string\", \"int\" ], \"default\" : null } ] }",
          emptyFooSchema,
          emptyFooValue
        }
      },
      {
        // record field with union with typeref, without null and optional, default is 1st member and not typeref-ed
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"string\", { \"type\" : \"typeref\", \"name\" : \"IntRef\", \"ref\" : \"int\" } ], \"optional\" : true, \"default\" : { \"string\" : \"abc\" } } ] }",
        new Object[] {
          translateDefault,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"string\", \"int\", \"null\" ], \"default\" : \"abc\" } ] }",
          emptyFooSchema,
          emptyFooValue
        },
        new Object[] {
          translateToNull,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"string\", \"int\" ], \"default\" : null } ] }",
          emptyFooSchema,
          emptyFooValue
        },
      },
      {
        // record field with union with typeref, without null and optional, default is 1st member and typeref-ed
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ { \"type\" : \"typeref\", \"name\" : \"IntRef\", \"ref\" : \"int\" }, \"string\" ], \"optional\" : true, \"default\" : { \"int\" : 42 } } ] }",
        new Object[] {
          translateDefault,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"int\", \"string\", \"null\" ], \"default\" : 42 } ] }",
          emptyFooSchema,
          emptyFooValue
        },
        new Object[] {
          translateToNull,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\", \"string\" ], \"default\" : null } ] }",
          emptyFooSchema,
          emptyFooValue
        }
      },
      {
        // record field with union with typeref, without null and optional, default is 2nd member and not typeref-ed
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ { \"type\" : \"typeref\", \"name\" : \"IntRef\", \"ref\" : \"int\" }, \"string\" ], \"optional\" : true, \"default\" : { \"string\" : \"abc\" } } ] }",
        new Object[] {
          translateDefault,
          IllegalArgumentException.class,
          "cannot translate union value"
        },
        new Object[] {
          translateToNull,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\", \"string\" ], \"default\" : null } ] }",
          emptyFooSchema,
          emptyFooValue
        }
      },
      {
        // record field with union with typeref, without null and optional, default is 2nd member and typeref-ed
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"string\", { \"type\" : \"typeref\", \"name\" : \"IntRef\", \"ref\" : \"int\" } ], \"optional\" : true, \"default\" : { \"int\" : 42 } } ] }",
        new Object[] {
          translateDefault,
          IllegalArgumentException.class,
          "cannot translate union value"
        },
        new Object[] {
          translateToNull,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"string\", \"int\" ], \"default\" : null } ] }",
          emptyFooSchema,
          emptyFooValue
        }
      },
      {
        // record field with union with typeref, with null 1st member
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", { \"type\" : \"typeref\", \"name\" : \"IntRef\", \"ref\" : \"int\" } ] } ] }",
        new Object[] {
          allModes,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\" ] } ] }"
        }
      },
      {
        // record field with union with typeref, with null 1st member, default is 1st member and null
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", { \"type\" : \"typeref\", \"name\" : \"IntRef\", \"ref\" : \"int\" } ], \"default\" : null } ] }",
        new Object[] {
          allModes,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\" ], \"default\" : null } ] }"
        }
      },
      {
        // record field with union with typeref, with null 1st member, default is last member and typeref-ed
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", { \"type\" : \"typeref\", \"name\" : \"IntRef\", \"ref\" : \"int\" } ], \"default\" : { \"int\" : 42 } } ] }",
        new Object[] {
          allModes,
          IllegalArgumentException.class,
          "cannot translate union value"
        }
      },
      {
        // record field with union with typeref with null 1st member, and optional
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", { \"type\" : \"typeref\", \"name\" : \"IntRef\", \"ref\" : \"int\" } ], \"optional\" : true } ] }",
        new Object[] {
          allModes,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\" ], \"default\" : null } ] }",
          emptyFooSchema,
          emptyFooValue
        }
      },
      {
        // record field with union with typeref with null 1st member, and optional, default is 1st member and null
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", { \"type\" : \"typeref\", \"name\" : \"IntRef\", \"ref\" : \"int\" } ], \"optional\" : true, \"default\" : null } ] }",
        new Object[] {
          allModes,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\" ], \"default\" : null } ] }",
          emptyFooSchema,
          emptyFooValue
        }
      },
      {
        // record field with union with typeref with null 1st member, and optional, default is last member and typeref-ed
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", { \"type\" : \"typeref\", \"name\" : \"IntRef\", \"ref\" : \"int\" } ], \"optional\" : true, \"default\" : { \"int\" : 42 } } ] }",
        new Object[] {
          translateDefault,
          IllegalArgumentException.class,
          "cannot translate union value"
        },
        new Object[] {
          translateToNull,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\" ], \"default\" : null } ] }",
          emptyFooSchema,
          emptyFooValue
        }
      },
      {
        // record field with union with typeref, with null last member
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ { \"type\" : \"typeref\", \"name\" : \"IntRef\", \"ref\" : \"int\" }, \"null\" ] } ] }",
        new Object[] {
          allModes,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"int\", \"null\" ] } ] }"
        }
      },
      {
        // record field with union with typeref, with null last member, default is 1st member and typeref-ed
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ { \"type\" : \"typeref\", \"name\" : \"IntRef\", \"ref\" : \"int\" }, \"null\" ], \"default\" : { \"int\" : 42 } } ] }",
        new Object[] {
          allModes,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"int\", \"null\" ], \"default\" : 42 } ] }",
          emptyFooSchema,
          emptyFooValue
        }
      },
      {
        // record field with union with typeref, with null last member, default is last member and null
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ { \"type\" : \"typeref\", \"name\" : \"IntRef\", \"ref\" : \"int\" }, \"null\" ], \"default\" : null } ] }",
        new Object[] {
          allModes,
          IllegalArgumentException.class,
          "cannot translate union value"
        }
      },
      {
        // record field with union with typeref, with null last member, and optional
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ { \"type\" : \"typeref\", \"name\" : \"IntRef\", \"ref\" : \"int\" }, \"null\" ], \"optional\" : true } ] }",
        new Object[] {
          allModes,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\" ], \"default\" : null } ] }",
          emptyFooSchema,
          emptyFooValue
        }
      },
      {
        // record field with union with typeref, with null last member, and optional, default is 1st member and typeref-ed
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ { \"type\" : \"typeref\", \"name\" : \"IntRef\", \"ref\" : \"int\" }, \"null\" ], \"optional\" : true, \"default\" : { \"int\" : 42 } } ] }",
        new Object[] {
          translateDefault,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"int\", \"null\" ], \"default\" : 42 } ] }",
          emptyFooSchema,
          emptyFooValue
        },
        new Object[] {
          translateToNull,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\" ], \"default\" : null } ] }",
          emptyFooSchema,
          emptyFooValue
        }
      },
      {
        // record field with union with typeref, with null last member, and optional, default is last member and null
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ { \"type\" : \"typeref\", \"name\" : \"IntRef\", \"ref\" : \"int\" }, \"null\" ], \"optional\" : true, \"default\" : null } ] }",
        new Object[] {
          translateDefault,
          IllegalArgumentException.class,
           "cannot translate union value"
        },
        new Object[] {
          translateToNull,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\" ], \"default\" : null } ] }",
          emptyFooSchema,
          emptyFooValue
        }
      },
      {
        // array of union with no default
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"array\", \"items\" : [ \"int\", \"string\" ] } } ] }",
        new Object[] {
          allModes,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"array\", \"items\" : [ \"int\", \"string\" ] } } ] }"
        }
      },
      {
        // array of union with default, default value uses only 1st member type
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"array\", \"items\" : [ \"int\", \"string\" ] }, \"default\" : [ { \"int\" : 42 }, { \"int\" : 13 } ] } ] }",
        new Object[] {
          allModes,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"array\", \"items\" : [ \"int\", \"string\" ] }, \"default\" : [ 42, 13 ] } ] }",
          emptyFooSchema,
          emptyFooValue
        }
      },
      {
        // array of union with default, default value uses only 1st null member type
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"array\", \"items\" : [ \"null\", \"string\" ] }, \"default\" : [ null, null ] } ] }",
        new Object[] {
          allModes,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"array\", \"items\" : [ \"null\", \"string\" ] }, \"default\" : [ null, null ] } ] }",
          emptyFooSchema,
          emptyFooValue
        }
      },
      {
        // array of union with default, default value uses 2nd member type
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"array\", \"items\" : [ \"int\", \"string\" ] }, \"default\" : [ { \"int\" : 42 }, { \"string\" : \"abc\" } ] } ] }",
        new Object[] {
          allModes,
          IllegalArgumentException.class,
          "cannot translate union value"
        }
      },
      {
        // optional array of union with no default
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"array\", \"items\" : [ \"int\", \"string\" ] }, \"optional\" : true } ] }",
        new Object[] {
          allModes,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", { \"type\" : \"array\", \"items\" : [ \"int\", \"string\" ] } ], \"default\" : null } ] }",
          emptyFooSchema,
          emptyFooValue
        }
      },
      {
        // optional array of union with default, default value uses only 1st member type
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"array\", \"items\" : [ \"int\", \"string\" ] }, \"optional\" : true, \"default\" : [ { \"int\" : 42 }, { \"int\" : 13 } ] } ] }",
        new Object[] {
          translateDefault,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ { \"type\" : \"array\", \"items\" : [ \"int\", \"string\" ] }, \"null\" ], \"default\" : [ 42, 13 ] } ] }",
          emptyFooSchema,
          emptyFooValue
        },
        new Object[] {
          translateToNull,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", { \"type\" : \"array\", \"items\" : [ \"int\", \"string\" ] } ], \"default\" : null } ] }",
          emptyFooSchema,
          emptyFooValue
        }
      },
      {
        // optional array of union with default, default value uses 2nd member type
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"array\", \"items\" : [ \"int\", \"string\" ] }, \"optional\" : true, \"default\" : [ { \"int\" : 42 }, { \"string\" : \"abc\" } ] } ] }",
        new Object[] {
          translateDefault,
          IllegalArgumentException.class,
          "cannot translate union value"
        },
        new Object[] {
          translateToNull,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", { \"type\" : \"array\", \"items\" : [ \"int\", \"string\" ] } ], \"default\" : null } ] }",
          emptyFooSchema,
          emptyFooValue
        }
      },
      {
        // map of union with no default
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"values\" : [ \"int\", \"string\" ] } } ] }",
        new Object[] {
          allModes,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"values\" : [ \"int\", \"string\" ] } } ] }",
        }
      },
      {
        // map of union with default, default value uses only 1st member type
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"values\" : [ \"int\", \"string\" ] }, \"default\" : { \"m1\" : { \"int\" : 42 } } } ] }",
        new Object[] {
          allModes,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"values\" : [ \"int\", \"string\" ] }, \"default\" : { \"m1\" : 42 } } ] }",
          emptyFooSchema,
          emptyFooValue
        }
      },
      {
        // map of union with default, default value uses only 1st null member type
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"values\" : [ \"null\", \"string\" ] }, \"default\" : { \"m1\" : null } } ] }",
        new Object[] {
          allModes,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"values\" : [ \"null\", \"string\" ] }, \"default\" : { \"m1\" : null } } ] }",
          emptyFooSchema,
          emptyFooValue
        }
      },
      {
        // map of union with default, default value uses 2nd member type
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"values\" : [ \"int\", \"string\" ] }, \"default\" : { \"m1\" : { \"string\" : \"abc\" } } } ] }",
        new Object[] {
          allModes,
          IllegalArgumentException.class,
          "cannot translate union value"
        }
      },
      {
        // optional map of union with no default
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"values\" : [ \"int\", \"string\" ] }, \"optional\" : true } ] }",
        new Object[] {
          allModes,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", { \"type\" : \"map\", \"values\" : [ \"int\", \"string\" ] } ], \"default\" : null } ] }",
          emptyFooSchema,
          emptyFooValue
        }
      },
      {
        // optional map of union with default, default value uses only 1st member type
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"values\" : [ \"int\", \"string\" ] }, \"optional\" : true, \"default\" : { \"m1\" : { \"int\" : 42 } } } ] }",
        new Object[] {
          translateDefault,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ { \"type\" : \"map\", \"values\" : [ \"int\", \"string\" ] }, \"null\" ], \"default\" : { \"m1\" : 42 } } ] }",
          emptyFooSchema,
          emptyFooValue
        },
        new Object[] {
          translateToNull,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", { \"type\" : \"map\", \"values\" : [ \"int\", \"string\" ] } ], \"default\" : null } ] }",
          emptyFooSchema,
          emptyFooValue
        },
      },
      {
        // optional map of union with default, default value uses 2nd member type
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"values\" : [ \"int\", \"string\" ] }, \"optional\" : true, \"default\" : { \"m1\" : { \"string\" : \"abc\" } } } ] }",
        new Object[] {
          translateDefault,
          IllegalArgumentException.class,
          "cannot translate union value"
        },
        new Object[] {
          translateToNull,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", { \"type\" : \"map\", \"values\" : [ \"int\", \"string\" ] } ], \"default\" : null } ] }",
          emptyFooSchema,
          emptyFooValue
        },
      },
      {
        // include
        "{ " +
        "  \"type\" : \"record\", " +
        "  \"name\" : \"foo\", " +
        "  \"include\" : [ " +
        "    { " +
        "      \"type\" : \"record\", " +
        "      \"name\" : \"bar\", " +
        "      \"fields\" : [ " +
        "        { \"name\" : \"b1\", \"type\" : \"int\" } " +
        "      ] " +
        "    } " +
        "  ], " +
        "  \"fields\" : [ " +
        "    { " +
        "      \"name\" : \"f1\", " +
        "      \"type\" : \"double\" " +
        "    } "+
        "  ] " +
        "}",
        new Object[] {
          allModes,
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"b1\", \"type\" : \"int\" }, { \"name\" : \"f1\", \"type\" : \"double\" } ] }"
        }
      },
      {
        // inconsistent default,
        // a referenced record has an optional field "frank" with default,
        // but field of referenced record type has default value which does not provide value for "frank"
        "{ " +
        "  \"type\" : \"record\", " +
        "  \"name\" : \"Bar\", " +
        "  \"fields\" : [ " +
        "    { " +
        "      \"name\" : \"barbara\", " +
        "      \"type\" : { " +
        "        \"type\" : \"record\", " +
        "        \"name\" : \"Foo\", " +
        "        \"fields\" : [ " +
        "          { " +
        "            \"name\" : \"frank\", " +
        "            \"type\" : \"string\", " +
        "            \"default\" : \"abc\", " +
        "            \"optional\" : true" +
        "          } " +
        "        ] " +
        "      }, " +
        "      \"default\" : { } " +
        "    } " +
        "  ]" +
        "}",
        new Object[] {
          translateDefault,
          IllegalArgumentException.class,
          "cannot translate absent optional field (to have null value) because this field is optional and has a default value"
        },
        new Object[] {
          translateToNull,
          "{ \"type\" : \"record\", \"name\" : \"Bar\", \"fields\" : [ { \"name\" : \"barbara\", \"type\" : { \"type\" : \"record\", \"name\" : \"Foo\", \"fields\" : [ { \"name\" : \"frank\", \"type\" : [ \"null\", \"string\" ], \"default\" : null } ] }, \"default\" : { \"frank\" : null } } ] }",
        }
      },
    };

    // test generating Avro schema from Pegasus schema
    for (Object[] row : inputs)
    {
      String schemaText = (String) row[0];
      if (debug) System.out.println(schemaText);

      for (int i = 1; i < row.length; i++)
      {
        Object[] modeInputs = (Object[]) row[i];
        OptionalDefaultMode optionalDefaultModes[] = (OptionalDefaultMode[]) modeInputs[0];
        for (OptionalDefaultMode optionalDefaultMode : optionalDefaultModes)
        {
          Object expected = modeInputs[1];
          DataSchema schema = TestUtil.dataSchemaFromString(schemaText);
          String preTranslateSchemaText = schema.toString();
          Exception exc = null;
          String avroTextFromSchema = null;
          try
          {
            avroTextFromSchema = SchemaTranslator.dataToAvroSchemaJson(
              schema,
              new DataToAvroSchemaTranslationOptions(optionalDefaultMode, JsonBuilder.Pretty.SPACES)
            );
            if (debug) System.out.println(optionalDefaultMode + ", Avro Schema: " + avroTextFromSchema);
          }
          catch (Exception e)
          {
            exc = e;
            if (debug) System.out.println(e);
          }
          if (expected instanceof String)
          {
            assertNull(exc);

            String avroText = (String) expected;
            assertEquals(avroTextFromSchema, avroText);
            String postTranslateSchemaText = schema.toString();
            assertEquals(preTranslateSchemaText, postTranslateSchemaText);

            // make sure Avro accepts it
            Schema avroSchema = Schema.parse(avroTextFromSchema);

            if (optionalDefaultMode == DataToAvroSchemaTranslationOptions.DEFAULT_OPTIONAL_DEFAULT_MODE)
            {
              // use other dataToAvroSchemaJson
              String avroSchema2Json = SchemaTranslator.dataToAvroSchemaJson(
                TestUtil.dataSchemaFromString(schemaText)
              );
              @SuppressWarnings("deprecation")
              String avroSchema2JsonCompact = SchemaTranslator.dataToAvroSchemaJson(
                TestUtil.dataSchemaFromString(schemaText),
                JsonBuilder.Pretty.COMPACT
              );
              assertEquals(avroSchema2Json, avroSchema2JsonCompact);
              Schema avroSchema2 = Schema.parse(avroSchema2Json);
              assertEquals(avroSchema2, avroSchema);

              // use dataToAvroSchema
              Schema avroSchema3 = SchemaTranslator.dataToAvroSchema(TestUtil.dataSchemaFromString(schemaText));
              assertEquals(avroSchema3, avroSchema2);
            }

            if (modeInputs.length >= 4)
            {
              // check if the translated default value is good by using it.
              // writer schema and Avro JSON value should not include fields with default values.
              String writerSchemaText = (String) modeInputs[2];
              String avroValueJson = (String) modeInputs[3];
              Schema writerSchema = Schema.parse(writerSchemaText);
              genericRecordFromString(avroValueJson, writerSchema, avroSchema);
            }
          }
          else
          {
            Class<?> expectedExceptionClass = (Class<?>) expected;
            String expectedString = (String) modeInputs[2];
            assertNotNull(exc);
            assertNull(avroTextFromSchema);
            assertTrue(expectedExceptionClass.isInstance(exc));
            assertTrue(exc.getMessage().contains(expectedString), "\"" + exc.getMessage() + "\" does not contain \"" + expectedString + "\"");
          }
        }
      }
    }
  }

  @Test
  public void testFromAvroSchema() throws IOException
  {
    boolean debug = false;

    String[][] inputs =
    {
      // {
      //   1st string is the Avro schema in JSON.
      //   2nd string is the expected output Pegasus schema in JSON.
      // }
      {
        // required, optional not specified
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ] }",
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ] }"
      },
      {
        // required and has default
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\", \"default\" : 42 } ] }",
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\", \"default\" : 42 } ] }"
      },
      {
        // union without null, 1 member type
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"int\" ] } ] }",
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"int\" ] } ] }"
      },
      {
        // union without null, 2 member types
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"int\", \"string\" ] } ] }",
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"int\", \"string\" ] } ] }"
      },
      {
        // union without null, 3 member types
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"int\", \"string\", \"boolean\" ] } ] }",
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"int\", \"string\", \"boolean\" ] } ] }"
      },
      {
        // union with null, 1 member type
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\" ] } ] }",
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [  ], \"optional\" : true } ] }"
      },
      {
        // union with null, 2 member types, no default
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\" ] } ] }",
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\", \"optional\" : true } ] }"
      },
      {
        // union with null, 2 member types, default is null (null is 1st member)
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\" ], \"default\" : null } ] }",
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\", \"optional\" : true } ] }"
      },
      {
        // union with null, 2 member types, default is not null (null is 1st member)
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"int\", \"null\" ], \"default\" : 42 } ] }",
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\", \"default\" : 42, \"optional\" : true } ] }",
      },
      {
        // union with null, 2 member types, default is not null, type is namespaced
        "{ \"type\" : \"record\", \"name\" : \"a.b.foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ { \"type\" : \"fixed\", \"name\" : \"a.c.baz\", \"size\" : 1 }, \"null\" ], \"default\" : \"1\" } ] }",
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"namespace\" : \"a.b\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"fixed\", \"name\" : \"baz\", \"namespace\" : \"a.c\", \"size\" : 1 }, \"default\" : \"1\", \"optional\" : true } ] }"
      },
      {
        // union with null, 2 member types, default is not null, type is namespaced as part of name
        "{ \"type\" : \"record\", \"name\" : \"a.b.foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"foo\", \"null\" ], \"default\" : { \"bar\" : null } } ] }",
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"namespace\" : \"a.b\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"foo\", \"default\" : {  }, \"optional\" : true } ] }"
      },
      {
        // union with null, 2 member types, default is no null, type is namespaced using namespace attribute
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"namespace\" : \"a.b\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"foo\", \"null\" ], \"default\" : { \"bar\" : null } } ] }",
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"namespace\" : \"a.b\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"foo\", \"default\" : {  }, \"optional\" : true } ] }"
      },
      {
        // union with null, 2 member types, default with multi-level nesting, type is namespaced using namespace attribute
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"namespace\" : \"a.b\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"foo\", \"null\" ], \"default\" : { \"bar\" : { \"bar\" : null } } } ] }",
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"namespace\" : \"a.b\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"foo\", \"default\" : { \"bar\" : {  } }, \"optional\" : true } ] }"
      },
      {
        // union with null, 2 member types, default is not null (null is 2nd member)
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"int\", \"null\" ], \"default\" : 42 } ] }",
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\", \"default\" : 42, \"optional\" : true } ] }",
      },
      {
        // union with null, 3 member types, no default
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\", \"string\" ] } ] }",
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"int\", \"string\" ], \"optional\" : true } ] }"
      },
      {
        // union with null, 3 member types, default is null
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\", \"string\" ], \"default\" : null } ] }",
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"int\", \"string\" ], \"optional\" : true } ] }"
      },
      {
        // union with null, 3 member types, default is not null
       "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"int\", \"null\", \"string\" ], \"default\" : 42 } ] }",
       "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"int\", \"string\" ], \"default\" : { \"int\" : 42 }, \"optional\" : true } ] }"
      },
      {
        // array of union with no default
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"array\", \"items\" : [ \"int\", \"string\" ] } } ] }",
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"array\", \"items\" : [ \"int\", \"string\" ] } } ] }",
      },
      {
        // array of union with default, default value uses only 1st member type
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"array\", \"items\" : [ \"int\", \"string\" ] }, \"default\" : [ 42, 13 ] } ] }",
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"array\", \"items\" : [ \"int\", \"string\" ] }, \"default\" : [ { \"int\" : 42 }, { \"int\" : 13 } ] } ] }",
      },
      {
        // array of union with default, default value uses only 1st null member type
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"array\", \"items\" : [ \"null\", \"string\" ] }, \"default\" : [ null, null ] } ] }",
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"array\", \"items\" : [ \"null\", \"string\" ] }, \"default\" : [ null, null ] } ] }",
      },
      {
        // "optional" array of union with no default
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", { \"type\" : \"array\", \"items\" : [ \"int\", \"string\" ] } ], \"default\" : null } ] }",
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"array\", \"items\" : [ \"int\", \"string\" ] }, \"optional\" : true } ] }",
      },
      {
        // "optional" array of union with default, default value uses only 1st member type
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ { \"type\" : \"array\", \"items\" : [ \"int\", \"string\" ] }, \"null\" ], \"default\" : [ 42, 13 ] } ] }",
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"array\", \"items\" : [ \"int\", \"string\" ] }, \"default\" : [ { \"int\" : 42 }, { \"int\" : 13 } ], \"optional\" : true } ] }",
      },
      {
        // map of union with no default
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"values\" : [ \"int\", \"string\" ] } } ] }",
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"values\" : [ \"int\", \"string\" ] } } ] }",
      },
      {
        // map of union with default, default value uses only 1st member type
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"values\" : [ \"int\", \"string\" ] }, \"default\" : { \"m1\" : 42 } } ] }",
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"values\" : [ \"int\", \"string\" ] }, \"default\" : { \"m1\" : { \"int\" : 42 } } } ] }",
      },
      {
        // map of union with default, default value uses only 1st null member type
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"values\" : [ \"null\", \"string\" ] }, \"default\" : { \"m1\" : null } } ] }",
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"values\" : [ \"null\", \"string\" ] }, \"default\" : { \"m1\" : null } } ] }",
      },
      {
        // optional map of union with no default
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", { \"type\" : \"map\", \"values\" : [ \"int\", \"string\" ] } ], \"default\" : null } ] }",
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"values\" : [ \"int\", \"string\" ] }, \"optional\" : true } ] }",
      },
      {
        // optional map of union with default, default value uses only 1st member type
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ { \"type\" : \"map\", \"values\" : [ \"int\", \"string\" ] }, \"null\" ], \"default\" : { \"m1\" : 42 } } ] }",
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"values\" : [ \"int\", \"string\" ] }, \"default\" : { \"m1\" : { \"int\" : 42 } }, \"optional\" : true } ] }",
      },
    };

    // test generating Pegasus schema from Avro schema
    for (String[] pair : inputs)
    {
      String avroText = pair[0];
      String schemaText = pair[1];
      if (debug) System.out.println(avroText);
      Schema avroSchema = Schema.parse(avroText);
      String preTranslateAvroSchema = avroSchema.toString();
      DataSchema schema = SchemaTranslator.avroToDataSchema(avroSchema);
      String schemaTextFromAvro = SchemaToJsonEncoder.schemaToJson(schema, JsonBuilder.Pretty.SPACES);
      assertEquals(schemaTextFromAvro, schemaText);
      String postTranslateAvroSchema = avroSchema.toString();
      assertEquals(preTranslateAvroSchema, postTranslateAvroSchema);
    }
  }

  @Test
  public void testUnionDefaultValues() throws IOException
  {
    boolean debug = false;

    final String emptySchemaText =
      "{ " +
      "  \"type\" : \"record\", " +
      "  \"name\" : \"foo\", " +
      "  \"fields\" : [] " +
      "}";

    final Schema emptySchema = Schema.parse(emptySchemaText);

    final String emptyRecord = "{}";

    final String input[] = {
      "{ " +
      "  \"type\" : \"record\", " +
      "  \"name\" : \"foo\", " +
      "  \"fields\" : [ " +
      "    { " +
      "      \"name\" : \"f1\", " +
      "      \"type\" : [ \"int\", \"null\" ], " +
      "      \"default\" : 42 " +
      "    }, " +
      "    { " +
      "      \"name\" : \"f2\", " +
      "      \"type\" : { " +
      "        \"type\" : \"record\", " +
      "        \"name\" : \"bar\", " +
      "        \"fields\" : [ " +
      "          { " +
      "            \"name\" : \"b1\", \"type\" : [ \"string\", \"null\" ] " +
      "          } " +
      "        ] " +
      "      }, " +
      "      \"default\" : { \"b1\" : \"abc\" } " +
      "    } " +
      "  ] " +
      "}",

      "{ " +
      "  \"type\" : \"record\", " +
      "  \"name\" : \"foo\", " +
      "  \"fields\" : [ " +
      "    { " +
      "      \"name\" : \"f1\", " +
      "      \"type\" : [ \"int\", \"null\" ], " +
      "      \"default\" : 42 " +
      "    }, " +
      "    { " +
      "      \"name\" : \"f2\", " +
      "      \"type\" : { " +
      "        \"type\" : \"record\", " +
      "        \"name\" : \"bar\", " +
      "        \"fields\" : [ " +
      "          { " +
      "            \"name\" : \"b1\", \"type\" : [ \"string\", \"null\" ], \"default\" : \"abc\" " +
      "          } " +
      "        ] " +
      "      }, " +
      "      \"default\" : { } " +
      "    } " +
      "  ] " +
      "}"
    };

    for (String readerSchemaText : input)
    {
      final Schema readerSchema = Schema.parse(readerSchemaText);

      GenericRecord record = TestSchemaTranslator.genericRecordFromString(emptyRecord, emptySchema, readerSchema);
      if (debug) System.out.println(record);

      SchemaParser parser = new SchemaParser();
      parser.getValidationOptions().setAvroUnionMode(true);
      InputStream stream = new ByteArrayInputStream(readerSchemaText.getBytes());
      parser.parse(stream);
      if (debug) System.out.println(parser.errorMessage());
      assertFalse(parser.hasError());

    }
  }

}
