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


import com.linkedin.data.DataMap;
import com.linkedin.data.TestUtil;
import com.linkedin.data.message.Message;
import com.linkedin.data.message.MessageList;
import com.linkedin.data.schema.DataSchema;
import java.io.IOException;
import org.apache.avro.Schema;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;


public class TestAvroOverrideFactory
{
  public static class MyCustomDataTranslator implements CustomDataTranslator
  {
    @Override
    public Object avroGenericToData(DataTranslatorContext context, Object avroData, Schema avroSchema, DataSchema schema)
    {
      return null;
    }

    @Override
    public Object dataToAvroGeneric(DataTranslatorContext context, Object data, DataSchema schema, Schema avroSchema)
    {
      return null;
    }
  }

  public static class PrivateConstructorMyCustomDataTranslator extends MyCustomDataTranslator
  {
    private PrivateConstructorMyCustomDataTranslator()
    {

    }
  }

  private static class MyAvroOverrideFactory extends AvroOverrideFactory
  {
    private MessageList<Message> _messageList = new MessageList<>();
    private static final Object[] _path = new Object[0];

    MyAvroOverrideFactory()
    {
      setInstantiateCustomDataTranslator(true);
    }

    public void setInstantiateCustomDataTranslator(boolean value)
    {
      super.setInstantiateCustomDataTranslator(value);
    }

    @Override
    public void emitMessage(String format, Object... args)
    {
      _messageList.add(new Message(_path, format, args));
    }

    public void reset()
    {
      _messageList.clear();
    }
  };

  @Test
  public void testGood() throws IOException
  {
    String schemaText =
      "{\n" +
      "  \"type\" : \"record\",\n" +
      "  \"name\" : \"Foo\",\n" +
      "  \"fields\" : [],\n" +
      "  \"avro\" : {\n" +
      "    \"schema\" : {\n" +
      "      \"name\" : \"CustomFoo\",\n" +
      "      \"namespace\" : \"org.foobar\"\n" +
      "    },\n" +
      "    \"translator\" : {\n" +
      "      \"class\" : \"" + MyCustomDataTranslator.class.getName() + "\"\n" +
      "    }\n" +
      "  }\n" +
      "}";

    final boolean debug = false;

    MyAvroOverrideFactory avroOverrideFactory = new MyAvroOverrideFactory();

    DataSchema schema = TestUtil.dataSchemaFromString(schemaText);
    AvroOverride avroOverride = avroOverrideFactory.createFromDataSchema(schema);
    String message = avroOverrideFactory._messageList.toString();

    assertTrue(message.isEmpty());
    assertSame(avroOverride.getAvroSchemaDataMap(), ((DataMap) schema.getProperties().get("avro")).get("schema"));
    assertEquals(avroOverride.getAvroSchemaFullName(), "org.foobar.CustomFoo");

    assertSame(avroOverride.getCustomDataTranslatorClassName(), ((DataMap) ((DataMap) schema.getProperties().get("avro")).get("translator")).get("class"));
    assertSame(avroOverride.getCustomDataTranslator().getClass(), MyCustomDataTranslator.class);
  }

  @Test
  public void testSchemaNames() throws IOException
  {
    String schemaTemplate =
      "{\n" +
      "  \"type\" : \"record\",\n" +
      "  \"name\" : \"Foo\",\n" +
      "  \"fields\" : [],\n" +
      "  \"avro\" : {\n" +
      "    \"schema\" : {\n" +
      "      NAME\n" +
      "    },\n" +
      "    \"translator\" : {\n" +
      "      \"class\" : \"" + MyCustomDataTranslator.class.getName() + "\"\n" +
      "    }\n" +
      "  }\n" +
      "}";

    String inputs[][] =
      {
        {
          "\"name\" : \"Foo\"",
          "Foo"
        },
        {
          "\"name\" : \"Foo\", \"namespace\" : \"org.foo\"",
          "org.foo.Foo"
        },
        {
          "\"name\" : \"org.foo.Foo\"",
          "org.foo.Foo"
        }
      };

    MyAvroOverrideFactory avroOverrideFactory = new MyAvroOverrideFactory();

    for (String[] row : inputs)
    {
      String name = row[0];
      String fullName = row[1];

      String schemaText = schemaTemplate.replaceAll("NAME", name);
      DataSchema schema = TestUtil.dataSchemaFromString(schemaText);

      AvroOverride avroOverride = avroOverrideFactory.createFromDataSchema(schema);
      String message = avroOverrideFactory._messageList.toString();
      assertTrue(message.isEmpty());

      assertEquals(avroOverride.getAvroSchemaFullName(), fullName);

      avroOverrideFactory.reset();
    }
  }

