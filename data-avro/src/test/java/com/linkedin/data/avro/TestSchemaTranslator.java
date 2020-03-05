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

import com.linkedin.data.Data;
import com.linkedin.data.DataMap;
import com.linkedin.data.TestUtil;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.JsonBuilder;
import com.linkedin.data.schema.PegasusSchemaParser;
import com.linkedin.data.schema.SchemaParser;
import com.linkedin.data.schema.SchemaToJsonEncoder;
import com.linkedin.data.schema.validation.ValidationOptions;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;


public class TestSchemaTranslator
{
  private static final String FS = File.separator;

  private static GenericRecord genericRecordFromString(String jsonString, Schema writerSchema, Schema readerSchema) throws IOException
  {
    GenericDatumReader<GenericRecord> reader = new GenericDatumReader<>(writerSchema, readerSchema);
    byte[] bytes = jsonString.getBytes(Data.UTF_8_CHARSET);
    Decoder binaryDecoder = DecoderFactory.defaultFactory().createBinaryDecoder(bytes, null);
    return reader.read(null, binaryDecoder);
  }

  @Test
  public void testTranslateDefaultBackwardsCompatibility()
  {
    DataToAvroSchemaTranslationOptions options = new DataToAvroSchemaTranslationOptions();
    assertSame(options.getOptionalDefaultMode(), OptionalDefaultMode.TRANSLATE_DEFAULT);
    assertSame(options.getPretty(), JsonBuilder.Pretty.COMPACT);

    assertSame(DataToAvroSchemaTranslationOptions.DEFAULT_OPTIONAL_DEFAULT_MODE, OptionalDefaultMode.TRANSLATE_DEFAULT);
  }

 @DataProvider
 public Object[][] toAvroSchemaData_testTypeRefAnnotationPropagation()
 {
   //These test were specially moved out from "toAvroSchemaData" tests because custom logic needed to validate the correctness
   //of those properties
   //The reason is that properties in the Avro's {@link Schema} class were represented as HashMap and
   //Schema#equal() comparision could cause issue when comparing properties because it uses serialization of HashMap as part of comparison
   // and the result won't be guaranteed to be same  as the entry set order might be different when serialized

   return new Object[][]
       {
           {
               // Test Annotations for TypeRef: one layer TypeRef case
               "{ \"type\" : \"record\", " +
               "\"name\" : \"Foo\", " +
               "\"namespace\" : \"com.x.y.z\", " +
               "\"fields\" : [ {" +
               "\"name\" : \"typedefField\", " +
               "\"type\" : { \"type\" : \"typeref\", " +
               "             \"name\" : \"refereeTypeName\", " +
               "             \"ref\"  : \"string\", " +
               "             \"compliance\" : [{\"dataType\":\"MEMBER_NAME\", \"format\": \"STRING\"}] } }] }",

               "{ \"type\" : \"record\", \"name\" : \"Foo\", \"namespace\" : \"com.x.y.z\", \"fields\" : [ { \"name\" : \"typedefField\", \"type\" : \"string\", \"compliance\" : [ { \"dataType\" : \"MEMBER_NAME\", \"format\" : \"STRING\" } ] } ] }"
           },
           {
               // Test Annotations propagation for TypeRef, reserved word, such as "validate", "java", should not be propagated
               "{" +
               "  \"type\": \"record\"," +
               "  \"name\": \"Foo\"," +
               "  \"namespace\": \"com.x.y.z\"," +
               "  \"fields\": [" +
               "    {" +
               "      \"name\": \"typedefField\"," +
               "      \"type\": {" +
               "        \"type\": \"typeref\"," +
               "        \"name\": \"refereeTypeName\"," +
               "        \"ref\": \"string\"," +
               "        \"compliance\": [" +
               "          {" +
               "            \"dataType\": \"MEMBER_NAME\"," +
               "            \"format\": \"STRING\"" +
               "          }" +
               "        ]," +
               "        \"validate\": {" +
               "          \"validator\": \"validateContent\"" +
               "        }," +
               "        \"java\": {" +
               "          \"class\": \"exampleTypedUrn\"" +
               "        }" +
               "      }" +
               "    }" +
               "  ]" +
               "}",

               "{ \"type\" : \"record\", " +
               "\"name\" : \"Foo\", " +
               "\"namespace\" : \"com.x.y.z\", " +
               "\"fields\" : [ " +
               "{ \"name\" : \"typedefField\", " +
               "\"type\" : \"string\", " +
               "\"compliance\" : [ { \"dataType\" : \"MEMBER_NAME\", \"format\" : \"STRING\" } ] } ] }",
           },
           {
               // Test Annotations for TypeRef : two layer nested TypeRef both have compliance annotation and outer layer should override
               "{\"type\" : " +
               "\"record\", " +
               "\"name\" : \"Foo\", " +
               "\"namespace\" : \"com.x.y.z\", " +
               "\"fields\" : [{\"name\" : " +
               "               \"typedefField\", " +
               "               \"type\" : {\"type\" : \"typeref\", " +
               "                           \"name\" : \"refereeTypeName\", " +
               "                           \"ref\"  : {\"type\" : \"typeref\", " +
               "                                       \"name\" : \"nestedrefereeTypeName\", " +
               "                                       \"ref\"  : \"int\", " +
               "               \"compliance\" : [{\"dataType\":\"MEMBER_NAME\", \"format\": \"INTEGER\"}] }, " +
               "\"compliance\" : [{\"dataType\":\"MEMBER_NAME\", \"format\": \"STRING\"}] } }] }",

               "{ \"type\" : \"record\", " +
               "\"name\" : \"Foo\", " +
               "\"namespace\" : \"com.x.y.z\", " +
               "\"fields\" : [ { " +
               "\"name\" : \"typedefField\", " +
               "\"type\" : \"int\", " +
               "\"compliance\" : [ { \"dataType\" : \"MEMBER_NAME\", \"format\" : \"STRING\" } ] } ] }"
           },

           {
               // Test Annotations for TypeRef : two layer nested TypeRef only second layer has compliance annotation
               "{ \"type\" : " +
               "\"record\", " +
               "\"name\" : " +
               "\"Foo\", " +
               "\"namespace\" : " +
               "\"com.x.y.z\", " +
               "\"fields\" : [ {\"name\" : " +
               "               \"typedefField\", " +
               "               \"type\" : { \"type\" : " +
               "                            \"typeref\", " +
               "                            \"name\" : " +
               "                            \"refereeTypeName\", " +
               "                            \"ref\"  : { \"type\" : " +
               "                                         \"typeref\", " +
               "                                         \"name\" : " +
               "                                         \"nestedrefereeTypeName\", " +
               "                                         \"ref\"  : \"int\", " +
               "\"compliance\" : [{\"dataType\":\"MEMBER_NAME\", \"format\": \"INTEGER\"}] } } }] }",

               "{ \"type\" : \"record\", \"name\" : \"Foo\", \"namespace\" : \"com.x.y.z\", \"fields\" : [ { \"name\" : \"typedefField\", \"type\" : \"int\", \"compliance\" : [ { \"dataType\" : \"MEMBER_NAME\", \"format\" : \"INTEGER\" } ] } ] }",
           },
           {
               // Test Annotations for TypeRef : three layer typerefs
               "{\"type\" : \"record\", " +
               "\"name\" : \"Foo\", " +
               "\"namespace\" : \"com.x.y.z\", " +
               "\"fields\" : [{\"name\" : \"typedefField\", " +
               "               \"type\" : {\"type\" : \"typeref\", " +
               "                           \"name\" : \"L1\", " +
               "                           \"ref\"  : {\"type\" : \"typeref\", " +
               "                                       \"name\" : \"L2\", " +
               "                                       \"ref\"  :  {\"type\" : " +
               "                                                    \"typeref\", " +
               "                                                    \"name\" : \"L3\", " +
               "                                                     \"ref\"  : \"boolean\", " +
               "\"compliance\" : [{\"dataType\":\"MEMBER_NAME\", \"format\": \"boolean\"}] } } } }] }",

               "{ \"type\" : \"record\", " +
               "\"name\" : \"Foo\", " +
               "\"namespace\" : \"com.x.y.z\", " +
               "\"fields\" : [ " + "{ " +
               "\"name\" : \"typedefField\", " +
               "\"type\" : \"boolean\", " + "" +
               "\"compliance\" : [ { \"dataType\" : \"MEMBER_NAME\", \"format\" : \"boolean\" } ] } ] }",
           },
           {
               // Test Annotations for TypeRef : one layer typeref, with field level has same property and has override and merged
               "{\n" +
               "  \"type\" : \"record\",\n" +
               "  \"name\" : \"Foo\",\n" +
               "  \"namespace\" : \"com.x.y.z\",\n" +
               "  \"fields\" : [\n" +
               "    {\"name\" : \"typedefMapField\",\n" +
               "      \"type\" :{\n" +
               "                    \"type\" : \"typeref\",\n" +
               "                    \"name\" : \"refToMap\", \"ref\" :{\n" +
               "                      \"type\" : \"map\",\n" +
               "                      \"values\":\"string\"\n" +
               "                    },\n" +
               "                    \"compliance\" : {\"/*\":[{\"dataType\":\"MEMBER_ID\"}], " +
               "                                     \"keysymbol\":[{\"dataType\":\"MEMBER_ID\"}]}\n" +
               "                },\n" +
               "      \"compliance\" : {\"keysymbol\":[{\"dataType\":\"MEMBER_NAME\"}]}\n" +
               "    }\n" +
               "  ]\n" +
               "}",

               "{ \"type\" : \"record\", \"name\" : \"Foo\", \"namespace\" : \"com.x.y.z\", \"fields\" : [ { \"name\" : \"typedefMapField\", \"type\" : { \"type\" : \"map\", \"values\" : \"string\" }, \"compliance\" : { \"keysymbol\" : [ { \"dataType\" : \"MEMBER_NAME\" } ], \"/*\" : [ { \"dataType\" : \"MEMBER_ID\" } ] } } ] }",
           },
           {
               // Test Annotations for TypeRef : one layer typeref, with field level has same property as Typeref and merged
               "{\n" +
               "  \"type\" : \"record\",\n" +
               "  \"name\" : \"Foo\",\n" +
               "  \"namespace\" : \"com.x.y.z\",\n" +
               "  \"fields\" : [\n" +
               "    {\"name\" : \"typedefMapField\",\n" +
               "      \"type\" :{\n" +
               "                    \"type\" : \"typeref\",\n" +
               "                    \"name\" : \"refToMap\", \"ref\" :{\n" +
               "                      \"type\" : \"map\",\n" +
               "                      \"values\":\"string\"\n" +
               "                    },\n" +
               "                    \"compliance\" : {\"/*\":[{\"dataType\":\"MEMBER_ID\"}]}\n" +
               "                },\n" +
               "      \"compliance\" : {\"keysymbol\":[{\"dataType\":\"MEMBER_NAME\"}]}\n" +
               "    }\n" +
               "  ]\n" +
               "}",

               "{ \"type\" : \"record\", \"name\" : \"Foo\", \"namespace\" : \"com.x.y.z\", \"fields\" : [ { \"name\" : \"typedefMapField\", \"type\" : { \"type\" : \"map\", \"values\" : \"string\" }, \"compliance\" : { \"keysymbol\" : [ { \"dataType\" : \"MEMBER_NAME\" } ], \"/*\" : [ { \"dataType\" : \"MEMBER_ID\" } ] } } ] }",
           },
           {
               // Test Annotations for TypeRef : one layer typeref, with field level has same property's override and not merged
               "{\n" +
               "  \"type\" : \"record\",\n" +
               "  \"name\" : \"Foo\",\n" +
               "  \"namespace\" : \"com.x.y.z\",\n" +
               "  \"fields\" : [\n" +
               "    {\"name\" : \"typedefMapField\",\n" +
               "      \"type\" :{\n" +
               "                    \"type\" : \"typeref\",\n" +
               "                    \"name\" : \"refToMap\", \"ref\" :{\n" +
               "                      \"type\" : \"map\",\n" +
               "                      \"values\":\"string\"\n" +
               "                    },\n" +
               "                    \"compliance\" : {\"/*\":[{\"dataType\":\"MEMBER_ID\"}]}\n" +
               "                },\n" +
               "      \"compliance\" : \"None\"\n" +
               "    }\n" +
               "  ]\n" +
               "}",

               "{ \"type\" : \"record\", " +
               "\"name\" : \"Foo\", " +
               "\"namespace\" : \"com.x.y.z\", " +
               "\"fields\" : [ { \"name\" : \"typedefMapField\", " +
               "\"type\" : { \"type\" : \"map\", \"values\" : \"string\" }, " +
               "\"compliance\" : \"None\" } ] }",
           },
           {
               // Test Annotations for TypeRef : one layer typeref, and properties merged
               "{\n" +
               "  \"type\" : \"record\",\n" +
               "  \"name\" : \"Foo\",\n" +
               "  \"namespace\" : \"com.x.y.z\",\n" +
               "  \"fields\" : [\n" +
               "    {\"name\" : \"typedefMapField\",\n" +
               "      \"type\" :{\n" +
               "                    \"type\" : \"typeref\",\n" +
               "                    \"name\" : \"refToMap\", \"ref\" :{\n" +
               "                      \"type\" : \"map\",\n" +
               "                      \"values\":\"string\"\n" +
               "                    },\n" +
               "                    \"compliance\" : {\"/*\":[{\"dataType\":\"MEMBER_ID\"}]}\n" +
               "                },\n" +
               "      \"otherannotation\" : \"None\"\n" +
               "    }\n" +
               "  ]\n" +
               "}",

               "{ \"type\" : \"record\", \"name\" : \"Foo\", \"namespace\" : \"com.x.y.z\", \"fields\" : [ { \"name\" : \"typedefMapField\", \"type\" : { \"type\" : \"map\", \"values\" : \"string\" }, \"otherannotation\" : \"None\", \"compliance\" : { \"/*\" : [ { \"dataType\" : \"MEMBER_ID\" } ] } } ] }",
           }

       };

 }

 @Test(dataProvider = "toAvroSchemaData_testTypeRefAnnotationPropagation")
 public void testToAvroSchema_testTypeRefAnnotationPropagation(String schemaBeforeTranslation,
                                                               String expectedAvroSchemaAsString) throws Exception
 {
   DataSchema schema = TestUtil.dataSchemaFromString(schemaBeforeTranslation);
   DataToAvroSchemaTranslationOptions transOptions = new DataToAvroSchemaTranslationOptions(OptionalDefaultMode.TRANSLATE_DEFAULT, JsonBuilder.Pretty.SPACES, EmbedSchemaMode.NONE);
   transOptions.setTyperefPropertiesExcludeSet(new HashSet<>(Arrays.asList("validate", "java")));

   String avroSchemaText = SchemaTranslator.dataToAvroSchemaJson(schema, transOptions);
   DataMap avroSchemaAsDataMap = TestUtil.dataMapFromString(avroSchemaText);
   DataMap fieldsPropertiesMap = TestUtil.dataMapFromString(expectedAvroSchemaAsString);
   assertEquals(fieldsPropertiesMap, avroSchemaAsDataMap);
 }


