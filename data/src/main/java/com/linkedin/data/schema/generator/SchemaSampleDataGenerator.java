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
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.EnumDataSchema;
import com.linkedin.data.schema.FixedDataSchema;
import com.linkedin.data.schema.SchemaParser;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.PegasusSchemaParser;
import com.linkedin.data.schema.TyperefDataSchema;
import com.linkedin.data.schema.UnionDataSchema;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Generates example data given a Pegasus schema. The schema can be provided in a
 * number of ways: a RecordTemplate Class, a RecordDataSchema or a directory containing
 * .pdsc files.
 *
 * @author dellamag, Keren Jin
 */
public class SchemaSampleDataGenerator implements DataGenerator
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

    /**
     * @return {@link SampleDataCallback} used for generating sample data,
     *         which by default is an instance of {@link DefaultSampleDataCallback}
     */
    public SampleDataCallback getCallback()
    {
      return _callback;
    }

    public void setCallback(SampleDataCallback pool)
    {
      _callback = pool;
    }

    private boolean _requiredFieldsOnly = false;
    private boolean _useDefaults = false;
    private int _arraySize = (int)(Math.random() * 3) + 1;
    private SampleDataCallback _callback = DefaultSampleDataCallback.INSTANCE;
  }

  private static class ParentSchemas
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

  /**
   * Generate a {@link DataMap} of the specified schema and filled with random values.
   *
   * @param schema schema of the result
   * @param spec options of how to generate the result
   * @return generated DataMap
   */
  public static DataMap buildRecordData(NamedDataSchema schema, DataGenerationOptions spec)
  {
    return buildRecordData(new ParentSchemas(), schema, spec);
  }

  /**
   * Generate a data object of the specified schema and filled with random value.
   *
   * @param schema schema of the result
   * @param spec options of how to generate the result
   * @return generated data object
   */
  public static Object buildData(DataSchema schema, DataGenerationOptions spec)
  {
    return buildData(new ParentSchemas(), schema, null, spec);
  }

  /**
   * Generate a data object of the specified schema and filled with random value.
   *
   * @param schema schema of the result
   * @param fieldName field name associated to the data object, which is used to specialize the random value
   *                  use null if not sure
   * @param spec options of how to generate the result
   * @return generated data object
   */
  public static Object buildData(DataSchema schema, String fieldName, DataGenerationOptions spec)
  {
    return buildData(new ParentSchemas(), schema, fieldName, spec);
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
            value = buildData(parentSchemas, field.getType(), field.getName(), spec);
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
      data.put("ref", buildData(parentSchemas, schema.getDereferencedDataSchema(), spec));
    }
    else
    {
      data.put("value", buildData(parentSchemas, schema, spec));
    }
    parentSchemas.decrementReferences(schema);
    return data;
  }

  private static Object buildData(ParentSchemas parentSchemas, DataSchema schema, DataGenerationOptions spec)
  {
    return buildData(parentSchemas, schema, null, spec);
  }

  private static Object buildData(ParentSchemas parentSchemas,
                                  DataSchema schema,
                                  String fieldName,
                                  DataGenerationOptions spec)
  {
    spec = preventRecursionIntoAlreadyTraversedSchemas(parentSchemas, spec, schema);
    parentSchemas.incrementReferences(schema);
    final DataSchema derefSchema = schema.getDereferencedDataSchema();
    final SampleDataCallback callback = spec.getCallback();
    Object data = null;
    switch (derefSchema.getType())
    {
      case BOOLEAN:
        data = callback.getBoolean(fieldName);
        break;
      case INT:
        data = callback.getInteger(fieldName);
        break;
      case LONG:
        data = callback.getLong(fieldName);
        break;
      case FLOAT:
        data = callback.getFloat(fieldName);
        break;
      case DOUBLE:
        data = callback.getDouble(fieldName);
        break;
      case BYTES:
        data = callback.getBytes(fieldName);
        break;
      case STRING:
        data = callback.getString(fieldName);
        break;
      case NULL:
        data = Data.NULL;
        break;
      case FIXED:
        data = callback.getFixed(fieldName, (FixedDataSchema) derefSchema);
        break;
      case ENUM:
        data = callback.getEnum(fieldName, (EnumDataSchema) derefSchema);
        break;
      case ARRAY:
        final DataList dataList = new DataList(spec.getArraySize());
        for (int i = 0; i < spec.getArraySize(); i++)
        {
          final Object item = buildData(parentSchemas, ((ArrayDataSchema) derefSchema).getItems(), fieldName, spec);
          dataList.add(item);
        }
        data = dataList;
        break;
      case RECORD:
        data = buildRecordData(parentSchemas, (RecordDataSchema) derefSchema, spec);
        break;
      case MAP:
        final DataMap dataMap = new DataMap();
        for (int i = 0; i < spec.getArraySize(); i++)
        {
          final Object item = buildData(parentSchemas, ((MapDataSchema) derefSchema).getValues(), fieldName, spec);
          dataMap.put("mapField_" + _random.nextInt(), item);
        }
        data = dataMap;
        break;
      case UNION:
        final UnionDataSchema unionSchema = (UnionDataSchema) derefSchema;
        final List<UnionDataSchema.Member> members = removeAlreadyTraversedSchemasFromUnionMemberList(parentSchemas, unionSchema.getMembers());
        final int unionIndex = _random.nextInt(members.size());
        final UnionDataSchema.Member unionMember = members.get(unionIndex);
        data = buildData(parentSchemas, unionMember.getType(), fieldName, spec);

        if (data != null)
        {
          final DataMap unionMap = new DataMap();
          unionMap.put(unionMember.getUnionMemberKey(), data);
          data = unionMap;
        }
        break;
      case TYPEREF:
        data = buildData(parentSchemas, derefSchema, fieldName, spec);
        break;
    }

    parentSchemas.decrementReferences(schema);
    return data;
  }

  public SchemaSampleDataGenerator(DataSchemaResolver resolver, DataGenerationOptions spec)
  {
    _schemaParser = new SchemaParser(resolver);
    _spec = spec;
  }

  @Override
  public Object buildData(String fieldName, DataSchema dataSchema)
  {
    return SchemaSampleDataGenerator.buildData(dataSchema, fieldName, _spec);
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

  private static List<UnionDataSchema.Member> removeAlreadyTraversedSchemasFromUnionMemberList(ParentSchemas parentSchemas, List<UnionDataSchema.Member> unionMembers)
  {
    final List<UnionDataSchema.Member> copy = unionMembers.stream().filter(member -> !parentSchemas.contains(member.getType())).collect(Collectors.toList());
    if(copy.isEmpty()) return unionMembers;  // eek, cannot safely filter out already traversed schemas, this code path will likely result in IllegalArgumentException being thrown from preventRecursionIntoAlreadyTraversedSchemas (which is the correct way to handle this).
    else return copy;
  }

  private final DataGenerationOptions _spec;
  private final PegasusSchemaParser _schemaParser;

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

  private static final Random _random = new Random();
}