  @Test
  public void testBad() throws IOException
  {
    final String[][] inputs =
      {
        {
          "{\n" +
          "  \"type\" : \"record\",\n" +
          "  \"name\" : \"Foo\",\n" +
          "  \"fields\" : [],\n" +
          "  \"avro\" : 0\n" +
          "}",
          "has \"avro\" property whose value is not a JSON object"
        },
        {
          "{\n" +
          "  \"type\" : \"record\",\n" +
          "  \"name\" : \"Foo\",\n" +
          "  \"fields\" : [],\n" +
          "  \"avro\" : {\n" +
          "    \"schema\" : 0\n" +
          "  }\n" +
          "}",
          "\"schema\" property is not a JSON object, value is 0"
        },
        {
          "{\n" +
          "  \"type\" : \"record\",\n" +
          "  \"name\" : \"Foo\",\n" +
          "  \"fields\" : [],\n" +
          "  \"avro\" : {\n" +
          "    \"schema\" : {\n" +
          "    }\n" +
          "  }\n" +
          "}",
          "\"name\" property of \"schema\" property is required"
        },
        {
          "{\n" +
          "  \"type\" : \"record\",\n" +
          "  \"name\" : \"Foo\",\n" +
          "  \"fields\" : [],\n" +
          "  \"avro\" : {\n" +
          "    \"schema\" : {\n" +
          "      \"name\" : 0\n" +
          "    }\n" +
          "  }\n" +
          "}",
          "\"name\" property of \"schema\" property is not a string, value is 0"
        },
        {
          "{\n" +
          "  \"type\" : \"record\",\n" +
          "  \"name\" : \"Foo\",\n" +
          "  \"fields\" : [],\n" +
          "  \"avro\" : {\n" +
          "    \"schema\" : {\n" +
          "      \"name\" : \"CustomFoo\",\n" +
          "      \"namespace\" : 0\n" +
          "    }\n" +
          "  }\n" +
          "}",
          "\"namespace\" property of \"schema\" property is not a string, value is 0"
        },
        {
          "{\n" +
          "  \"type\" : \"record\",\n" +
          "  \"name\" : \"Foo\",\n" +
          "  \"fields\" : [],\n" +
          "  \"avro\" : {\n" +
          "    \"translator\" : 0\n" +
          "  }\n" +
          "}",
          "\"translator\" property is not a JSON object, value is 0"
        },
        {
          "{\n" +
          "  \"type\" : \"record\",\n" +
          "  \"name\" : \"Foo\",\n" +
          "  \"fields\" : [],\n" +
          "  \"avro\" : {\n" +
          "    \"translator\" : {\n" +
          "    }\n" +
          "  }\n" +
          "}",
          "\"class\" property of \"translator\" property is required"
        },
        {
          "{\n" +
          "  \"type\" : \"record\",\n" +
          "  \"name\" : \"Foo\",\n" +
          "  \"fields\" : [],\n" +
          "  \"avro\" : {\n" +
          "    \"translator\" : {\n" +
          "      \"class\" : 0\n" +
          "    }\n" +
          "  }\n" +
          "}",
          "\"class\" property of \"translator\" property is not a string, value is 0"
        },
        {
          "{\n" +
          "  \"type\" : \"record\",\n" +
          "  \"name\" : \"Foo\",\n" +
          "  \"fields\" : [],\n" +
          "  \"avro\" : {\n" +
          "    \"schema\" : {\n" +
          "      \"name\" : \"CustomFoo\",\n" +
          "      \"namespace\" : \"org.foobar\"\n" +
          "    }\n" +
          "  }\n" +
          "}",
          "both \"translator\" and \"schema\" properties of \"avro\" are required if either is present"
        },
        {
          "{\n" +
          "  \"type\" : \"record\",\n" +
          "  \"name\" : \"Foo\",\n" +
          "  \"fields\" : [],\n" +
          "  \"avro\" : {\n" +
          "    \"translator\" : {\n" +
          "      \"class\" : \"" + MyCustomDataTranslator.class.getName() + "\"\n" +
          "    }\n" +
          "  }\n" +
          "}",
          "both \"translator\" and \"schema\" properties of \"avro\" are required if either is present"
        },
        {
          "{\n" +
          "  \"type\" : \"record\",\n" +
          "  \"name\" : \"Foo\",\n" +
          "  \"fields\" : [],\n" +
          "  \"avro\" : {\n" +
          "    \"schema\" : {\n" +
          "      \"name\" : \"CustomFoo\",\n" +
          "      \"namespace\" : \"org.foobar\"\n" +
          "    },\n" +
          "    \"translator\" : {\n" +
          "      \"class\" : \"java.util.Date\"\n" +
          "    }\n" +
          "  }\n" +
          "}",
          "java.util.Date is not a com.linkedin.data.avro.CustomDataTranslator"
        },
        {
          "{\n" +
          "  \"type\" : \"record\",\n" +
          "  \"name\" : \"Foo\",\n" +
          "  \"fields\" : [],\n" +
          "  \"avro\" : {\n" +
          "    \"schema\" : {\n" +
          "      \"name\" : \"CustomFoo\",\n" +
          "      \"namespace\" : \"org.foobar\"\n" +
          "    },\n" +
          "    \"translator\" : {\n" +
          "      \"class\" : \"" + PrivateConstructorMyCustomDataTranslator.class.getName() + "\"\n" +
          "    }\n" +
          "  }\n" +
          "}",
          "com.linkedin.data.avro.TestAvroOverrideFactory$PrivateConstructorMyCustomDataTranslator cannot be instantiated due to java.lang.IllegalAccessException"
        },
      };

    final boolean debug = false;

    MyAvroOverrideFactory avroOverrideFactory = new MyAvroOverrideFactory();


    for (String[] row : inputs)
    {
      String schemaText = row[0];

      DataSchema schema = TestUtil.dataSchemaFromString(schemaText);
      AvroOverride avroOverride = avroOverrideFactory.createFromDataSchema(schema);

      String message = avroOverrideFactory._messageList.toString();
      if (debug) TestUtil.out.println(message);

      for (int i = 1; i < row.length; i++)
      {
        assertTrue(message.contains(row[i]), message + " contains " + row[i]);
      }
      avroOverrideFactory.reset();
    }
  }
}