  @DataProvider
  public Object[][] toAvroSchemaData()
  {
    final String emptyFooSchema = "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ ] }";
    final String emptyFooValue = "{}";

    final OptionalDefaultMode allModes[] = { OptionalDefaultMode.TRANSLATE_DEFAULT, OptionalDefaultMode.TRANSLATE_TO_NULL };
    final OptionalDefaultMode translateDefault[] = { OptionalDefaultMode.TRANSLATE_DEFAULT };
    final OptionalDefaultMode translateToNull[] = { OptionalDefaultMode.TRANSLATE_TO_NULL };

    return new Object[][]
      {
          // {
          //   1st element is the Pegasus schema in JSON.
          //     The string may be marked with ##T_START and ##T_END markers. The markers are used for typeref testing.
          //     If the string these markers, then two schemas will be constructed and tested.
          //     The first schema replaces these markers with two empty strings.
          //     The second schema replaces these markers with a typeref enclosing the type between these markers.
          //   Each following element is an Object array,
          //     1st element of this array is an array of OptionalDefaultMode's to be used for default translation.
          //     2nd element is either a string or an Exception.
          //       If it is a string, it is the expected output Avro schema in JSON.
          //         If there are 3rd and 4th elements, then the 3rd element is an Avro schema used to write the 4th element
          //         which is JSON serialized Avro data. Usually, this is used to make sure that the translated default
          //         value is valid for Avro. Avro does not validate the default value in the schema. It will only
          //         de-serialize (and validate) the default value when it is actually used. The writer schema and
          //         the JSON serialized Avro data should not include fields with default values. The 4th element may be
          //         marked with ##Q_START and ##Q_END around enum values. On Avro v1.4, the GenericRecord#toString() does
          //         wrap enum values with quotes but it does on v1.6. These markers are used to handle this.
          //       If it is an Exception, then the Pegasus schema cannot be translated and this is the exception that
          //         is expected. The 3rd element is a string that should be contained in the message of the exception.
          // }
          {
              // custom properties :
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START \"int\" ##T_END } ], \"version\" : 1 }",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ], \"version\" : 1 }",
              null,
              null,
              null
          },
          {
              // required, optional not specified
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START \"int\" ##T_END } ] }",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ] }",
              null,
              null,
              null
          },
          {
              // required and has default
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START \"int\" ##T_END, \"default\" : 42 } ] }",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\", \"default\" : 42 } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // required, optional is false
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START \"int\" ##T_END, \"optional\" : false } ] }",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ] }",
              null,
              null,
              null
          },
          {
              // required, optional is false and has default
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START \"int\" ##T_END, \"default\" : 42, \"optional\" : false } ] }",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\", \"default\" : 42 } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // optional is true
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START \"int\" ##T_END, \"optional\" : true } ] }",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\" ], \"default\" : null } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // optional and has default
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START \"int\" ##T_END, \"optional\" : true, \"default\" : 42 } ] }",
              translateDefault,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"int\", \"null\" ], \"default\" : 42 } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // optional and has default
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START \"int\" ##T_END, \"optional\" : true, \"default\" : 42 } ] }",
              translateToNull,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\" ], \"default\" : null } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // optional and has default, enum type
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START { \"type\" : \"enum\", \"name\" : \"fruits\", \"symbols\" : [ \"APPLE\", \"ORANGE\" ] } ##T_END, \"optional\" : true, \"default\" : \"APPLE\" } ] }",
              translateDefault,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ { \"type\" : \"enum\", \"name\" : \"fruits\", \"symbols\" : [ \"APPLE\", \"ORANGE\" ] }, \"null\" ], \"default\" : \"APPLE\" } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // optional and has default, enum type
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START { \"type\" : \"enum\", \"name\" : \"fruits\", \"symbols\" : [ \"APPLE\", \"ORANGE\" ] } ##T_END, \"optional\" : true, \"default\" : \"APPLE\" } ] }",
              translateToNull,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", { \"type\" : \"enum\", \"name\" : \"fruits\", \"symbols\" : [ \"APPLE\", \"ORANGE\" ] } ], \"default\" : null } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // optional and has default with namespaced type
              "{ \"type\" : \"record\", \"name\" : \"a.b.foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START { \"type\" : \"record\", \"name\" : \"b.c.bar\", \"fields\" : [ ] } ##T_END, \"default\" : {  }, \"optional\" : true } ] }",
              translateDefault,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"namespace\" : \"a.b\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ { \"type\" : \"record\", \"name\" : \"bar\", \"namespace\" : \"b.c\", \"fields\" : [  ] }, \"null\" ], \"default\" : {  } } ] }",
              "{ \"type\" : \"record\", \"name\" : \"a.b.foo\", \"fields\" : [ ] }",
              emptyFooValue,
              null
          },
          {
              // optional and has default with namespaced type
              "{ \"type\" : \"record\", \"name\" : \"a.b.foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START { \"type\" : \"record\", \"name\" : \"b.c.bar\", \"fields\" : [ ] } ##T_END, \"default\" : {  }, \"optional\" : true } ] }",
              translateToNull,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"namespace\" : \"a.b\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", { \"type\" : \"record\", \"name\" : \"bar\", \"namespace\" : \"b.c\", \"fields\" : [  ] } ], \"default\" : null } ] }",
              "{ \"type\" : \"record\", \"name\" : \"a.b.foo\", \"fields\" : [ ] }",
              emptyFooValue,
              null
          },
          {
              // optional and has default value with multi-level nesting
              "{ \"type\" : \"record\", \"name\" : \"a.b.foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"record\", \"name\" : \"b.c.bar\", \"fields\" : [ { \"name\" : \"baz\", \"type\" : { \"type\" : \"record\", \"name\" : \"c.d.baz\", \"fields\" : [ ] } } ] }, \"default\" : { \"baz\" : { } }, \"optional\" : true } ] }",
              translateDefault,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"namespace\" : \"a.b\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ { \"type\" : \"record\", \"name\" : \"bar\", \"namespace\" : \"b.c\", \"fields\" : [ { \"name\" : \"baz\", \"type\" : { \"type\" : \"record\", \"name\" : \"baz\", \"namespace\" : \"c.d\", \"fields\" : [  ] } } ] }, \"null\" ], \"default\" : { \"baz\" : {  } } } ] }",
              "{ \"type\" : \"record\", \"name\" : \"a.b.foo\", \"fields\" : [ ] }",
              emptyFooValue,
              null
          },
          {
              // optional and has default value with multi-level nesting
              "{ \"type\" : \"record\", \"name\" : \"a.b.foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"record\", \"name\" : \"b.c.bar\", \"fields\" : [ { \"name\" : \"baz\", \"type\" : { \"type\" : \"record\", \"name\" : \"c.d.baz\", \"fields\" : [ ] } } ] }, \"default\" : { \"baz\" : { } }, \"optional\" : true } ] }",
              translateToNull,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"namespace\" : \"a.b\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", { \"type\" : \"record\", \"name\" : \"bar\", \"namespace\" : \"b.c\", \"fields\" : [ { \"name\" : \"baz\", \"type\" : { \"type\" : \"record\", \"name\" : \"baz\", \"namespace\" : \"c.d\", \"fields\" : [  ] } } ] } ], \"default\" : null } ] }",
              "{ \"type\" : \"record\", \"name\" : \"a.b.foo\", \"fields\" : [ ] }",
              emptyFooValue,
              null
          },
          {
              // optional and has default but with circular references with inconsistent defaults, inconsistent because optional field has default, and also missing (which requires default to be null)
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"foo\", \"default\" : {  }, \"optional\" : true } ] }",
              translateToNull,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"foo\" ], \"default\" : null } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // optional and has default but with circular references with inconsistent defaults, inconsistent because optional field has default, and also missing (which requires default to be null)
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"foo\", \"default\" : { \"bar\" : {  } }, \"optional\" : true } ] }",
              translateToNull,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"foo\" ], \"default\" : null } ] }",
              null,
              null,
              null
          },
          {
              // required union without null
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START [ \"int\", \"string\" ] ##T_END } ] }",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"int\", \"string\" ] } ] }",
              null,
              null,
              null
          },
          {
              // required union with null
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START [ \"null\", \"string\" ] ##T_END } ] }",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"string\" ] } ] }",
              null,
              null,
              null
          },
          {
              // optional union without null
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START [ \"int\", \"string\" ] ##T_END, \"optional\" : true } ] }",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\", \"string\" ], \"default\" : null } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // optional union with null
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START [ \"null\", \"int\", \"string\" ] ##T_END, \"optional\" : true } ] }",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\", \"string\" ], \"default\" : null } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // optional union without null and default is 1st member type
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START [ \"int\", \"string\" ] ##T_END, \"default\" : { \"int\" : 42 }, \"optional\" : true } ] }",
              translateDefault,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"int\", \"string\", \"null\" ], \"default\" : 42 } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // optional union without null and default is 1st member type
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START [ \"int\", \"string\" ] ##T_END, \"default\" : { \"int\" : 42 }, \"optional\" : true } ] }",
              translateToNull,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\", \"string\" ], \"default\" : null } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // optional union without null and default is 2nd member type
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START [ \"int\", \"string\" ] ##T_END, \"default\" : { \"string\" : \"abc\" }, \"optional\" : true } ] }",
              translateToNull,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\", \"string\" ], \"default\" : null } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // optional union with null and non-null default, default is 1st member type
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START [ \"int\", \"null\", \"string\" ] ##T_END, \"default\" : { \"int\" : 42 }, \"optional\" : true } ] }",
              translateDefault,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"int\", \"null\", \"string\" ], \"default\" : 42 } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // optional union with null and non-null default, default is 1st member type
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START [ \"int\", \"null\", \"string\" ] ##T_END, \"default\" : { \"int\" : 42 }, \"optional\" : true } ] }",
              translateToNull,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\", \"string\" ], \"default\" : null } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // optional union with null and non-null default, default is 2nd member type
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START [ \"int\", \"null\", \"string\" ] ##T_END, \"default\" : null, \"optional\" : true } ] }",
              translateToNull,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\", \"string\" ], \"default\" : null } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // optional union with null and non-null default, default is 3rd member type
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START [ \"int\", \"null\", \"string\" ] ##T_END, \"default\" : { \"string\" : \"abc\" }, \"optional\" : true } ] }",
              translateToNull,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\", \"string\" ], \"default\" : null } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // optional union with null and null default, default is 1st member type
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START [ \"null\", \"int\", \"string\" ] ##T_END, \"default\" : null, \"optional\" : true } ] }",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\", \"string\" ], \"default\" : null } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // optional union but with circular references with inconsistent defaults, inconsistent because optional field has default, and also missing (which requires default to be null)
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START [ \"foo\", \"string\" ] ##T_END, \"default\" : { \"foo\" : { } }, \"optional\" : true } ] }",
              translateToNull,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"foo\", \"string\" ], \"default\" : null } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // optional union but with circular references with but with consistent defaults (the only default that works is null for circularly referenced unions)
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START [ \"null\", \"foo\" ] ##T_END, \"default\" : null, \"optional\" : true } ] }",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"foo\" ], \"default\" : null } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // typeref of fixed
              "##T_START { \"type\" : \"record\", \"name\" : \"Foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ] } ##T_END",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"Foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ] }",
              null,
              null,
              null
          },
          {
              // typeref of enum
              "##T_START { \"type\" : \"enum\", \"name\" : \"Fruits\", \"symbols\" : [ \"APPLE\", \"ORANGE\" ] } ##T_END",
              allModes,
              "{ \"type\" : \"enum\", \"name\" : \"Fruits\", \"symbols\" : [ \"APPLE\", \"ORANGE\" ] }",
              null,
              null,
              null
          },
          {
              // typeref of fixed
              "##T_START { \"type\" : \"fixed\", \"name\" : \"Md5\", \"size\" : 16 } ##T_END",
              allModes,
              "{ \"type\" : \"fixed\", \"name\" : \"Md5\", \"size\" : 16 }",
              null,
              null,
              null
          },
          {
              // typeref of array
              "##T_START { \"type\" : \"array\", \"items\" : \"int\" } ##T_END",
              allModes,
              "{ \"type\" : \"array\", \"items\" : \"int\" }",
              null,
              null,
              null
          },
          {
              // typeref of map
              "##T_START { \"type\" : \"map\", \"values\" : \"int\" } ##T_END",
              allModes,
              "{ \"type\" : \"map\", \"values\" : \"int\" }",
              null,
              null,
              null
          },
          {
              // typeref of union
              "##T_START [ \"null\", \"int\" ] ##T_END",
              allModes,
              "[ \"null\", \"int\" ]",
              null,
              null,
              null
          },
          {
              // typeref in array
              "{ \"type\" : \"array\", \"items\" : ##T_START \"int\" ##T_END }",
              allModes,
              "{ \"type\" : \"array\", \"items\" : \"int\" }",
              null,
              null,
              null
          },
          {
              // typeref in map
              "{ \"type\" : \"map\", \"values\" : ##T_START \"int\" ##T_END }",
              allModes,
              "{ \"type\" : \"map\", \"values\" : \"int\" }",
              null,
              null,
              null
          },
          {
              // typeref in union
              "[ \"null\", ##T_START \"int\" ##T_END ]",
              allModes,
              "[ \"null\", \"int\" ]",
              null,
              null,
              null
          },
          {
              // record field with union with typeref, without null in record field
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"string\", ##T_START \"int\" ##T_END ] } ] }",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"string\", \"int\" ] } ] }",
              null,
              null,
              null
          },
          {
              // record field with union with typeref, without null and default is 1st member type and not typeref-ed
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"string\", ##T_START \"int\" ##T_END ], \"default\" : { \"string\" : \"abc\" } } ] }",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"string\", \"int\" ], \"default\" : \"abc\" } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // record field with union with typeref, without null and default is 1st member type and typeref-ed
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ ##T_START \"int\" ##T_END, \"string\" ], \"default\" : { \"int\" : 42 } } ] }",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"int\", \"string\" ], \"default\" : 42 } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // record field with union with typeref, without null and optional
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"string\", ##T_START \"int\" ##T_END ], \"optional\" : true } ] }",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"string\", \"int\" ], \"default\" : null } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // record field with union with typeref, without null and optional, default is 1st member and not typeref-ed
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"string\", ##T_START \"int\" ##T_END ], \"optional\" : true, \"default\" : { \"string\" : \"abc\" } } ] }",
              translateDefault,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"string\", \"int\", \"null\" ], \"default\" : \"abc\" } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // record field with union with typeref, without null and optional, default is 1st member and not typeref-ed
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"string\", ##T_START \"int\" ##T_END ], \"optional\" : true, \"default\" : { \"string\" : \"abc\" } } ] }",
              translateToNull,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"string\", \"int\" ], \"default\" : null } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // record field with union with typeref, without null and optional, default is 1st member and typeref-ed
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ ##T_START \"int\" ##T_END, \"string\" ], \"optional\" : true, \"default\" : { \"int\" : 42 } } ] }",
              translateToNull,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\", \"string\" ], \"default\" : null } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // record field with union with typeref, without null and optional, default is 2nd member and not typeref-ed
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ ##T_START \"int\" ##T_END, \"string\" ], \"optional\" : true, \"default\" : { \"string\" : \"abc\" } } ] }",
              translateToNull,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\", \"string\" ], \"default\" : null } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // record field with union with typeref, without null and optional, default is 2nd member and typeref-ed
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"string\", ##T_START \"int\" ##T_END ], \"optional\" : true, \"default\" : { \"int\" : 42 } } ] }",
              translateToNull,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"string\", \"int\" ], \"default\" : null } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // record field with union with typeref, with null 1st member
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", ##T_START \"int\" ##T_END ] } ] }",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\" ] } ] }",
              null,
              null,
              null
          },
          {
              // record field with union with typeref, with null 1st member, default is 1st member and null
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", ##T_START \"int\" ##T_END ], \"default\" : null } ] }",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\" ], \"default\" : null } ] }",
              null,
              null,
              null
          },
          {
              // record field with union with typeref with null 1st member, and optional
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", ##T_START \"int\" ##T_END ], \"optional\" : true } ] }",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\" ], \"default\" : null } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // record field with union with typeref with null 1st member, and optional, default is 1st member and null
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", ##T_START \"int\" ##T_END ], \"optional\" : true, \"default\" : null } ] }",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\" ], \"default\" : null } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // record field with union with typeref with null 1st member, and optional, default is last member and typeref-ed
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", ##T_START \"int\" ##T_END ], \"optional\" : true, \"default\" : { \"int\" : 42 } } ] }",
              translateToNull,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\" ], \"default\" : null } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // record field with union with typeref, with null last member
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ ##T_START \"int\" ##T_END, \"null\" ] } ] }",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"int\", \"null\" ] } ] }",
              null,
              null,
              null
          },
          {
              // record field with union with typeref, with null last member, default is 1st member and typeref-ed
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ ##T_START \"int\" ##T_END, \"null\" ], \"default\" : { \"int\" : 42 } } ] }",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"int\", \"null\" ], \"default\" : 42 } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // record field with union with typeref, with null last member, and optional
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ ##T_START \"int\" ##T_END, \"null\" ], \"optional\" : true } ] }",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\" ], \"default\" : null } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // record field with union with typeref, with null last member, and optional, default is 1st member and typeref-ed
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ ##T_START \"int\" ##T_END, \"null\" ], \"optional\" : true, \"default\" : { \"int\" : 42 } } ] }",
              translateDefault,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"int\", \"null\" ], \"default\" : 42 } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // record field with union with typeref, with null last member, and optional, default is 1st member and typeref-ed
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ ##T_START \"int\" ##T_END, \"null\" ], \"optional\" : true, \"default\" : { \"int\" : 42 } } ] }",
              translateToNull,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\" ], \"default\" : null } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // record field with union with typeref, with null last member, and optional, default is last member and null
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ ##T_START \"int\" ##T_END, \"null\" ], \"optional\" : true, \"default\" : null } ] }",
              translateToNull,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\" ], \"default\" : null } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // array of union with no default
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"array\", \"items\" : ##T_START [ \"int\", \"string\" ] ##T_END } } ] }",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"array\", \"items\" : [ \"int\", \"string\" ] } } ] }",
              null,
              null,
              null
          },
          {
              // array of union with default, default value uses only 1st member type
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START { \"type\" : \"array\", \"items\" : [ \"int\", \"string\" ] } ##T_END, \"default\" : [ { \"int\" : 42 }, { \"int\" : 13 } ] } ] }",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"array\", \"items\" : [ \"int\", \"string\" ] }, \"default\" : [ 42, 13 ] } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // array of union with default, default value uses only 1st null member type
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"array\", \"items\" : ##T_START [ \"null\", \"string\" ] ##T_END }, \"default\" : [ null, null ] } ] }",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"array\", \"items\" : [ \"null\", \"string\" ] }, \"default\" : [ null, null ] } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // optional array of union with no default
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"array\", \"items\" : ##T_START [ \"int\", \"string\" ] ##T_END }, \"optional\" : true } ] }",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", { \"type\" : \"array\", \"items\" : [ \"int\", \"string\" ] } ], \"default\" : null } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // optional array of union with default, default value uses only 1st member type
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START { \"type\" : \"array\", \"items\" : [ \"int\", \"string\" ] } ##T_END, \"optional\" : true, \"default\" : [ { \"int\" : 42 }, { \"int\" : 13 } ] } ] }",
              translateDefault,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ { \"type\" : \"array\", \"items\" : [ \"int\", \"string\" ] }, \"null\" ], \"default\" : [ 42, 13 ] } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // optional array of union with default, default value uses only 1st member type
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START { \"type\" : \"array\", \"items\" : [ \"int\", \"string\" ] } ##T_END, \"optional\" : true, \"default\" : [ { \"int\" : 42 }, { \"int\" : 13 } ] } ] }",
              translateToNull,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", { \"type\" : \"array\", \"items\" : [ \"int\", \"string\" ] } ], \"default\" : null } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // optional array of union with default, default value uses 2nd member type
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"array\", \"items\" : ##T_START [ \"int\", \"string\" ] ##T_END }, \"optional\" : true, \"default\" : [ { \"int\" : 42 }, { \"string\" : \"abc\" } ] } ] }",
              translateToNull,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", { \"type\" : \"array\", \"items\" : [ \"int\", \"string\" ] } ], \"default\" : null } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // map of union with no default
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"values\" : ##T_START [ \"int\", \"string\" ] ##T_END } } ] }",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"values\" : [ \"int\", \"string\" ] } } ] }",
              null,
              null,
              null
          },
          {
              // map of union with default, default value uses only 1st member type
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START { \"type\" : \"map\", \"values\" : [ \"int\", \"string\" ] } ##T_END, \"default\" : { \"m1\" : { \"int\" : 42 } } } ] }",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"values\" : [ \"int\", \"string\" ] }, \"default\" : { \"m1\" : 42 } } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // map of union with default, default value uses only 1st null member type
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"values\" : ##T_START [ \"null\", \"string\" ] ##T_END }, \"default\" : { \"m1\" : null } } ] }",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"values\" : [ \"null\", \"string\" ] }, \"default\" : { \"m1\" : null } } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // optional map of union with no default
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"values\" : ##T_START [ \"int\", \"string\" ] ##T_END }, \"optional\" : true } ] }",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", { \"type\" : \"map\", \"values\" : [ \"int\", \"string\" ] } ], \"default\" : null } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // optional map of union with default, default value uses only 1st member type
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START { \"type\" : \"map\", \"values\" : [ \"int\", \"string\" ] } ##T_END, \"optional\" : true, \"default\" : { \"m1\" : { \"int\" : 42 } } } ] }",
              translateDefault,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ { \"type\" : \"map\", \"values\" : [ \"int\", \"string\" ] }, \"null\" ], \"default\" : { \"m1\" : 42 } } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // optional map of union with default, default value uses only 1st member type
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START { \"type\" : \"map\", \"values\" : [ \"int\", \"string\" ] } ##T_END, \"optional\" : true, \"default\" : { \"m1\" : { \"int\" : 42 } } } ] }",
              translateToNull,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", { \"type\" : \"map\", \"values\" : [ \"int\", \"string\" ] } ], \"default\" : null } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // optional map of union with default, default value uses 2nd member type
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"values\" : ##T_START [ \"int\", \"string\" ] ##T_END }, \"optional\" : true, \"default\" : { \"m1\" : { \"string\" : \"abc\" } } } ] }",
              translateToNull,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", { \"type\" : \"map\", \"values\" : [ \"int\", \"string\" ] } ], \"default\" : null } ] }",
              emptyFooSchema,
              emptyFooValue,
              null
          },
          {
              // include
              "{ " +
                  "  \"type\" : \"record\", " +
                  "  \"name\" : \"foo\", " +
                  "  \"include\" : [ " +
                  "    ##T_START { " +
                  "      \"type\" : \"record\", " +
                  "      \"name\" : \"bar\", " +
                  "      \"fields\" : [ " +
                  "        { \"name\" : \"b1\", \"type\" : \"int\" } " +
                  "      ] " +
                  "    } ##T_END " +
                  "  ], " +
                  "  \"fields\" : [ " +
                  "    { " +
                  "      \"name\" : \"f1\", " +
                  "      \"type\" : \"double\" " +
                  "    } "+
                  "  ] " +
                  "}",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"b1\", \"type\" : \"int\" }, { \"name\" : \"f1\", \"type\" : \"double\" } ] }",
              null,
              null,
              null
          },
          {
              // include more than once
              "{ " +
                  "  \"type\" : \"record\", " +
                  "  \"name\" : \"foo\", " +
                  "  \"include\" : [ " +
                  "    ##T_START { " +
                  "      \"type\" : \"record\", " +
                  "      \"name\" : \"bar\", " +
                  "      \"fields\" : [ " +
                  "        { \"name\" : \"b1\", \"type\" : \"int\", \"optional\" : true } " +
                  "      ] " +
                  "    } ##T_END " +
                  "  ], " +
                  "  \"fields\" : [ " +
                  "    { " +
                  "      \"name\" : \"f1\", " +
                  "      \"type\" : { \"type\" : \"record\", \"name\" : \"f1\", \"include\" : [ \"bar\" ], \"fields\" : [] }" +
                  "    }, "+
                  "    { " +
                  "      \"name\" : \"f2\", " +
                  "      \"type\" : { \"type\" : \"record\", \"name\" : \"f2\", \"include\" : [ \"bar\" ], \"fields\" : [] }" +
                  "    } "+
                  "  ] " +
                  "}",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"b1\", \"type\" : [ \"null\", \"int\" ], \"default\" : null }, { \"name\" : \"f1\", \"type\" : { \"type\" : \"record\", \"name\" : \"f1\", \"fields\" : [ { \"name\" : \"b1\", \"type\" : [ \"null\", \"int\" ], \"default\" : null } ] } }, { \"name\" : \"f2\", \"type\" : { \"type\" : \"record\", \"name\" : \"f2\", \"fields\" : [ { \"name\" : \"b1\", \"type\" : [ \"null\", \"int\" ], \"default\" : null } ] } } ] }",
              null,
              null,
              null
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
              translateToNull,
              "{ \"type\" : \"record\", \"name\" : \"Bar\", \"fields\" : [ { \"name\" : \"barbara\", \"type\" : { \"type\" : \"record\", \"name\" : \"Foo\", \"fields\" : [ { \"name\" : \"frank\", \"type\" : [ \"null\", \"string\" ], \"default\" : null } ] }, \"default\" : { \"frank\" : null } } ] }",
              null,
              null,
              null
          },
          {
              // default override "foo1" default for "bar1" is "xyz", it should override "bar1" default "abc".
              "{\n" +
                  "  \"type\":\"record\",\n" +
                  "  \"name\":\"foo\",\n" +
                  "  \"fields\":[\n" +
                  "    {\n" +
                  "      \"name\": \"foo1\",\n" +
                  "      \"type\": {\n" +
                  "        \"type\" : \"record\",\n" +
                  "        \"name\" : \"bar\",\n" +
                  "        \"fields\" : [\n" +
                  "           {\n" +
                  "             \"name\" : \"bar1\",\n" +
                  "             \"type\" : \"string\",\n" +
                  "             \"default\" : \"abc\", " +
                  "             \"optional\" : true\n" +
                  "           }\n" +
                  "        ]\n" +
                  "      },\n" +
                  "      \"optional\": true,\n" +
                  "      \"default\": { \"bar1\": \"xyz\" }\n" +
                  "    }\n" +
                  "  ]\n" +
                  "}\n",
              translateDefault,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"foo1\", \"type\" : [ { \"type\" : \"record\", \"name\" : \"bar\", \"fields\" : [ { \"name\" : \"bar1\", \"type\" : [ \"string\", \"null\" ], \"default\" : \"abc\" } ] }, \"null\" ], \"default\" : { \"bar1\" : \"xyz\" } } ] }",
              emptyFooSchema,
              "{}",
              "{\"foo1\": {\"bar1\": \"xyz\"}}"
          },
          {
              // Required 'union with aliases' field with no default value
              "{" +
                  "\"type\": \"record\"," +
                  "\"name\": \"foo\"," +
                  "\"fields\": [" +
                  "{" +
                  "\"name\": \"result\"," +
                  "\"type\": ##T_START [" +
                  "{ \"type\" : \"string\", \"alias\" : \"success\", \"doc\": \"Success message\" }," +
                  "{ \"type\" : \"string\", \"alias\" : \"failure\", \"doc\": \"Failure message\" }" +
                  "] ##T_END" +
                  "}" +
                  "]" +
                  "}",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"result\", \"type\" : { \"type\" : \"record\", \"name\" : \"fooResult\", \"fields\" : [ { \"name\" : \"success\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Success message\", \"default\" : null }, { \"name\" : \"failure\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Failure message\", \"default\" : null }, { \"name\" : \"fieldDiscriminator\", \"type\" : { \"type\" : \"enum\", \"name\" : \"fooResultDiscriminator\", \"symbols\" : [ \"success\", \"failure\" ] }, \"doc\" : \"Contains the name of the field that has its value set.\" } ] } } ] }",
              null,
              null,
              null
          },
          {
              // Required 'union with aliases' field with a null member and no default value
              "{" +
                  "\"type\": \"record\"," +
                  "\"name\": \"foo\"," +
                  "\"fields\": [" +
                  "{" +
                  "\"name\": \"result\"," +
                  "\"type\": [" +
                  "\"null\"," +
                  "{ \"type\" : \"string\", \"alias\" : \"success\", \"doc\": \"Success message\" }," +
                  "{ \"type\" : \"string\", \"alias\" : \"failure\", \"doc\": \"Failure message\" }" +
                  "]" +
                  "}" +
                  "]" +
                  "}",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"result\", \"type\" : { \"type\" : \"record\", \"name\" : \"fooResult\", \"fields\" : [ { \"name\" : \"success\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Success message\", \"default\" : null }, { \"name\" : \"failure\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Failure message\", \"default\" : null }, { \"name\" : \"fieldDiscriminator\", \"type\" : { \"type\" : \"enum\", \"name\" : \"fooResultDiscriminator\", \"symbols\" : [ \"null\", \"success\", \"failure\" ] }, \"doc\" : \"Contains the name of the field that has its value set.\" } ] } } ] }",
              null,
              null,
              null
          },
          {
              // Optional 'union with aliases' field with no default value
              "{" +
                  "\"type\": \"record\"," +
                  "\"name\": \"foo\"," +
                  "\"fields\": [" +
                  "{" +
                  "\"name\": \"result\"," +
                  "\"type\": [" +
                  "{ \"type\" : \"string\", \"alias\" : \"success\", \"doc\": \"Success message\" }," +
                  "{ \"type\" : \"string\", \"alias\" : \"failure\", \"doc\": \"Failure message\" }" +
                  "]," +
                  "\"optional\": true" +
                  "}" +
                  "]" +
                  "}",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"result\", \"type\" : [ \"null\", { \"type\" : \"record\", \"name\" : \"fooResult\", \"fields\" : [ { \"name\" : \"success\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Success message\", \"default\" : null }, { \"name\" : \"failure\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Failure message\", \"default\" : null }, { \"name\" : \"fieldDiscriminator\", \"type\" : { \"type\" : \"enum\", \"name\" : \"fooResultDiscriminator\", \"symbols\" : [ \"success\", \"failure\" ] }, \"doc\" : \"Contains the name of the field that has its value set.\" } ] } ], \"default\" : null } ] }",
              emptyFooSchema,
              emptyFooValue,
              "{\"result\": null}"
          },
          {
              // Optional 'union with aliases' field with a null member and no default value
              "{" +
                  "\"type\": \"record\"," +
                  "\"name\": \"foo\"," +
                  "\"fields\": [" +
                  "{" +
                  "\"name\": \"result\"," +
                  "\"type\": [" +
                  "\"null\"," +
                  "{ \"type\" : \"string\", \"alias\" : \"success\", \"doc\": \"Success message\" }," +
                  "{ \"type\" : \"string\", \"alias\" : \"failure\", \"doc\": \"Failure message\" }" +
                  "]," +
                  "\"optional\": true" +
                  "}" +
                  "]" +
                  "}",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"result\", \"type\" : [ \"null\", { \"type\" : \"record\", \"name\" : \"fooResult\", \"fields\" : [ { \"name\" : \"success\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Success message\", \"default\" : null }, { \"name\" : \"failure\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Failure message\", \"default\" : null }, { \"name\" : \"fieldDiscriminator\", \"type\" : { \"type\" : \"enum\", \"name\" : \"fooResultDiscriminator\", \"symbols\" : [ \"null\", \"success\", \"failure\" ] }, \"doc\" : \"Contains the name of the field that has its value set.\" } ] } ], \"default\" : null } ] }",
              emptyFooSchema,
              emptyFooValue,
              "{\"result\": null}"
          },
          {
              // Required 'union with aliases' field with a default value
              "{" +
                  "\"type\": \"record\"," +
                  "\"name\": \"foo\"," +
                  "\"fields\": [" +
                  "{" +
                  "\"name\": \"result\"," +
                  "\"type\": [" +
                  "{ \"type\" : \"string\", \"alias\" : \"success\", \"doc\": \"Success message\" }," +
                  "{ \"type\" : \"string\", \"alias\" : \"failure\", \"doc\": \"Failure message\" }" +
                  "]," +
                  "\"default\": { \"success\": \"Union with aliases.\" }" +
                  "}" +
                  "]" +
                  "}",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"result\", \"type\" : { \"type\" : \"record\", \"name\" : \"fooResult\", \"fields\" : [ { \"name\" : \"success\", \"type\" : [ \"string\", \"null\" ], \"doc\" : \"Success message\", \"default\" : \"Union with aliases.\" }, { \"name\" : \"failure\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Failure message\", \"default\" : null }, { \"name\" : \"fieldDiscriminator\", \"type\" : { \"type\" : \"enum\", \"name\" : \"fooResultDiscriminator\", \"symbols\" : [ \"success\", \"failure\" ] }, \"doc\" : \"Contains the name of the field that has its value set.\" } ] }, \"default\" : { \"failure\" : null, \"fieldDiscriminator\" : \"success\", \"success\" : \"Union with aliases.\" } } ] }",
              emptyFooSchema,
              emptyFooValue,
              "{\"result\": {\"success\": \"Union with aliases.\", \"failure\": null, \"fieldDiscriminator\": ##Q_STARTsuccess##Q_END}}"
          },
          {
              // Optional 'union with aliases' field with a default value
              "{" +
                  "\"type\": \"record\"," +
                  "\"name\": \"foo\"," +
                  "\"fields\": [" +
                  "{" +
                  "\"name\": \"result\"," +
                  "\"type\": [" +
                  "{ \"type\" : \"string\", \"alias\" : \"success\", \"doc\": \"Success message\" }," +
                  "{ \"type\" : \"string\", \"alias\" : \"failure\", \"doc\": \"Failure message\" }" +
                  "]," +
                  "\"optional\": true," +
                  "\"default\": { \"success\": \"Union with aliases.\" }" +
                  "}" +
                  "]" +
                  "}",
              translateDefault,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"result\", \"type\" : [ { \"type\" : \"record\", \"name\" : \"fooResult\", \"fields\" : [ { \"name\" : \"success\", \"type\" : [ \"string\", \"null\" ], \"doc\" : \"Success message\", \"default\" : \"Union with aliases.\" }, { \"name\" : \"failure\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Failure message\", \"default\" : null }, { \"name\" : \"fieldDiscriminator\", \"type\" : { \"type\" : \"enum\", \"name\" : \"fooResultDiscriminator\", \"symbols\" : [ \"success\", \"failure\" ] }, \"doc\" : \"Contains the name of the field that has its value set.\" } ] }, \"null\" ], \"default\" : { \"failure\" : null, \"fieldDiscriminator\" : \"success\", \"success\" : \"Union with aliases.\" } } ] }",
              emptyFooSchema,
              emptyFooValue,
              "{\"result\": {\"success\": \"Union with aliases.\", \"failure\": null, \"fieldDiscriminator\": ##Q_STARTsuccess##Q_END}}"
          },
          {
              // Optional 'union with aliases' field with a default value
              "{" +
                  "\"type\": \"record\"," +
                  "\"name\": \"foo\"," +
                  "\"fields\": [" +
                  "{" +
                  "\"name\": \"result\"," +
                  "\"type\": [" +
                  "{ \"type\" : \"string\", \"alias\" : \"success\", \"doc\": \"Success message\" }," +
                  "{ \"type\" : \"string\", \"alias\" : \"failure\", \"doc\": \"Failure message\" }" +
                  "]," +
                  "\"optional\": true," +
                  "\"default\": { \"success\": \"Union with aliases.\" }" +
                  "}" +
                  "]" +
                  "}",
              translateToNull,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"result\", \"type\" : [ \"null\", { \"type\" : \"record\", \"name\" : \"fooResult\", \"fields\" : [ { \"name\" : \"success\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Success message\", \"default\" : null }, { \"name\" : \"failure\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Failure message\", \"default\" : null }, { \"name\" : \"fieldDiscriminator\", \"type\" : { \"type\" : \"enum\", \"name\" : \"fooResultDiscriminator\", \"symbols\" : [ \"success\", \"failure\" ] }, \"doc\" : \"Contains the name of the field that has its value set.\" } ] } ], \"default\" : null } ] }",
              emptyFooSchema,
              emptyFooValue,
              "{\"result\": null}"
          },
          {
              // Optional 'union with aliases' field with a null member and a default null value
              "{" +
                  "\"type\": \"record\"," +
                  "\"name\": \"foo\"," +
                  "\"fields\": [" +
                  "{" +
                  "\"name\": \"result\"," +
                  "\"type\": [" +
                  "\"null\"," +
                  "{ \"type\" : \"string\", \"alias\" : \"success\", \"doc\": \"Success message\" }," +
                  "{ \"type\" : \"string\", \"alias\" : \"failure\", \"doc\": \"Failure message\" }" +
                  "]," +
                  "\"optional\": true," +
                  "\"default\": null" +
                  "}" +
                  "]" +
                  "}",
              translateDefault,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"result\", \"type\" : [ { \"type\" : \"record\", \"name\" : \"fooResult\", \"fields\" : [ { \"name\" : \"success\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Success message\", \"default\" : null }, { \"name\" : \"failure\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Failure message\", \"default\" : null }, { \"name\" : \"fieldDiscriminator\", \"type\" : { \"type\" : \"enum\", \"name\" : \"fooResultDiscriminator\", \"symbols\" : [ \"null\", \"success\", \"failure\" ] }, \"doc\" : \"Contains the name of the field that has its value set.\" } ] }, \"null\" ], \"default\" : { \"failure\" : null, \"fieldDiscriminator\" : \"null\", \"success\" : null } } ] }",
              emptyFooSchema,
              emptyFooValue,
              "{\"result\": {\"success\": null, \"failure\": null, \"fieldDiscriminator\": ##Q_STARTnull##Q_END}}"
          },
          {
              // Optional 'union with aliases' field with a null member and a default null value
              "{" +
                  "\"type\": \"record\"," +
                  "\"name\": \"foo\"," +
                  "\"fields\": [" +
                  "{" +
                  "\"name\": \"result\"," +
                  "\"type\": [" +
                  "\"null\"," +
                  "{ \"type\" : \"string\", \"alias\" : \"success\", \"doc\": \"Success message\" }," +
                  "{ \"type\" : \"string\", \"alias\" : \"failure\", \"doc\": \"Failure message\" }" +
                  "]," +
                  "\"optional\": true," +
                  "\"default\": null" +
                  "}" +
                  "]" +
                  "}",
              translateToNull,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"result\", \"type\" : [ \"null\", { \"type\" : \"record\", \"name\" : \"fooResult\", \"fields\" : [ { \"name\" : \"success\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Success message\", \"default\" : null }, { \"name\" : \"failure\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Failure message\", \"default\" : null }, { \"name\" : \"fieldDiscriminator\", \"type\" : { \"type\" : \"enum\", \"name\" : \"fooResultDiscriminator\", \"symbols\" : [ \"null\", \"success\", \"failure\" ] }, \"doc\" : \"Contains the name of the field that has its value set.\" } ] } ], \"default\" : null } ] }",
              emptyFooSchema,
              emptyFooValue,
              "{\"result\": null}"
          },
          {
              // Two 'union with aliases' fields under different records but with the same field name. The generated record
              // representation for these two unions should include the parent record's name to avoid any name conflicts.
              "{" +
                  "\"type\": \"record\"," +
                  "\"name\": \"foo\"," +
                  "\"fields\": [" +
                  "{" +
                  "\"name\": \"bar\"," +
                  "\"type\": {" +
                  "\"type\": \"record\"," +
                  "\"name\": \"Bar\"," +
                  "\"fields\": [" +
                  "{" +
                  "\"name\": \"result\"," + // same union field name as the one below.
                  "\"type\": [ { \"type\" : \"string\", \"alias\" : \"resultUrn\" } ]" +
                  "}" +
                  "]" +
                  "}" +
                  "}," +
                  "{" +
                  "\"name\": \"baz\"," +
                  "\"type\": {" +
                  "\"type\": \"record\"," +
                  "\"name\": \"Baz\"," +
                  "\"fields\": [" +
                  "{" +
                  "\"name\": \"result\"," + // same union field name as the one above.
                  "\"type\": [ { \"type\" : \"string\", \"alias\" : \"resultUrn\" } ]" +
                  "}" +
                  "]" +
                  "}" +
                  "}" +
                  "]" +
                  "}",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"record\", \"name\" : \"Bar\", \"fields\" : [ { \"name\" : \"result\", \"type\" : { \"type\" : \"record\", \"name\" : \"BarResult\", \"fields\" : [ { \"name\" : \"resultUrn\", \"type\" : [ \"null\", \"string\" ], \"default\" : null }, { \"name\" : \"fieldDiscriminator\", \"type\" : { \"type\" : \"enum\", \"name\" : \"BarResultDiscriminator\", \"symbols\" : [ \"resultUrn\" ] }, \"doc\" : \"Contains the name of the field that has its value set.\" } ] } } ] } }, { \"name\" : \"baz\", \"type\" : { \"type\" : \"record\", \"name\" : \"Baz\", \"fields\" : [ { \"name\" : \"result\", \"type\" : { \"type\" : \"record\", \"name\" : \"BazResult\", \"fields\" : [ { \"name\" : \"resultUrn\", \"type\" : [ \"null\", \"string\" ], \"default\" : null }, { \"name\" : \"fieldDiscriminator\", \"type\" : { \"type\" : \"enum\", \"name\" : \"BazResultDiscriminator\", \"symbols\" : [ \"resultUrn\" ] }, \"doc\" : \"Contains the name of the field that has its value set.\" } ] } } ] } } ] }",
              null,
              null,
              null
          },
          {
              // An 'union with aliases' field containing a record member which has another 'union with aliases' field
              "{" +
                  "\"type\": \"record\"," +
                  "\"name\": \"foo\"," +
                  "\"fields\": [" +
                  "{" +
                  "\"name\": \"result\"," + // 'result' is an union field with just one member of type 'MessageRecord' record
                  "\"type\": [" +
                  "{ " +
                  "\"type\" : {" +
                  "\"type\": \"record\"," +
                  "\"name\": \"MessageRecord\"," +
                  "\"fields\": [" +
                  "{" +
                  "\"name\": \"message\"," + // 'message' is an union field under 'MessageRecord'
                  "\"type\": [" +
                  "{ \"type\" : \"string\", \"alias\" : \"success\", \"doc\": \"Success message\" }," +
                  "{ \"type\" : \"string\", \"alias\" : \"failure\", \"doc\": \"Failure message\" }" +
                  "]" +
                  "}" +
                  "]" +
                  "}," +
                  "\"alias\" : \"message\"" +
                  "}" +
                  "]" +
                  "}" +
                  "]" +
                  "}",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"result\", \"type\" : { \"type\" : \"record\", \"name\" : \"fooResult\", \"fields\" : [ { \"name\" : \"message\", \"type\" : [ \"null\", { \"type\" : \"record\", \"name\" : \"MessageRecord\", \"fields\" : [ { \"name\" : \"message\", \"type\" : { \"type\" : \"record\", \"name\" : \"MessageRecordMessage\", \"fields\" : [ { \"name\" : \"success\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Success message\", \"default\" : null }, { \"name\" : \"failure\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Failure message\", \"default\" : null }, { \"name\" : \"fieldDiscriminator\", \"type\" : { \"type\" : \"enum\", \"name\" : \"MessageRecordMessageDiscriminator\", \"symbols\" : [ \"success\", \"failure\" ] }, \"doc\" : \"Contains the name of the field that has its value set.\" } ] } } ] } ], \"default\" : null }, { \"name\" : \"fieldDiscriminator\", \"type\" : { \"type\" : \"enum\", \"name\" : \"fooResultDiscriminator\", \"symbols\" : [ \"message\" ] }, \"doc\" : \"Contains the name of the field that has its value set.\" } ] } } ] }",
              null,
              null,
              null
          },
          {
              // A required array field with 'union with aliases' as its item type and no default value
              "{" +
                  "\"type\": \"record\"," +
                  "\"name\": \"foo\"," +
                  "\"fields\": [" +
                  "{" +
                  "\"name\": \"results\"," +
                  "\"type\": ##T_START {" +
                  "\"type\": \"array\"," +
                  "\"items\": [" +
                  "{ \"type\" : \"string\", \"alias\" : \"success\", \"doc\": \"Success message\" }," +
                  "{ \"type\" : \"string\", \"alias\" : \"failure\", \"doc\": \"Failure message\" }" +
                  "]" +
                  "} ##T_END" +
                  "}" +
                  "]" +
                  "}",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"results\", \"type\" : { \"type\" : \"array\", \"items\" : { \"type\" : \"record\", \"name\" : \"fooResults\", \"fields\" : [ { \"name\" : \"success\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Success message\", \"default\" : null }, { \"name\" : \"failure\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Failure message\", \"default\" : null }, { \"name\" : \"fieldDiscriminator\", \"type\" : { \"type\" : \"enum\", \"name\" : \"fooResultsDiscriminator\", \"symbols\" : [ \"success\", \"failure\" ] }, \"doc\" : \"Contains the name of the field that has its value set.\" } ] } } } ] }",
              null,
              null,
              null
          },
          {
              // A required array field with 'union with aliases' as its item type and a default value
              "{" +
                  "\"type\": \"record\"," +
                  "\"name\": \"foo\"," +
                  "\"fields\": [" +
                  "{" +
                  "\"name\": \"results\"," +
                  "\"type\": {" +
                  "\"type\": \"array\"," +
                  "\"items\": [" +
                  "{ \"type\" : \"string\", \"alias\" : \"success\", \"doc\": \"Success message\" }," +
                  "{ \"type\" : \"string\", \"alias\" : \"failure\", \"doc\": \"Failure message\" }" +
                  "]" +
                  "}," +
                  "\"default\": [ { \"success\": \"Operation completed.\" }, { \"failure\": \"Operation failed.\" } ]" +
                  "}" +
                  "]" +
                  "}",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"results\", \"type\" : { \"type\" : \"array\", \"items\" : { \"type\" : \"record\", \"name\" : \"fooResults\", \"fields\" : [ { \"name\" : \"success\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Success message\", \"default\" : null }, { \"name\" : \"failure\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Failure message\", \"default\" : null }, { \"name\" : \"fieldDiscriminator\", \"type\" : { \"type\" : \"enum\", \"name\" : \"fooResultsDiscriminator\", \"symbols\" : [ \"success\", \"failure\" ] }, \"doc\" : \"Contains the name of the field that has its value set.\" } ] } } } ] }",
              null,
              null,
              null
          },
          {
              // An optional array field with 'union with aliases' as its item type and no default value
              "{" +
                  "\"type\": \"record\"," +
                  "\"name\": \"foo\"," +
                  "\"fields\": [" +
                  "{" +
                  "\"name\": \"results\"," +
                  "\"type\": {" +
                  "\"type\": \"array\"," +
                  "\"items\": [" +
                  "{ \"type\" : \"string\", \"alias\" : \"success\", \"doc\": \"Success message\" }," +
                  "{ \"type\" : \"string\", \"alias\" : \"failure\", \"doc\": \"Failure message\" }" +
                  "]" +
                  "}," +
                  "\"optional\": true" +
                  "}" +
                  "]" +
                  "}",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"results\", \"type\" : [ \"null\", { \"type\" : \"array\", \"items\" : { \"type\" : \"record\", \"name\" : \"fooResults\", \"fields\" : [ { \"name\" : \"success\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Success message\", \"default\" : null }, { \"name\" : \"failure\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Failure message\", \"default\" : null }, { \"name\" : \"fieldDiscriminator\", \"type\" : { \"type\" : \"enum\", \"name\" : \"fooResultsDiscriminator\", \"symbols\" : [ \"success\", \"failure\" ] }, \"doc\" : \"Contains the name of the field that has its value set.\" } ] } } ], \"default\" : null } ] }",
              null,
              null,
              null
          },
          {
              // An optional array field with 'union with aliases' as its item type and a default value
              "{" +
                  "\"type\": \"record\"," +
                  "\"name\": \"foo\"," +
                  "\"fields\": [" +
                  "{" +
                  "\"name\": \"results\"," +
                  "\"type\": {" +
                  "\"type\": \"array\"," +
                  "\"items\": [" +
                  "{ \"type\" : \"string\", \"alias\" : \"success\", \"doc\": \"Success message\" }," +
                  "{ \"type\" : \"string\", \"alias\" : \"failure\", \"doc\": \"Failure message\" }" +
                  "]" +
                  "}," +
                  "\"default\": [ { \"success\": \"Operation completed.\" }, { \"failure\": \"Operation failed.\" } ]," +
                  "\"optional\": true" +
                  "}" +
                  "]" +
                  "}",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"results\", \"type\" : [ \"null\", { \"type\" : \"array\", \"items\" : { \"type\" : \"record\", \"name\" : \"fooResults\", \"fields\" : [ { \"name\" : \"success\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Success message\", \"default\" : null }, { \"name\" : \"failure\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Failure message\", \"default\" : null }, { \"name\" : \"fieldDiscriminator\", \"type\" : { \"type\" : \"enum\", \"name\" : \"fooResultsDiscriminator\", \"symbols\" : [ \"success\", \"failure\" ] }, \"doc\" : \"Contains the name of the field that has its value set.\" } ] } } ], \"default\" : null } ] }",
              null,
              null,
              null
          },
          {
              // A nested array field with 'union with aliases' as its item type and a default value
              "{" +
                  "\"type\": \"record\"," +
                  "\"name\": \"foo\"," +
                  "\"fields\": [" +
                  "{" +
                  "\"name\": \"results\"," +
                  "\"type\": {" +
                  "\"type\": \"array\"," +
                  "\"items\": {" +
                  "\"type\": \"array\"," +
                  "\"items\": {" +
                  "\"type\": \"array\"," +
                  "\"items\": [" +
                  "{ \"type\" : \"string\", \"alias\" : \"success\", \"doc\": \"Success message\" }," +
                  "{ \"type\" : \"string\", \"alias\" : \"failure\", \"doc\": \"Failure message\" }" +
                  "]" +
                  "}" +
                  "}" +
                  "}," +
                  "\"default\": [ [ [ { \"success\": \"Operation completed.\" }, { \"failure\": \"Operation failed.\" } ] ] ]" +
                  "}" +
                  "]" +
                  "}",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"results\", \"type\" : { \"type\" : \"array\", \"items\" : { \"type\" : \"array\", \"items\" : { \"type\" : \"array\", \"items\" : { \"type\" : \"record\", \"name\" : \"fooResults\", \"fields\" : [ { \"name\" : \"success\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Success message\", \"default\" : null }, { \"name\" : \"failure\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Failure message\", \"default\" : null }, { \"name\" : \"fieldDiscriminator\", \"type\" : { \"type\" : \"enum\", \"name\" : \"fooResultsDiscriminator\", \"symbols\" : [ \"success\", \"failure\" ] }, \"doc\" : \"Contains the name of the field that has its value set.\" } ] } } } } } ] }",
              null,
              null,
              null
          },
          {
              // A nested array and map field with 'union with aliases' as its item type and a default value
              "{" +
                  "\"type\": \"record\"," +
                  "\"name\": \"foo\"," +
                  "\"fields\": [" +
                  "{" +
                  "\"name\": \"results\"," +
                  "\"type\": {" +
                  "\"type\": \"array\"," +
                  "\"items\": {" +
                  "\"type\": \"map\"," +
                  "\"values\": {" +
                  "\"type\": \"array\"," +
                  "\"items\": [" +
                  "{ \"type\" : \"string\", \"alias\" : \"success\", \"doc\": \"Success message\" }," +
                  "{ \"type\" : \"string\", \"alias\" : \"failure\", \"doc\": \"Failure message\" }" +
                  "]" +
                  "}" +
                  "}" +
                  "}," +
                  "\"default\": [ { \"key\": [ { \"success\": \"Operation completed.\" }, { \"failure\": \"Operation failed.\" } ] } ]" +
                  "}" +
                  "]" +
                  "}",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"results\", \"type\" : { \"type\" : \"array\", \"items\" : { \"type\" : \"map\", \"values\" : { \"type\" : \"array\", \"items\" : { \"type\" : \"record\", \"name\" : \"fooResults\", \"fields\" : [ { \"name\" : \"success\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Success message\", \"default\" : null }, { \"name\" : \"failure\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Failure message\", \"default\" : null }, { \"name\" : \"fieldDiscriminator\", \"type\" : { \"type\" : \"enum\", \"name\" : \"fooResultsDiscriminator\", \"symbols\" : [ \"success\", \"failure\" ] }, \"doc\" : \"Contains the name of the field that has its value set.\" } ] } } } } } ] }",
              null,
              null,
              null
          },
          {
              // A nested map field with 'union with aliases' as its item type and a default value
              "{" +
                  "\"type\": \"record\"," +
                  "\"name\": \"foo\"," +
                  "\"fields\": [" +
                  "{" +
                  "\"name\": \"results\"," +
                  "\"type\": {" +
                  "\"type\": \"map\"," +
                  "\"values\": {" +
                  "\"type\": \"map\"," +
                  "\"values\": {" +
                  "\"type\": \"map\"," +
                  "\"values\": [" +
                  "{ \"type\" : \"string\", \"alias\" : \"success\", \"doc\": \"Success message\" }," +
                  "{ \"type\" : \"string\", \"alias\" : \"failure\", \"doc\": \"Failure message\" }" +
                  "]" +
                  "}" +
                  "}" +
                  "}," +
                  "\"default\": { \"level1\": { \"level2\": { \"level3key1\": { \"success\": \"Operation completed.\" }, \"level3key2\": { \"failure\": \"Operation failed.\" } } } }" +
                  "}" +
                  "]" +
                  "}",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"results\", \"type\" : { \"type\" : \"map\", \"values\" : { \"type\" : \"map\", \"values\" : { \"type\" : \"map\", \"values\" : { \"type\" : \"record\", \"name\" : \"fooResults\", \"fields\" : [ { \"name\" : \"success\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Success message\", \"default\" : null }, { \"name\" : \"failure\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Failure message\", \"default\" : null }, { \"name\" : \"fieldDiscriminator\", \"type\" : { \"type\" : \"enum\", \"name\" : \"fooResultsDiscriminator\", \"symbols\" : [ \"success\", \"failure\" ] }, \"doc\" : \"Contains the name of the field that has its value set.\" } ] } } } } } ] }",
              null,
              null,
              null
          },
          {
              // A nested map and array field with 'union with aliases' as its item type and a default value
              "{" +
                  "\"type\": \"record\"," +
                  "\"name\": \"foo\"," +
                  "\"fields\": [" +
                  "{" +
                  "\"name\": \"results\"," +
                  "\"type\": {" +
                  "\"type\": \"map\"," +
                  "\"values\": {" +
                  "\"type\": \"array\"," +
                  "\"items\": {" +
                  "\"type\": \"map\"," +
                  "\"values\": [" +
                  "{ \"type\" : \"string\", \"alias\" : \"success\", \"doc\": \"Success message\" }," +
                  "{ \"type\" : \"string\", \"alias\" : \"failure\", \"doc\": \"Failure message\" }" +
                  "]" +
                  "}" +
                  "}" +
                  "}," +
                  "\"default\": { \"level1\": [ { \"level3key1\": { \"success\": \"Operation completed.\" }, \"level3key2\": { \"failure\": \"Operation failed.\" } } ] }" +
                  "}" +
                  "]" +
                  "}",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"results\", \"type\" : { \"type\" : \"map\", \"values\" : { \"type\" : \"array\", \"items\" : { \"type\" : \"map\", \"values\" : { \"type\" : \"record\", \"name\" : \"fooResults\", \"fields\" : [ { \"name\" : \"success\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Success message\", \"default\" : null }, { \"name\" : \"failure\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Failure message\", \"default\" : null }, { \"name\" : \"fieldDiscriminator\", \"type\" : { \"type\" : \"enum\", \"name\" : \"fooResultsDiscriminator\", \"symbols\" : [ \"success\", \"failure\" ] }, \"doc\" : \"Contains the name of the field that has its value set.\" } ] } } } } } ] }",
              null,
              null,
              null
          },
          {
              // A required map field with 'union with aliases' as its item type with no default value
              "{" +
                  "\"type\": \"record\"," +
                  "\"name\": \"foo\"," +
                  "\"fields\": [" +
                  "{" +
                  "\"name\": \"results\"," +
                  "\"type\": ##T_START {" +
                  "\"type\": \"map\"," +
                  "\"values\": [" +
                  "{ \"type\" : \"string\", \"alias\" : \"success\", \"doc\": \"Success message\" }," +
                  "{ \"type\" : \"string\", \"alias\" : \"failure\", \"doc\": \"Failure message\" }" +
                  "]" +
                  "} ##T_END" +
                  "}" +
                  "]" +
                  "}",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"results\", \"type\" : { \"type\" : \"map\", \"values\" : { \"type\" : \"record\", \"name\" : \"fooResults\", \"fields\" : [ { \"name\" : \"success\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Success message\", \"default\" : null }, { \"name\" : \"failure\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Failure message\", \"default\" : null }, { \"name\" : \"fieldDiscriminator\", \"type\" : { \"type\" : \"enum\", \"name\" : \"fooResultsDiscriminator\", \"symbols\" : [ \"success\", \"failure\" ] }, \"doc\" : \"Contains the name of the field that has its value set.\" } ] } } } ] }",
              null,
              null,
              null
          },
          {
              " { " +
              "   \"type\" : \"record\", " +
              "   \"name\" : \"Foo\", " +
              "   \"fields\" : [ { " +
              "     \"name\" : \"field1\", " +
              "     \"type\" : \"int\", " +
              "     \"b_customerAnnotation\" : \"f1\", " +
              "     \"c_customerAnnotation\" : \"f1\", " +
              "     \"a_customerAnnotation\" : \"f1\" " +
              "   } ] " +
              " } ",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"Foo\", \"fields\" : [ { \"name\" : \"field1\", \"type\" : \"int\", \"a_customerAnnotation\" : \"f1\", \"b_customerAnnotation\" : \"f1\", \"c_customerAnnotation\" : \"f1\" } ] }",
              null,
              null,
              null
          },
          {
              " { " +
              "   \"type\" : \"record\", " +
              "   \"name\" : \"Foo\", " +
              "   \"fields\" : [ { " +
              "     \"name\" : \"field1\", " +
              "     \"type\" : \"int\", " +
              "     \"c_customerAnnotation\" : { " +
              "       \"b_nested\" : \"a\", " +
              "       \"a_nested\" : \"a\", " +
              "       \"c_nested\" : \"a\" " +
              "     }, " +
              "     \"a_customerAnnotation\" : \"f1\", " +
              "     \"b_customerAnnotation\" : \"f1\" " +
              "   } ] " +
              " } ",
              allModes,
              "{ \"type\" : \"record\", \"name\" : \"Foo\", \"fields\" : [ { \"name\" : \"field1\", \"type\" : \"int\", \"a_customerAnnotation\" : \"f1\", \"b_customerAnnotation\" : \"f1\", \"c_customerAnnotation\" : { \"a_nested\" : \"a\", \"b_nested\" : \"a\", \"c_nested\" : \"a\" } } ] }",
              null,
              null,
              null
          }
      };


  }

  @Test(dataProvider = "toAvroSchemaData")
  public void testToAvroSchema(String schemaText,
      OptionalDefaultMode[] optionalDefaultModes,
      String expected,
      String writerSchemaText,
      String avroValueJson,
      String expectedGenericRecordJson) throws IOException
  {
    // test generating Avro schema from Pegasus schema
    if (schemaText.contains("##T_START"))
    {
      assertTrue(schemaText.contains("##T_END"));
      String noTyperefSchemaText = schemaText.replace("##T_START", "").replace("##T_END", "");
      assertFalse(noTyperefSchemaText.contains("##T_"));
      assertFalse(noTyperefSchemaText.contains("typeref"));
      String typerefSchemaText = schemaText
        .replace("##T_START", "{ \"type\" : \"typeref\", \"name\" : \"Ref\", \"ref\" : ")
        .replace("##T_END", "}");
      assertFalse(typerefSchemaText.contains("##T_"));
      assertTrue(typerefSchemaText.contains("typeref"));
      testToAvroSchemaInternal(noTyperefSchemaText, optionalDefaultModes, expected, writerSchemaText, avroValueJson, expectedGenericRecordJson);
      testToAvroSchemaInternal(typerefSchemaText, optionalDefaultModes, expected, writerSchemaText, avroValueJson, expectedGenericRecordJson);
    }
    else
    {
      assertFalse(schemaText.contains("##"));
      testToAvroSchemaInternal(schemaText, optionalDefaultModes, expected, writerSchemaText, avroValueJson, expectedGenericRecordJson);
    }
  }

  private void testToAvroSchemaInternal(String schemaText,
      OptionalDefaultMode[] optionalDefaultModes,
      String expected,
      String writerSchemaText,
      String avroValueJson,
      String expectedGenericRecordJson) throws IOException
  {
    for (EmbedSchemaMode embedSchemaMode : EmbedSchemaMode.values())
    {
      for (OptionalDefaultMode optionalDefaultMode : optionalDefaultModes)
      {
        DataSchema schema = TestUtil.dataSchemaFromString(schemaText);
        String preTranslateSchemaText = schema.toString();
        String avroTextFromSchema = null;
        DataToAvroSchemaTranslationOptions transOptions = new DataToAvroSchemaTranslationOptions(optionalDefaultMode, JsonBuilder.Pretty.SPACES, embedSchemaMode);
        transOptions.setTyperefPropertiesExcludeSet(new HashSet<>(Arrays.asList("validate", "java")));
        avroTextFromSchema = SchemaTranslator.dataToAvroSchemaJson(
          schema, transOptions);

        if (embedSchemaMode == EmbedSchemaMode.ROOT_ONLY && hasEmbeddedSchema(schema))
        {
          // when embeddedSchema is enabled
          // for map, array, enums. and records, we embed the original Pegasus schema
          DataMap expectedAvroDataMap = TestUtil.dataMapFromString(expected);
          DataMap resultAvroDataMap = TestUtil.dataMapFromString(avroTextFromSchema);
          Object dataProperty = resultAvroDataMap.remove(SchemaTranslator.DATA_PROPERTY);
          assertEquals(resultAvroDataMap, expectedAvroDataMap);

          // look for embedded schema
          assertNotNull(dataProperty);
          assertTrue(dataProperty instanceof DataMap);
          Object schemaProperty = ((DataMap) dataProperty).get(SchemaTranslator.SCHEMA_PROPERTY);
          assertNotNull(schemaProperty);
          assertTrue(schemaProperty instanceof DataMap);

          // make sure embedded schema is same as the original schema
          PegasusSchemaParser schemaParser = TestUtil.schemaParserFromObjects(Arrays.asList(schemaProperty));
          DataSchema embeddedSchema = schemaParser.topLevelDataSchemas().get(0);
          assertEquals(embeddedSchema, schema.getDereferencedDataSchema());

          // look for optional default mode
          Object optionalDefaultModeProperty = ((DataMap) dataProperty).get(SchemaTranslator.OPTIONAL_DEFAULT_MODE_PROPERTY);
          assertNotNull(optionalDefaultMode);
          assertEquals(optionalDefaultModeProperty, optionalDefaultMode.toString());
        }
        else
        {
          // embeddedSchema is not enabled or
          // for unions and primitives, we never embed the pegasus schema
          if (embedSchemaMode == EmbedSchemaMode.NONE && hasEmbeddedSchema(schema))
          {
            // make sure no embedded schema when
            DataMap resultAvroDataMap = TestUtil.dataMapFromString(avroTextFromSchema);
            assertFalse(resultAvroDataMap.containsKey(SchemaTranslator.DATA_PROPERTY));
          }
          assertEquals(avroTextFromSchema, expected);
        }

        String postTranslateSchemaText = schema.toString();
        assertEquals(postTranslateSchemaText, preTranslateSchemaText);

        // make sure Avro accepts it
        Schema avroSchema = Schema.parse(avroTextFromSchema);

        SchemaParser parser = new SchemaParser();
        ValidationOptions options = new ValidationOptions();
        options.setAvroUnionMode(true);
        parser.setValidationOptions(options);
        parser.parse(avroTextFromSchema);
        assertFalse(parser.hasError(), parser.errorMessage());

        if (optionalDefaultMode == DataToAvroSchemaTranslationOptions.DEFAULT_OPTIONAL_DEFAULT_MODE)
        {
          // use other dataToAvroSchemaJson
          String avroSchema2Json = SchemaTranslator.dataToAvroSchemaJson(
            TestUtil.dataSchemaFromString(schemaText)
          );
          String avroSchema2JsonCompact = SchemaTranslator.dataToAvroSchemaJson(
            TestUtil.dataSchemaFromString(schemaText),
            new DataToAvroSchemaTranslationOptions()
          );
          assertEquals(avroSchema2Json, avroSchema2JsonCompact);
          Schema avroSchema2 = Schema.parse(avroSchema2Json);
          assertEquals(avroSchema2, avroSchema);

          // use dataToAvroSchema
          Schema avroSchema3 = SchemaTranslator.dataToAvroSchema(TestUtil.dataSchemaFromString(schemaText));
          assertEquals(avroSchema3, avroSchema2);
        }

        if (writerSchemaText != null || avroValueJson != null)
        {
          // check if the translated default value is good by using it.
          // writer schema and Avro JSON value should not include fields with default values.
          Schema writerSchema = Schema.parse(writerSchemaText);
          GenericRecord genericRecord = genericRecordFromString(avroValueJson, writerSchema, avroSchema);

          if (expectedGenericRecordJson != null)
          {
            String genericRecordAsString = genericRecord.toString();
            assertEquals(genericRecordAsString, TestAvroUtil.serializedEnumValueProcessor(expectedGenericRecordJson));
          }
        }

        if (embedSchemaMode == EmbedSchemaMode.ROOT_ONLY && hasEmbeddedSchema(schema))
        {
          // if embedded schema is enabled, translate Avro back to Pegasus schema.
          // the output Pegasus schema should be exactly same the input schema
          // taking into account typeref.
          AvroToDataSchemaTranslationOptions avroToDataSchemaMode = new AvroToDataSchemaTranslationOptions(AvroToDataSchemaTranslationMode.VERIFY_EMBEDDED_SCHEMA);
          DataSchema embeddedSchema = SchemaTranslator.avroToDataSchema(avroTextFromSchema, avroToDataSchemaMode);
          assertEquals(embeddedSchema, schema.getDereferencedDataSchema());
        }
      }
    }
  }

  @DataProvider
  public Object[][] pegasusDefaultToAvroOptionalSchemaTranslationProvider() {
    return new String[][] {
        {
            // union type with default
            "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START [\"int\", \"string\"] ##T_END, \"default\" : { \"int\" : 42 } } ] }",
            "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\", \"string\" ], \"default\" : null } ] }",
        },
        {
            // enum type with default
            "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START { \"type\" : \"enum\", \"name\" : \"fruits\", \"symbols\" : [ \"APPLE\", \"ORANGE\" ] } ##T_END,  \"default\" : \"APPLE\" } ] }",
            "{\"type\":\"record\",\"name\":\"foo\",\"fields\":[{\"name\":\"bar\",\"type\":[\"null\",{\"type\":\"enum\",\"name\":\"fruits\",\"symbols\":[\"APPLE\",\"ORANGE\"]}],\"default\":null}]}"
        },
        {
            // required and has default
            "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START \"int\" ##T_END, \"default\" : 42 } ] }",
            "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\" ], \"default\" : null } ] }",
        },
        {
            // required, optional is false
            "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START \"int\" ##T_END, \"optional\" : false } ] }",
            "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ] }",
        },
        {
            // required, optional is false and has default
            "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START \"int\" ##T_END, \"default\" : 42, \"optional\" : false } ] }",
            "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"int\" ], \"default\" : null } ] }",
        }
    };
  }


  @Test(dataProvider = "pegasusDefaultToAvroOptionalSchemaTranslationProvider",
        description = "Test schemaTranslator for default fields to optional fields translation, in different schema translation modes")
  public void testPegasusDefaultToAvroOptionalSchemaTranslation(String... testSchemaTextAndExpected) throws IOException
  {

    String schemaText = null;
    String expectedAvroSchema = null;
    DataMap resultAvroDataMap = null;
    DataMap expectedAvroDataMap = null;
    schemaText = testSchemaTextAndExpected[0];
    expectedAvroSchema = testSchemaTextAndExpected[1];
    List<String> schemaTextForTesting = null;

    if (schemaText.contains("##T_START")) {
      String noTyperefSchemaText = schemaText.replace("##T_START", "").replace("##T_END", "");
      String typerefSchemaText = schemaText
          .replace("##T_START", "{ \"type\" : \"typeref\", \"name\" : \"Ref\", \"ref\" : ")
          .replace("##T_END", "}");
      schemaTextForTesting = Arrays.asList(noTyperefSchemaText, typerefSchemaText);
    }
    else {
      schemaTextForTesting = Arrays.asList(schemaText);
    }

    for (String schemaStringText: schemaTextForTesting) {
      DataSchema schema = TestUtil.dataSchemaFromString(schemaStringText);
      String avroTextFromSchema = null;
      avroTextFromSchema = SchemaTranslator.dataToAvroSchemaJson(
          schema,
          new DataToAvroSchemaTranslationOptions(PegasusToAvroDefaultFieldTranslationMode.DO_NOT_TRANSLATE)
      );
      resultAvroDataMap = TestUtil.dataMapFromString(avroTextFromSchema);
      expectedAvroDataMap = TestUtil.dataMapFromString(expectedAvroSchema);
      assertEquals(resultAvroDataMap, expectedAvroDataMap);

      // Test avro Schema
      Schema avroSchema = Schema.parse(avroTextFromSchema);

      // Test validation parsing
      SchemaParser parser = new SchemaParser();
      ValidationOptions options = new ValidationOptions();
      options.setAvroUnionMode(true);
      parser.setValidationOptions(options);
      parser.parse(avroTextFromSchema);
      assertFalse(parser.hasError(), parser.errorMessage());
    }
  }

  @DataProvider
  public Object[][] toAvroSchemaErrorData()
  {
    final OptionalDefaultMode allModes[] = { OptionalDefaultMode.TRANSLATE_DEFAULT, OptionalDefaultMode.TRANSLATE_TO_NULL };
    final OptionalDefaultMode translateDefault[] = { OptionalDefaultMode.TRANSLATE_DEFAULT };

    return new Object[][]
        {
            // {
            //   1st element is the Pegasus schema in JSON.
            //     The string may be marked with ##T_START and ##T_END markers. The markers are used for typeref testing.
            //     If the string these markers, then two schemas will be constructed and tested.
            //     The first schema replaces these markers with two empty strings.
            //     The second schema replaces these markers with a typeref enclosing the type between these markers.
            //   Each following element is an Object array,
            //     1st element of this array is an array of OptionalDefaultMode's to be used for default translation.
            //     2nd element is either a string or an Exception.
            //       If it is a string, it is the expected output Avro schema in JSON.
            //         If there are 3rd and 4th elements, then the 3rd element is an Avro schema used to write the 4th element
            //         which is JSON serialized Avro data. Usually, this is used to make sure that the translated default
            //         value is valid for Avro. Avro does not validate the default value in the schema. It will only
            //         de-serialize (and validate) the default value when it is actually used. The writer schema and
            //         the JSON serialized Avro data should not include fields with default values. The 4th element may be
            //         marked with ##Q_START and ##Q_END around enum values. On Avro v1.4, the GenericRecord#toString() does
            //         wrap enum values with quotes but it does on v1.6. These markers are used to handle this.
            //       If it is an Exception, then the Pegasus schema cannot be translated and this is the exception that
            //         is expected. The 3rd element is a string that should be contained in the message of the exception.
            // }
            {
                // optional and has default but with circular references with inconsistent defaults, inconsistent because optional field has default, and also missing (which requires default to be null)
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"foo\", \"default\" : {  }, \"optional\" : true } ] }",
                translateDefault,
                IllegalArgumentException.class,
                "cannot translate absent optional field (to have null value) because this field is optional and has a default value"
            },
            {
                // optional and has default but with circular references with inconsistent defaults, inconsistent because optional field has default, and also missing (which requires default to be null)
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"foo\", \"default\" : { \"bar\" : {  } }, \"optional\" : true } ] }",
                translateDefault,
                IllegalArgumentException.class,
                "cannot translate absent optional field (to have null value) because this field is optional and has a default value"
            },
            {
                // optional union without null and default is 2nd member type
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START [ \"int\", \"string\" ] ##T_END, \"default\" : { \"string\" : \"abc\" }, \"optional\" : true } ] }",
                translateDefault,
                IllegalArgumentException.class,
                "cannot translate union value"
            },
            {
                // optional union with null and non-null default, default is 2nd member type
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START [ \"int\", \"null\", \"string\" ] ##T_END, \"default\" : null, \"optional\" : true } ] }",
                translateDefault,
                IllegalArgumentException.class,
                "cannot translate union value"
            },
            {
                // optional union with null and non-null default, default is 3rd member type
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START [ \"int\", \"null\", \"string\" ] ##T_END, \"default\" : { \"string\" : \"abc\" }, \"optional\" : true } ] }",
                translateDefault,
                IllegalArgumentException.class,
                "cannot translate union value"
            },
            {
                // optional union but with circular references with inconsistent defaults, inconsistent because optional field has default, and also missing (which requires default to be null)
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START [ \"foo\", \"string\" ] ##T_END, \"default\" : { \"foo\" : { } }, \"optional\" : true } ] }",
                translateDefault,
                IllegalArgumentException.class,
                "cannot translate absent optional field (to have null value) or field with non-null union value because this field is optional and has a non-null default value",
            },
            {
                // record field with union with typeref, without null and default is 2nd member type and not typeref-ed
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ ##T_START \"int\" ##T_END, \"string\" ], \"default\" : { \"string\" : \"abc\" } } ] }",
                allModes,
                IllegalArgumentException.class,
                "cannot translate union value"
            },
            {
                // record field with union with typeref, without null and default is 2nd member type and typeref-ed
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"string\", ##T_START \"int\" ##T_END ], \"default\" : { \"int\" : 42 } } ] }",
                allModes,
                IllegalArgumentException.class,
                "cannot translate union value"
            },
            {
                // record field with union with typeref, without null and optional, default is 2nd member and not typeref-ed
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ ##T_START \"int\" ##T_END, \"string\" ], \"optional\" : true, \"default\" : { \"string\" : \"abc\" } } ] }",
                translateDefault,
                IllegalArgumentException.class,
                "cannot translate union value"
            },
            {
                // record field with union with typeref, without null and optional, default is 2nd member and typeref-ed
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"string\", ##T_START \"int\" ##T_END ], \"optional\" : true, \"default\" : { \"int\" : 42 } } ] }",
                translateDefault,
                IllegalArgumentException.class,
                "cannot translate union value"
            },
            {
                // record field with union with typeref, with null 1st member, default is last member and typeref-ed
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", ##T_START \"int\" ##T_END ], \"default\" : { \"int\" : 42 } } ] }",
                allModes,
                IllegalArgumentException.class,
                "cannot translate union value"
            },
            {
                // record field with union with typeref with null 1st member, and optional, default is last member and typeref-ed
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", ##T_START \"int\" ##T_END ], \"optional\" : true, \"default\" : { \"int\" : 42 } } ] }",
                translateDefault,
                IllegalArgumentException.class,
                "cannot translate union value"
            },
            {
                // record field with union with typeref, with null last member, default is last member and null
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ ##T_START \"int\" ##T_END, \"null\" ], \"default\" : null } ] }",
                allModes,
                IllegalArgumentException.class,
                "cannot translate union value"
            },
            {
                // record field with union with typeref, with null last member, and optional, default is last member and null
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ ##T_START \"int\" ##T_END, \"null\" ], \"optional\" : true, \"default\" : null } ] }",
                translateDefault,
                IllegalArgumentException.class,
                "cannot translate union value"
            },
            {
                // array of union with default, default value uses 2nd member type
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START { \"type\" : \"array\", \"items\" : [ \"int\", \"string\" ] } ##T_END, \"default\" : [ { \"int\" : 42 }, { \"string\" : \"abc\" } ] } ] }",
                allModes,
                IllegalArgumentException.class,
                "cannot translate union value"
            },
            {
                // optional array of union with default, default value uses 2nd member type
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"array\", \"items\" : ##T_START [ \"int\", \"string\" ] ##T_END }, \"optional\" : true, \"default\" : [ { \"int\" : 42 }, { \"string\" : \"abc\" } ] } ] }",
                translateDefault,
                IllegalArgumentException.class,
                "cannot translate union value"
            },
            {
                // map of union with default, default value uses 2nd member type
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : ##T_START { \"type\" : \"map\", \"values\" : [ \"int\", \"string\" ] } ##T_END, \"default\" : { \"m1\" : { \"string\" : \"abc\" } } } ] }",
                allModes,
                IllegalArgumentException.class,
                "cannot translate union value"
            },
            {
                // optional map of union with default, default value uses 2nd member type
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"values\" : ##T_START [ \"int\", \"string\" ] ##T_END }, \"optional\" : true, \"default\" : { \"m1\" : { \"string\" : \"abc\" } } } ] }",
                translateDefault,
                IllegalArgumentException.class,
                "cannot translate union value"
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
                translateDefault,
                IllegalArgumentException.class,
                "cannot translate absent optional field (to have null value) because this field is optional and has a default value"
            },
            {
                // inconsistent default,
                // a referenced record has an optional field "bar1" without default which translates with union with null as 1st member
                // but field of referenced record type has default value and it provides string value for "bar1"
                "{\n" +
                    "  \"type\":\"record\",\n" +
                    "  \"name\":\"foo\",\n" +
                    "  \"fields\":[\n" +
                    "    {\n" +
                    "      \"name\": \"foo1\",\n" +
                    "      \"type\": {\n" +
                    "        \"type\" : \"record\",\n" +
                    "        \"name\" : \"bar\",\n" +
                    "        \"fields\" : [\n" +
                    "           {\n" +
                    "             \"name\" : \"bar1\",\n" +
                    "             \"type\" : \"string\",\n" +
                    "             \"optional\" : true\n" +
                    "           }\n" +
                    "        ]\n" +
                    "      },\n" +
                    "      \"optional\": true,\n" +
                    "      \"default\": { \"bar1\": \"US\" }\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}\n",
                translateDefault,
                IllegalArgumentException.class,
                "cannot translate field because its default value's type is not the same as translated field's first union member's type"
            }
        };
  }

  @Test(dataProvider = "toAvroSchemaErrorData")
  public void testToAvroSchemaError(String schemaText, OptionalDefaultMode[] optionalDefaultModes, Class<?> expectedExceptionClass, String expectedString) throws IOException
  {
    // test generating Avro schema from Pegasus schema
    if (schemaText.contains("##T_START"))
    {
      assertTrue(schemaText.contains("##T_END"));
      String noTyperefSchemaText = schemaText.replace("##T_START", "").replace("##T_END", "");
      assertFalse(noTyperefSchemaText.contains("##T_"));
      assertFalse(noTyperefSchemaText.contains("typeref"));
      String typerefSchemaText = schemaText
          .replace("##T_START", "{ \"type\" : \"typeref\", \"name\" : \"Ref\", \"ref\" : ")
          .replace("##T_END", "}");
      assertFalse(typerefSchemaText.contains("##T_"));
      assertTrue(typerefSchemaText.contains("typeref"));
      testToAvroSchemaErrorInternal(noTyperefSchemaText, optionalDefaultModes, expectedExceptionClass, expectedString);
      testToAvroSchemaErrorInternal(typerefSchemaText, optionalDefaultModes, expectedExceptionClass, expectedString);
    }
    else
    {
      assertFalse(schemaText.contains("##"));
      testToAvroSchemaErrorInternal(schemaText, optionalDefaultModes, expectedExceptionClass, expectedString);
    }
  }

  private void testToAvroSchemaErrorInternal(String schemaText, OptionalDefaultMode[] optionalDefaultModes, Class<?> expectedExceptionClass, String expectedMessage) throws IOException
  {
    for (EmbedSchemaMode embedSchemaMode : EmbedSchemaMode.values())
    {
      for (OptionalDefaultMode optionalDefaultMode : optionalDefaultModes)
      {
        DataSchema schema = TestUtil.dataSchemaFromString(schemaText);
        String avroTextFromSchema = null;
        try
        {
          avroTextFromSchema = SchemaTranslator.dataToAvroSchemaJson(
              schema,
              new DataToAvroSchemaTranslationOptions(optionalDefaultMode, JsonBuilder.Pretty.SPACES, embedSchemaMode)
          );
        }
        catch (Exception e)
        {
          assertNotNull(e);
          assertTrue(expectedExceptionClass.isInstance(e));
          assertTrue(e.getMessage().contains(expectedMessage), "\"" + e.getMessage() + "\" does not contain \"" + expectedMessage + "\"");

          continue;
        }

        fail("Expect exception: " + expectedExceptionClass);
      }
    }
  }

  private static boolean hasEmbeddedSchema(DataSchema schema)
  {
    DataSchema.Type type = schema.getDereferencedType();
    return type == DataSchema.Type.ARRAY ||
           type == DataSchema.Type.MAP ||
           type == DataSchema.Type.ENUM ||
           type == DataSchema.Type.FIXED ||
           type == DataSchema.Type.RECORD;
  }

  @DataProvider
  public Object[][] embeddingSchemaWithDataPropertyData()
  {
    return new Object[][]
        {
            {
                // already has "com.linkedin.data" property but it is not a DataMap, replace with DataMap.
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ], \"com.linkedin.data\" : 1 }",
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ], \"com.linkedin.data\" : { \"schema\" : { \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ], \"com.linkedin.data\" : 1 }, \"optionalDefaultMode\" : \"TRANSLATE_DEFAULT\" } }"
            },
            {
                // already has "com.linkedin.data" property
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ], \"com.linkedin.data\" : {} }",
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ], \"com.linkedin.data\" : { \"schema\" : { \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ], \"com.linkedin.data\" : {  } }, \"optionalDefaultMode\" : \"TRANSLATE_DEFAULT\" } }"
            },
            {
                // already has "com.linkedin.data" property containing "extra" property, "extra" property is reserved in translated schema
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ], \"com.linkedin.data\" : { \"extra\" : 2 } }",
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ], \"com.linkedin.data\" : { \"schema\" : { \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ], \"com.linkedin.data\" : { \"extra\" : 2 } }, \"optionalDefaultMode\" : \"TRANSLATE_DEFAULT\", \"extra\" : 2 } }"
            },
            {
                // already has "com.linkedin.data" property containing reserved "schema" property, "schema" property is replaced in translated schema
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ], \"com.linkedin.data\" : { \"schema\" : 2 } }",
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ], \"com.linkedin.data\" : { \"schema\" : { \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ], \"com.linkedin.data\" : { \"schema\" : 2 } }, \"optionalDefaultMode\" : \"TRANSLATE_DEFAULT\" } }"
            },
            {
                // already has "com.linkedin.data" property containing reserved "optionalDefaultMode" property, "optionalDefaultMode" property is replaced in translated schema
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ], \"com.linkedin.data\" : { \"optionalDefaultMode\" : 2 } }",
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ], \"com.linkedin.data\" : { \"schema\" : { \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ], \"com.linkedin.data\" : { \"optionalDefaultMode\" : 2 } }, \"optionalDefaultMode\" : \"TRANSLATE_DEFAULT\" } }"
            }
        };
  }

  @Test(dataProvider = "embeddingSchemaWithDataPropertyData")
  public void testEmbeddingSchemaWithDataProperty(String schemaText, String expected) throws IOException
  {
    DataToAvroSchemaTranslationOptions options = new DataToAvroSchemaTranslationOptions(JsonBuilder.Pretty.SPACES, EmbedSchemaMode.ROOT_ONLY);
    String avroSchemaJson = SchemaTranslator.dataToAvroSchemaJson(TestUtil.dataSchemaFromString(schemaText), options);
    DataMap avroSchemaDataMap = TestUtil.dataMapFromString(avroSchemaJson);
    DataMap expectedDataMap = TestUtil.dataMapFromString(expected);
    assertEquals(avroSchemaDataMap, expectedDataMap);
  }

  @DataProvider
  public Object[][] schemaWithNamespaceOverride()
  {
    return new Object[][]
        {
            {
                // no namespace
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ]}",
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"namespace\" : \"avro\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ] }"
            },
            {
                // namespace inside name
                "{ \"type\" : \"record\", \"name\" : \"x.y.foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ]}",
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"namespace\" : \"avro.x.y\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ] }"
            },
            {
                // exist namespace
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"namespace\" : \"x.y\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ]}",
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"namespace\" : \"avro.x.y\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ] }"
            },
            {
              // reference to inlined schema
                "{" +
                    "  \"type\": \"record\"," +
                    "  \"name\": \"foo\"," +
                    "  \"namespace\": \"x.y\"," +
                    "  \"fields\": [" +
                    "    {" +
                    "      \"name\": \"bar\"," +
                    "      \"type\": \"int\"" +
                    "    }," +
                    "    {" +
                    "      \"name\": \"FirstPost\"," +
                    "      \"type\": {" +
                    "        \"type\": \"record\"," +
                    "        \"name\": \"Date\"," +
                    "        \"namespace\": \"x.y\"," +
                    "        \"fields\": [" +
                    "          {" +
                    "            \"name\": \"day\"," +
                    "            \"type\": \"int\"" +
                    "          }," +
                    "          {" +
                    "            \"name\": \"month\"," +
                    "            \"type\": \"int\"" +
                    "          }," +
                    "          {" +
                    "            \"name\": \"year\"," +
                    "            \"type\": \"int\"" +
                    "          }" +
                    "        ]" +
                    "      }" +
                    "    }," +
                    "    {" +
                    "      \"name\": \"SecondPost\"," +
                    "      \"type\": \"Date\"" +
                    "    }," +
                    "    {" +
                    "      \"name\": \"LastPost\"," +
                    "      \"type\": \"x.y.Date\"" +
                    "    }" +
                    "  ]" +
                    "}",
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"namespace\" : \"avro.x.y\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" }," +
                    " { \"name\" : \"FirstPost\", \"type\" : { \"type\" : \"record\", \"name\" : \"Date\", \"fields\" : [ { \"name\" : \"day\", \"type\" : \"int\" }, " +
                    "{ \"name\" : \"month\", \"type\" : \"int\" }, { \"name\" : \"year\", \"type\" : \"int\" } ] } }," +
                    " { \"name\" : \"SecondPost\", \"type\" : \"avro.x.y.Date\" }, { \"name\" : \"LastPost\", \"type\" : \"avro.x.y.Date\" } ] }"
            }
        };
  }

  @Test(dataProvider = "schemaWithNamespaceOverride")
  public void testSchemaWithNamespaceOverride(String schemaText, String expected) throws IOException
  {
    DataToAvroSchemaTranslationOptions options = new DataToAvroSchemaTranslationOptions(JsonBuilder.Pretty.SPACES).setOverrideNamespace(true);
    String avroSchemaJson = SchemaTranslator.dataToAvroSchemaJson(TestUtil.dataSchemaFromString(schemaText), options);
    assertEquals(avroSchemaJson, expected);
  }

  @DataProvider
  public Object[][] fromAvroSchemaData()
  {
    return new Object[][]
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
                // Union with a default value for its record type member. The converted Pegasus union's default value should use the fully qualified name of the record as its member key.
                "{ \"type\" : \"record\", \"name\" : \"a.b.c.foo\", \"fields\" : [ { \"name\" : \"fooField\", \"type\" : [ { \"type\" : \"record\", \"name\" : \"x.y.z.bar\", \"fields\" : [ { \"name\" : \"barField\", \"type\" : \"string\" } ] }, \"int\" ], \"default\" : { \"barField\" : \"Union with an inlined record member\" } } ] }",
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"namespace\" : \"a.b.c\", \"fields\" : [ { \"name\" : \"fooField\", \"type\" : [ { \"type\" : \"record\", \"name\" : \"bar\", \"namespace\" : \"x.y.z\", \"fields\" : [ { \"name\" : \"barField\", \"type\" : \"string\" } ] }, \"int\" ], \"default\" : { \"x.y.z.bar\" : { \"barField\" : \"Union with an inlined record member\" } } } ] }"
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
            {
                // Avro schema containing a record translated from a required Pegasus union with no default value.
                // Translated union member property contains default value.
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"result\", \"type\" : { \"type\" : \"record\", \"name\" : \"fooResult\", \"fields\" : [ { \"name\" : \"success\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Success message\", \"default\" : null }, { \"name\" : \"failure\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Failure message\", \"default\" : null }, { \"name\" : \"fieldDiscriminator\", \"type\" : { \"type\" : \"enum\", \"name\" : \"fooResultDiscriminator\", \"symbols\" : [ \"success\", \"failure\" ] }, \"doc\" : \"Contains the name of the field that has its value set.\" } ] } } ] }",
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"result\", \"type\" : { \"type\" : \"record\", \"name\" : \"fooResult\", \"fields\" : [ { \"name\" : \"success\", \"type\" : \"string\", \"doc\" : \"Success message\", \"optional\" : true }, { \"name\" : \"failure\", \"type\" : \"string\", \"doc\" : \"Failure message\", \"optional\" : true }, { \"name\" : \"fieldDiscriminator\", \"type\" : { \"type\" : \"enum\", \"name\" : \"fooResultDiscriminator\", \"symbols\" : [ \"success\", \"failure\" ] }, \"doc\" : \"Contains the name of the field that has its value set.\" } ] } } ] }"
            },
            {
                // Avro schema containing a record translated from an optional Pegasus union with no default value.
                // Translated union member property contains default value.
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"result\", \"type\" : [ \"null\", { \"type\" : \"record\", \"name\" : \"fooResult\", \"fields\" : [ { \"name\" : \"success\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Success message\", \"default\" : null }, { \"name\" : \"failure\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Failure message\", \"default\" : null }, { \"name\" : \"fieldDiscriminator\", \"type\" : { \"type\" : \"enum\", \"name\" : \"fooResultDiscriminator\", \"symbols\" : [ \"success\", \"failure\" ] }, \"doc\" : \"Contains the name of the field that has its value set.\" } ] } ], \"default\" : null } ] }",
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"result\", \"type\" : { \"type\" : \"record\", \"name\" : \"fooResult\", \"fields\" : [ { \"name\" : \"success\", \"type\" : \"string\", \"doc\" : \"Success message\", \"optional\" : true }, { \"name\" : \"failure\", \"type\" : \"string\", \"doc\" : \"Failure message\", \"optional\" : true }, { \"name\" : \"fieldDiscriminator\", \"type\" : { \"type\" : \"enum\", \"name\" : \"fooResultDiscriminator\", \"symbols\" : [ \"success\", \"failure\" ] }, \"doc\" : \"Contains the name of the field that has its value set.\" } ] }, \"optional\" : true } ] }"
            },
            {
                // Avro schema containing a record translated from a required Pegasus union with a default value.
                // Translated union member property contains default value.
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"result\", \"type\" : { \"type\" : \"record\", \"name\" : \"fooResult\", \"fields\" : [ { \"name\" : \"success\", \"type\" : [ \"string\", \"null\" ], \"doc\" : \"Success message\", \"default\" : \"Union with aliases.\" }, { \"name\" : \"failure\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Failure message\", \"default\" : null }, { \"name\" : \"fieldDiscriminator\", \"type\" : { \"type\" : \"enum\", \"name\" : \"fooResultDiscriminator\", \"symbols\" : [ \"success\", \"failure\" ] }, \"doc\" : \"Contains the name of the field that has its value set.\" } ] }, \"default\" : { \"fieldDiscriminator\" : \"success\", \"success\" : \"Union with aliases.\", \"failure\" : null } } ] }",
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"result\", \"type\" : { \"type\" : \"record\", \"name\" : \"fooResult\", \"fields\" : [ { \"name\" : \"success\", \"type\" : \"string\", \"doc\" : \"Success message\", \"default\" : \"Union with aliases.\", \"optional\" : true }, { \"name\" : \"failure\", \"type\" : \"string\", \"doc\" : \"Failure message\", \"optional\" : true }, { \"name\" : \"fieldDiscriminator\", \"type\" : { \"type\" : \"enum\", \"name\" : \"fooResultDiscriminator\", \"symbols\" : [ \"success\", \"failure\" ] }, \"doc\" : \"Contains the name of the field that has its value set.\" } ] }, \"default\" : { \"success\" : \"Union with aliases.\", \"fieldDiscriminator\" : \"success\" } } ] }"
            },
            {
                // Avro schema containing a record translated from an optional Pegasus union with a default value.
                // Translated union member property contains default value.
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"result\", \"type\" : [ { \"type\" : \"record\", \"name\" : \"fooResult\", \"fields\" : [ { \"name\" : \"success\", \"type\" : [ \"string\", \"null\" ], \"doc\" : \"Success message\", \"default\" : \"Union with aliases.\" }, { \"name\" : \"failure\", \"type\" : [ \"null\", \"string\" ], \"doc\" : \"Failure message\", \"default\" : null }, { \"name\" : \"fieldDiscriminator\", \"type\" : { \"type\" : \"enum\", \"name\" : \"fooResultDiscriminator\", \"symbols\" : [ \"success\", \"failure\" ] }, \"doc\" : \"Contains the name of the field that has its value set.\" } ] }, \"null\" ], \"default\" : { \"fieldDiscriminator\" : \"success\", \"success\" : \"Union with aliases.\", \"failure\" : null } } ] }",
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"result\", \"type\" : { \"type\" : \"record\", \"name\" : \"fooResult\", \"fields\" : [ { \"name\" : \"success\", \"type\" : \"string\", \"doc\" : \"Success message\", \"default\" : \"Union with aliases.\", \"optional\" : true }, { \"name\" : \"failure\", \"type\" : \"string\", \"doc\" : \"Failure message\", \"optional\" : true }, { \"name\" : \"fieldDiscriminator\", \"type\" : { \"type\" : \"enum\", \"name\" : \"fooResultDiscriminator\", \"symbols\" : [ \"success\", \"failure\" ] }, \"doc\" : \"Contains the name of the field that has its value set.\" } ] }, \"default\" : { \"success\" : \"Union with aliases.\", \"fieldDiscriminator\" : \"success\" }, \"optional\" : true } ] }"
            }
        };
  }

  @Test(dataProvider = "fromAvroSchemaData")
  public void testFromAvroSchema(String avroText, String schemaText) throws Exception
  {
    AvroToDataSchemaTranslationOptions options[] =
    {
      new AvroToDataSchemaTranslationOptions(AvroToDataSchemaTranslationMode.TRANSLATE),
      new AvroToDataSchemaTranslationOptions(AvroToDataSchemaTranslationMode.RETURN_EMBEDDED_SCHEMA),
      new AvroToDataSchemaTranslationOptions(AvroToDataSchemaTranslationMode.VERIFY_EMBEDDED_SCHEMA)
    };

    // test generating Pegasus schema from Avro schema
    for (AvroToDataSchemaTranslationOptions option : options)
    {
      DataSchema schema = SchemaTranslator.avroToDataSchema(avroText, option);
      String schemaTextFromAvro = SchemaToJsonEncoder.schemaToJson(schema, JsonBuilder.Pretty.SPACES);
      assertEquals(TestUtil.dataMapFromString(schemaTextFromAvro), TestUtil.dataMapFromString(schemaText));

      Schema avroSchema = Schema.parse(avroText);
      String preTranslateAvroSchema = avroSchema.toString();
      schema = SchemaTranslator.avroToDataSchema(avroSchema, option);
      schemaTextFromAvro = SchemaToJsonEncoder.schemaToJson(schema, JsonBuilder.Pretty.SPACES);
      assertEquals(TestUtil.dataMapFromString(schemaTextFromAvro), TestUtil.dataMapFromString(schemaText));
      String postTranslateAvroSchema = avroSchema.toString();
      assertEquals(preTranslateAvroSchema, postTranslateAvroSchema);
    }
  }

  @DataProvider
  public Object[][] avroToDataSchemaTranslationModeData()
  {
    return new Object[][]
        {
            {
                AvroToDataSchemaTranslationMode.TRANSLATE,
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ], \"com.linkedin.data\" : { } }",
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ], \"com.linkedin.data\" : { } }"
            },
            {
                AvroToDataSchemaTranslationMode.RETURN_EMBEDDED_SCHEMA,
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ ], \"com.linkedin.data\" : { \"schema\" : { \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ] }, \"optionalDefaultMode\" : \"TRANSLATE_DEFAULT\" } }",
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ] }"
            },
            {
                AvroToDataSchemaTranslationMode.VERIFY_EMBEDDED_SCHEMA,
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ], \"com.linkedin.data\" : { \"schema\" : { \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ] }, \"optionalDefaultMode\" : \"TRANSLATE_DEFAULT\" } }",
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ] }"
            },
            {
                // Convert an Avro schema containing a record field that was originally translated from a Pegasus union. The embedded
                // pegasus schema under "com.linkedin.data" property has this union field. Since TRANSLATE is used, the generated
                // pegasus schema will not contain this union but a translated record from the Avro record.
                AvroToDataSchemaTranslationMode.TRANSLATE,
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"result\", \"type\" : { \"type\" : \"record\", \"name\" : \"fooResult\", \"fields\" : [ { \"name\" : \"success\", \"type\" : [ \"null\", \"string\" ] }, { \"name\" : \"fieldDiscriminator\", \"type\" : { \"type\" : \"enum\", \"name\" : \"fooResultDiscriminator\", \"symbols\" : [ \"success\" ] }, \"doc\" : \"Contains the name of the field that has its value set.\" } ] } } ], \"com.linkedin.data\" : { \"schema\" : { \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"result\", \"type\" : [ { \"alias\" : \"success\", \"type\" : \"string\" } ] } ] }, \"optionalDefaultMode\" : \"TRANSLATE_DEFAULT\" } }",
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"result\", \"type\" : { \"type\" : \"record\", \"name\" : \"fooResult\", \"fields\" : [ { \"name\" : \"success\", \"type\" : \"string\", \"optional\" : true }, { \"name\" : \"fieldDiscriminator\", \"type\" : { \"type\" : \"enum\", \"name\" : \"fooResultDiscriminator\", \"symbols\" : [ \"success\" ] }, \"doc\" : \"Contains the name of the field that has its value set.\" } ] } } ], \"com.linkedin.data\" : { \"schema\" : { \"name\" : \"foo\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"result\", \"type\" : [ { \"alias\" : \"success\", \"type\" : \"string\" } ] } ] }, \"optionalDefaultMode\" : \"TRANSLATE_DEFAULT\" } }"
            },
            {
                // Convert an Avro schema containing a record field that was originally translated from a Pegasus Union. The embedded
                // pegasus schema under "com.linkedin.data" property has this union field. Since RETURN_EMBEDDED_SCHEMA is used, the
                // generated pegasus schema will be the embedded schema.
                AvroToDataSchemaTranslationMode.RETURN_EMBEDDED_SCHEMA,
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"result\", \"type\" : { \"type\" : \"record\", \"name\" : \"fooResult\", \"fields\" : [ { \"name\" : \"success\", \"type\" : [ \"null\", \"string\" ] }, { \"name\" : \"fieldDiscriminator\", \"type\" : { \"type\" : \"enum\", \"name\" : \"fooResultDiscriminator\", \"symbols\" : [ \"success\" ] }, \"doc\" : \"Contains the name of the field that has its value set.\" } ] } } ], \"com.linkedin.data\" : { \"schema\" : { \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"result\", \"type\" : [ { \"alias\" : \"success\", \"type\" : \"string\" } ] } ] }, \"optionalDefaultMode\" : \"TRANSLATE_DEFAULT\" } }",
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"result\", \"type\" : [ { \"type\" : \"string\", \"alias\" : \"success\" } ] } ] }"
            },
            {
                // Convert an Avro schema containing a record field that was originally translated from a Pegasus Union. The embedded
                // pegasus schema under "com.linkedin.data" property has this union field. Since VERIFY_EMBEDDED_SCHEMA is used, the
                // generated pegasus schema will be the embedded schema.
                AvroToDataSchemaTranslationMode.VERIFY_EMBEDDED_SCHEMA,
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"result\", \"type\" : { \"type\" : \"record\", \"name\" : \"fooResult\", \"fields\" : [ { \"name\" : \"success\", \"type\" : [ \"null\", \"string\" ], \"default\" : null }, { \"name\" : \"fieldDiscriminator\", \"type\" : { \"type\" : \"enum\", \"name\" : \"fooResultDiscriminator\", \"symbols\" : [ \"success\" ] }, \"doc\" : \"Contains the name of the field that has its value set.\" } ] } } ], \"com.linkedin.data\" : { \"schema\" : { \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"result\", \"type\" : [ { \"alias\" : \"success\", \"type\" : \"string\" } ] } ] }, \"optionalDefaultMode\" : \"TRANSLATE_DEFAULT\" } }",
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"result\", \"type\" : [ { \"type\" : \"string\", \"alias\" : \"success\" } ] } ] }"
            }
        };
  }

  @Test(dataProvider = "avroToDataSchemaTranslationModeData")
  public void testAvroToDataSchemaTranslationMode(AvroToDataSchemaTranslationMode translationMode, String avroSchemaText, String expected)
      throws IOException
  {
    AvroToDataSchemaTranslationOptions options = new AvroToDataSchemaTranslationOptions(translationMode);
    DataSchema translatedDataSchema = SchemaTranslator.avroToDataSchema(avroSchemaText, options);
    assertEquals(TestUtil.dataMapFromString(translatedDataSchema.toString()), TestUtil.dataMapFromString(expected));
  }

  @DataProvider
  public Object[][] avroToDataSchemaTranslationModeErrorData()
  {
    return new Object[][]
        {
            {
                AvroToDataSchemaTranslationMode.VERIFY_EMBEDDED_SCHEMA,
                "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ ], \"com.linkedin.data\" : { \"schema\" : { \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ] }, \"optionalDefaultMode\" : \"TRANSLATE_DEFAULT\" } }",
                IllegalArgumentException.class
            }
        };
  }

  @Test(dataProvider = "avroToDataSchemaTranslationModeErrorData")
  public void testAvroToDataSchemaTranslationModeError(AvroToDataSchemaTranslationMode translationMode, String avroSchemaText, Class<? extends Exception> expectedException)
  {
    AvroToDataSchemaTranslationOptions options = new AvroToDataSchemaTranslationOptions(translationMode);
    try
    {
      SchemaTranslator.avroToDataSchema(avroSchemaText, options);
      fail("Expect exception: " + expectedException);
    }
    catch (Exception e)
    {
      assertTrue(expectedException.isAssignableFrom(e.getClass()));
    }
  }

  @DataProvider
  public Object[][] unionDefaultValuesData()
  {
    return new Object[][] {
        {
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
        },
        {
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
        }
    };

  }

  @Test(dataProvider = "unionDefaultValuesData")
  public void testUnionDefaultValues(String readerSchemaText) throws IOException
  {
    final String emptySchemaText =
      "{ " +
      "  \"type\" : \"record\", " +
      "  \"name\" : \"foo\", " +
      "  \"fields\" : [] " +
      "}";

    final Schema emptySchema = Schema.parse(emptySchemaText);

    final String emptyRecord = "{}";

    final Schema readerSchema = Schema.parse(readerSchemaText);

    genericRecordFromString(emptyRecord, emptySchema, readerSchema);

    SchemaParser parser = new SchemaParser();
    parser.getValidationOptions().setAvroUnionMode(true);
    parser.parse(readerSchemaText);
    assertFalse(parser.hasError());
  }

  @Test
  public void testAvroUnionModeChaining() throws IOException
  {
    String expectedSchema = "{ " +
        "  \"type\" : \"record\", " +
        "  \"name\" : \"A\", " +
        "  \"namespace\" : \"com.linkedin.pegasus.test\", " +
        "  \"fields\" : [ " +
        "    { " +
        "      \"name\" : \"someBorC\", " +
        "      \"type\" : [ " +
        "        { " +
        "          \"type\" : \"record\", " +
        "          \"name\" : \"B\", " +
        "          \"fields\" : [ " +
        "            { " +
        "              \"name\" : \"someAorC\", " +
        "              \"type\" : [ " +
        "                \"A\", " +
        "                { " +
        "                  \"type\" : \"record\", " +
        "                  \"name\" : \"C\", " +
        "                  \"fields\" : [ " +
        "                    { " +
        "                      \"name\" : \"something\", " +
        "                      \"type\" : \"int\", " +
        "                      \"optional\" : true, " +
        "                      \"default\" : 42" +
        "                    } " +
        "                  ] " +
        "                } " +
        "              ] " +
        "            } " +
        "          ] " +
        "        }, " +
        "        \"C\" " +
        "      ] " +
        "    } " +
        "  ]" +
        "}";

    String avroRootUrl = getClass().getClassLoader().getResource("avro").getFile();
    String avroRootDir = new File(avroRootUrl).getAbsolutePath();
    String avroFilePath =  avroRootDir + FS + "com" + FS + "linkedin" + FS + "pegasus" + FS + "test" + FS + "A.avsc";
    File avroFile = new File(avroFilePath);

    String schema = readFile(avroFile);
    AvroToDataSchemaTranslationOptions options =
        new AvroToDataSchemaTranslationOptions(AvroToDataSchemaTranslationMode.TRANSLATE).setFileResolutionPaths(avroRootDir);
    DataSchema pdscSchema = SchemaTranslator.avroToDataSchema(schema, options);
    DataMap actual = TestUtil.dataMapFromString(pdscSchema.toString());
    DataMap expected = TestUtil.dataMapFromString(expectedSchema);
    assertEquals(actual, expected);
  }

  private static String readFile(File file) throws IOException
  {
    BufferedReader br = new BufferedReader(new FileReader(file));
    StringBuilder sb = new StringBuilder();
    String line;
    while((line = br.readLine()) != null)
    {
      sb.append(line + "\n");
    }
    return sb.toString();
  }
}
