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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
  private static final int MAX_ALLOWED_SCHEMA_RECURSION = 5;

  public static class DataGenerationOptions implements Cloneable
  {
    @Override
    public DataGenerationOptions clone()
    {
      try
      {
        return (DataGenerationOptions) super.clone();
      }
      catch (CloneNotSupportedException e)
      {
        throw new IllegalStateException(e);
      }
    }

    /**
     * Get a constrained instance of this {@link DataGenerationOptions} to break out of the recursive nested
     * data generation for valid schemas.
     *
     * @return a cloned {@link DataGenerationOptions} instance with required field only and no more array element
     */
    public DataGenerationOptions getConstrained()
    {
      final DataGenerationOptions constrained = clone();

      constrained._requiredFieldsOnly = true;
      constrained._arraySize = 0;

      return constrained;
    }

    /**
     * @return whether only the required field should be generated for the record
     */
    public boolean isRequiredFieldsOnly()
    {
      return _requiredFieldsOnly;
    }

    public void setRequiredFieldsOnly(boolean requiredFieldsOnly)
    {
      _requiredFieldsOnly = requiredFieldsOnly;
    }

    /**
     * @return whether to use the default field value in the generated record
     */
    public boolean isUseDefaults()
    {
      return _useDefaults;
    }

    public void setUseDefaults(boolean useDefaults)
    {
      _useDefaults = useDefaults;
    }

    /**
     * @return size of the arrays in the generated record
     */
    public int getArraySize()
    {
      return _arraySize;
    }

    public void setArraySize(int arraySize)
    {
      _arraySize = arraySize;
    }

    private boolean _requiredFieldsOnly = false;
    private boolean _useDefaults = false;
    private int _arraySize = (int)(Math.random() * 3) + 1;
  }

  public static class ParentSchemas
  {
    private final Map<DataSchema, Integer> counts = new HashMap<DataSchema, Integer>();
    public void incrementReferences(DataSchema schema)
    {
      Integer count = counts.get(schema);
      if(count == null)
      {
        count = 0;
      }
      count++;
      counts.put(schema, count);
    }

    public void decrementReferences(DataSchema schema)
    {
      Integer count = counts.get(schema);
      if(count == null || count == 0)
      {
        throw new IllegalArgumentException("No references to remove for given schema");
      }
      count--;
      if(count == 0) counts.remove(schema);
      else counts.put(schema, count);
    }

    public int count(DataSchema schema)
    {
      Integer count = counts.get(schema);
      if(count == null)
      {
        count = 0;
      }
      return count;
    }

    public boolean contains(DataSchema schema)
    {
      return count(schema) > 0;
    }

    public Set<DataSchema> getAllReferenced()
    {
      return counts.keySet();
    }
  }

  public static DataMap buildRecordData(NamedDataSchema schema, DataGenerationOptions spec)
  {
    return buildRecordData(new ParentSchemas(), schema, spec);
  }

  public static Object buildDataMappable(DataSchema schema, DataGenerationOptions spec)
  {
    return buildDataMappable(new ParentSchemas(), schema, null, spec);
  }

  public static Object buildDataMappable(DataSchema schema, String fieldName, DataGenerationOptions spec)
  {
    return buildDataMappable(new ParentSchemas(), schema, fieldName, spec);
  }

  private static DataMap buildRecordData(ParentSchemas parentSchemas, NamedDataSchema schema, DataGenerationOptions spec)
  {
    spec = preventRecursionIntoAlreadyTraversedSchemas(parentSchemas, spec, schema);
    parentSchemas.incrementReferences(schema);
    final DataMap data = new DataMap();
    if (schema instanceof RecordDataSchema)
    {
      for (RecordDataSchema.Field field: ((RecordDataSchema) schema).getFields())
      {
        if (!(spec.isRequiredFieldsOnly() && field.getOptional()))
        {
          final Object value;
          if (spec.isUseDefaults() && field.getDefault() != null)
          {
            value = field.getDefault();
          }
          else
          {
            value = buildDataMappable(parentSchemas, field.getType(), field.getName(), spec);
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
      data.put("ref", buildDataMappable(parentSchemas, schema.getDereferencedDataSchema(), spec));
    }
    else
    {
      data.put("value", buildDataMappable(parentSchemas, schema, spec));
    }
    parentSchemas.decrementReferences(schema);
    return data;
  }

  private static Object buildDataMappable(ParentSchemas parentSchemas, DataSchema schema, DataGenerationOptions spec)
  {
    return buildDataMappable(parentSchemas, schema, null, spec);
  }

  private static Object buildDataMappable(ParentSchemas parentSchemas, DataSchema schema, String fieldName, DataGenerationOptions spec)
  {
    spec = preventRecursionIntoAlreadyTraversedSchemas(parentSchemas, spec, schema);
    parentSchemas.incrementReferences(schema);
    Object data = null;
    final DataSchema derefSchema = schema.getDereferencedDataSchema();
    switch (derefSchema.getType())
    {
      case ARRAY:
        final DataList dataList = new DataList(spec.getArraySize());
        for (int i = 0; i < spec.getArraySize(); i++)
        {
          final Object item = buildDataMappable(parentSchemas, ((ArrayDataSchema) derefSchema).getItems(), fieldName, spec);
          dataList.add(item);
        }
        data = dataList;
        break;
      case BOOLEAN:
        data = Math.random() > .5 ? true : false;
        break;
      case BYTES:
        data = ByteString.copy("some bytes".getBytes(Data.UTF_8_CHARSET));
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
        for (int i = 0; i < spec.getArraySize(); i++)
        {
          final Object item = buildDataMappable(parentSchemas, ((MapDataSchema) derefSchema).getValues(), fieldName, spec);
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
        data = buildRecordData(parentSchemas, (RecordDataSchema) derefSchema, spec);
        break;
      case STRING:
        data = buildStringData(parentSchemas, fieldName, spec);
        break;
      case TYPEREF:
        data = buildDataMappable(parentSchemas, derefSchema, fieldName, spec);
        break;
      case UNION:
        final UnionDataSchema unionSchema = (UnionDataSchema) derefSchema;
        final List<DataSchema> types = removeAlreadyTraversedSchemasFromUnionMemberList(parentSchemas, unionSchema.getTypes());
        final int unionIdx = (int)(Math.random() * types.size());
        final DataSchema unionItemSchema = types.get(unionIdx);
        data = buildDataMappable(parentSchemas, unionItemSchema, fieldName, spec);

        if (data != null)
        {
          final DataMap unionMap = new DataMap();
          unionMap.put(unionItemSchema.getUnionMemberKey(), data);
          data = unionMap;
        }
        break;
    }

    parentSchemas.decrementReferences(schema);
    return data;
  }

  // TODO Consider validation rules here as well, such as length (easy), regex (really hard), etc
  private static Object buildStringData(ParentSchemas parentSchemas, String fieldName, DataGenerationOptions spec)
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

  public SchemaSampleDataGenerator(DataSchemaResolver resolver)
  {
    _schemaParser = new SchemaParser(resolver);
  }

  private DataMap buildDataMap(ParentSchemas parentSchemas, String pegasusDataSchemaName, DataGenerationOptions spec)
  {
    final DataSchema schema = _schemaParser.lookupName(pegasusDataSchemaName);
    spec = preventRecursionIntoAlreadyTraversedSchemas(parentSchemas, spec, schema);
    parentSchemas.incrementReferences(schema);
    if (schema == null)
    {
      throw new IllegalArgumentException(String.format("Could not find pegasus data schema '%s'", pegasusDataSchemaName));
    }

    assert(schema instanceof RecordDataSchema);
    final DataMap data = buildRecordData(parentSchemas, (RecordDataSchema) schema, spec);
    parentSchemas.decrementReferences(schema);
    return data;
  }

  private static DataGenerationOptions preventRecursionIntoAlreadyTraversedSchemas(ParentSchemas parentSchemas, DataGenerationOptions spec, final DataSchema schema) {
    if(parentSchemas.count(schema) > MAX_ALLOWED_SCHEMA_RECURSION)
    {
      throw new IllegalArgumentException("Could not generate data for recursively referenced schemas. Recursive referenced schemas must be optional or in a list, map or union with valid alternatives.");
    }
    if(parentSchemas.contains(schema)) // if there is a recursively referenced schema
    {
      spec = spec.getConstrained();
    }
    return spec;
  }

  private static List<DataSchema> removeAlreadyTraversedSchemasFromUnionMemberList(ParentSchemas parentSchemas, List<DataSchema> unionMembers)
  {
    final ArrayList<DataSchema> copy = new ArrayList<DataSchema>(unionMembers);
    copy.removeAll(parentSchemas.getAllReferenced());
    if(copy.isEmpty()) return unionMembers;  // eek, cannot safely filter out already traversed schemas, this code path will likely result in IllegalArgumentException being thrown from preventRecursionIntoAlreadyTraversedSchemas (which is the correct way to handle this).
    else return copy;
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
