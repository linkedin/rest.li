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

import com.linkedin.data.ByteString;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.EnumDataSchema;
import com.linkedin.data.schema.FixedDataSchema;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.SchemaParser;
import com.linkedin.data.schema.TyperefDataSchema;
import com.linkedin.data.schema.UnionDataSchema;

/**
 * Generates example data given a Pegasus schema. The schema can be provided in a
 * number of ways: a RecordTemplate Class, a RecordDataSchema or a directory containing
 * .pdsc files.
 *
 * @author dellamag, Keren Jin
 */
public class SchemaSampleDataGenerator
{
  public static class DataGenerationSpec
  {
    public boolean requiredFieldsOnly = false;
    public boolean useDefaults = false;
    public boolean realisticData = true;
    public int arraySize = (int)(Math.random() * 3) + 1;
  }

  public static DataMap buildRecordData(NamedDataSchema schema,
                                        DataGenerationSpec spec)
  {
    final DataMap data = new DataMap();
    if (schema instanceof RecordDataSchema)
    {
      for (RecordDataSchema.Field field: ((RecordDataSchema) schema).getFields())
      {
        if (! (spec.requiredFieldsOnly && field.getOptional()))
        {
          final Object value;
          if (spec.useDefaults && field.getDefault() != null)
          {
            value = field.getDefault();
          }
          else
          {
            value = buildDataMappable(field.getType(), field.getName(), spec);
          }

          // null is returned for NULL Pegasus type (used in unions, primarily)
          if (value == null)
          {
            data.remove(field.getName());
          }
          else
          {
            data.put(field.getName(), value);
          }
        }
      }
    }
    else if (schema instanceof TyperefDataSchema)
    {
      data.put("ref", buildDataMappable(schema.getDereferencedDataSchema(), spec));
    }
    else
    {
      data.put("value", buildDataMappable(schema, spec));
    }

    return data;
  }

  public static Object buildDataMappable(DataSchema schema, DataGenerationSpec spec)
  {
    return buildDataMappable(schema, null, spec);
  }

  public static Object buildDataMappable(DataSchema schema, String fieldName, DataGenerationSpec spec)
  {
    Object data = null;
    final DataSchema derefSchema = schema.getDereferencedDataSchema();
    switch (derefSchema.getType())
    {
      case ARRAY:
        final DataList dataList = new DataList(spec.arraySize);
        for (int i = 0; i < spec.arraySize; i++)
        {
          final Object item = buildDataMappable(((ArrayDataSchema) derefSchema).getItems(), fieldName, spec);
          dataList.add(item);
        }
        data = dataList;
        break;
      case BOOLEAN:
        data = Math.random() > .5 ? true : false;
        break;
      case BYTES:
        data = ByteString.copy("some bytes".getBytes());
        break;
      case DOUBLE:
        data = Math.random() * 100;
        break;
      case FLOAT:
        data = (float)(Math.random() * 100);
        break;
      case ENUM:
        final EnumDataSchema enumSchema = (EnumDataSchema) derefSchema;
        final int idx = (int)(Math.random() * enumSchema.getSymbols().size());
        data = enumSchema.getSymbols().get(idx);
        break;
      case FIXED:
        final FixedDataSchema fixedSchema = (FixedDataSchema) derefSchema;
        final byte[] bytes = new byte[fixedSchema.getSize()];
        for (int i = 0; i < fixedSchema.getSize(); i++)
        {
          bytes[i] = '1';
        }
        data = ByteString.copy(bytes);
        break;
      case INT:
        data = (int)(Math.random() * 100);
        break;
      case LONG:
        data = (long)(Math.random() * 100);
        break;
      case MAP:
        final DataMap dataMap = new DataMap();
        for (int i = 0; i < spec.arraySize; i++)
        {
          final Object item = buildDataMappable(((MapDataSchema) derefSchema).getValues(), fieldName, spec);
          final int key = (int)(Math.random() * 1000) + 1000;
          dataMap.put(String.valueOf(key), item);
        }
        data = dataMap;
        break;
      case NULL:
        // ???
        data = null;
        break;
      case RECORD:
        data = buildRecordData((RecordDataSchema) derefSchema, spec);
        break;
      case STRING:
        data = buildStringData(fieldName, spec);
        break;
      case TYPEREF:
        data = buildDataMappable(derefSchema, fieldName, spec);
        break;
      case UNION:
        final UnionDataSchema unionSchema = (UnionDataSchema) derefSchema;
        final int unionIdx = (int)(Math.random() * unionSchema.getTypes().size());
        final DataSchema unionItemSchema = unionSchema.getTypes().get(unionIdx);
        data = buildDataMappable(unionItemSchema, fieldName, spec);

        if (data != null)
        {
          final DataMap unionMap = new DataMap();
          unionMap.put(unionItemSchema.getUnionMemberKey(), data);
          data = unionMap;
        }
        break;
    }

    return data;
  }

  public SchemaSampleDataGenerator(DataSchemaResolver resolver)
  {
    _schemaParser = new SchemaParser(resolver);
  }

  // TODO Consider validation rules here as well, such as length (easy), regex (really hard), etc
  private static Object buildStringData(String fieldName, DataGenerationSpec spec)
  {
    String[] EXAMPLE_STRINGS = STRINGS;

    if (fieldName != null)
    {
      // hacky guess as to the type of string we should generate (based on field name)
      if (matchFieldName(fieldName, new String[] {"Url", "Link"}))
      {
        EXAMPLE_STRINGS = URL_STRINGS;
      }
      else if (matchFieldName(fieldName, new String[] {"Name"}))
      {
        EXAMPLE_STRINGS = NAME_STRINGS;
      }
      else if (matchFieldName(fieldName, new String[] {"Email", "emailAddress", "email_address"}))
      {
        EXAMPLE_STRINGS = EMAIL_STRINGS;
      }
      else if (matchFieldName(fieldName, new String[] {"Description", "Summary"}))
      {
        EXAMPLE_STRINGS = DESCRIPTION_STRINGS;
      }
    }

    final int stringIdx = (int)(Math.random() * EXAMPLE_STRINGS.length);
    return EXAMPLE_STRINGS[stringIdx];
  }

  private static boolean matchFieldName(String fieldName, String[] parts)
  {
    boolean matches = false;
    for (String part : parts)
    {
      if (fieldName.equals(part) || fieldName.equals(part.toLowerCase()) ||
          fieldName.endsWith(part) || fieldName.endsWith(part.toLowerCase()))
      {
        matches = true;
        break;
      }
    }

    return matches;
  }

  private DataMap buildDataMap(String pegasusDataSchemaName, DataGenerationSpec spec)
  {
    final DataSchema schema = _schemaParser.lookupName(pegasusDataSchemaName);
    if (schema == null)
    {
      throw new IllegalArgumentException(String.format("Could not find pegasus data schema '%s'", pegasusDataSchemaName));
    }

    assert(schema instanceof RecordDataSchema);
    return buildRecordData((RecordDataSchema) schema, spec);
  }

  private static final String[] STRINGS = new String[] {
    "foo"
  };

  private static final String[] URL_STRINGS = new String[] {
    "http://www.example.com"
  };

  private static final String[] NAME_STRINGS = new String[] {
    "Loreum Ipsum"
  };

  private static final String[] EMAIL_STRINGS = new String[] {
    "foo@example.com"
  };

  private static final String[] DESCRIPTION_STRINGS = new String[] {
    "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
    "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.",
    // http://hipsteripsum.me/
    "Aesthetic sustainable raw denim messenger bag narwhal 8-bit, ethnic vegan craft beer quinoa selvage authentic dolor.",
    "Vegan commodo kogi twee, consectetur single-origin coffee readymade swag.",
    "Organic american apparel eiusmod, high life craft beer mollit polaroid lo-fi sed culpa.",
    "Lo-fi vinyl 3 wolf moon hoodie PBR eiusmod farm-to-table next level, est aliqua sriracha pour-over raw denim"
  };

  private final SchemaParser _schemaParser;

  // TODO this Main function will be used in offline documentation generation, which is not ready yet
  /*
  public static void main(String[] args)
  {
    final List<String> paths = new ArrayList<String>();
    paths.add("pegasus/restli-examples-api/src/main/pegasus");

    final DataSchemaResolver schemaResolver = new FileDataSchemaResolver(SchemaParserFactory.instance(), paths);
    final SchemaSampleDataGenerator gen = new SchemaSampleDataGenerator(schemaResolver);

    final DataMap dataMap = gen.buildDataMap("com.linkedin.restli.examples.groups.api.GroupMembership",
                                             new DataGenerationSpec());
  }
  */
}
