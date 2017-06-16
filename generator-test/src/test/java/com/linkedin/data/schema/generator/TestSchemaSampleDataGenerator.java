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
import com.linkedin.data.Data;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaConstants;
import com.linkedin.data.schema.DataSchemaUtil;
import com.linkedin.data.schema.EnumDataSchema;
import com.linkedin.data.schema.FixedDataSchema;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.PrimitiveDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.TyperefDataSchema;
import com.linkedin.data.schema.UnionDataSchema;
import com.linkedin.data.schema.validation.ValidateDataAgainstSchema;
import com.linkedin.data.schema.validation.ValidationOptions;
import com.linkedin.data.schema.validation.ValidationResult;
import com.linkedin.data.template.BooleanArray;
import com.linkedin.data.template.BooleanMap;
import com.linkedin.data.template.BytesArray;
import com.linkedin.data.template.BytesMap;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.DirectArrayTemplate;
import com.linkedin.data.template.DirectMapTemplate;
import com.linkedin.data.template.DoubleArray;
import com.linkedin.data.template.DoubleMap;
import com.linkedin.data.template.FloatArray;
import com.linkedin.data.template.FloatMap;
import com.linkedin.data.template.IntegerArray;
import com.linkedin.data.template.IntegerMap;
import com.linkedin.data.template.LongArray;
import com.linkedin.data.template.LongMap;
import com.linkedin.data.template.StringArray;
import com.linkedin.data.template.StringMap;
import com.linkedin.pegasus.generator.test.Certification;
import com.linkedin.pegasus.generator.test.EnumFruits;
import com.linkedin.pegasus.generator.test.FixedMD5;
import com.linkedin.pegasus.generator.test.InvalidSelfReference;
import com.linkedin.pegasus.generator.test.SelfReference;
import com.linkedin.pegasus.generator.test.TyperefTest;
import com.linkedin.pegasus.generator.test.UnionTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

public class TestSchemaSampleDataGenerator
{
  @Test
  public void testPrimitiveSchema()
  {
    for (Map.Entry<DataSchema.Type, Class<?>> entry: _dataSchemaTypeToPrimitiveJavaTypeMap.entrySet())
    {
      final PrimitiveDataSchema schema = DataSchemaUtil.dataSchemaTypeToPrimitiveDataSchema(entry.getKey());
      final Object value = SchemaSampleDataGenerator.buildData(schema, _spec);
      Assert.assertSame(value.getClass(), entry.getValue());
    }

    final PrimitiveDataSchema nullSchema = DataSchemaConstants.NULL_DATA_SCHEMA;
    final Object nullData = SchemaSampleDataGenerator.buildData(nullSchema, _spec);
    Assert.assertEquals(nullData, Data.NULL);
  }

  @Test
  public void testArraySchema()
  {
    for (Map.Entry<DataSchema.Type, Class<? extends DirectArrayTemplate<?>>> entry: _dataSchemaTypeToprimitiveArrayMap.entrySet())
    {
      final PrimitiveDataSchema itemsSchema = DataSchemaUtil.dataSchemaTypeToPrimitiveDataSchema(entry.getKey());
      final ArrayDataSchema arraySchema = new ArrayDataSchema(itemsSchema);
      final DataList value = (DataList) SchemaSampleDataGenerator.buildData(arraySchema, _spec);
      final ParameterizedType arrayType = (ParameterizedType) entry.getValue().getGenericSuperclass();
      assert(arrayType.getRawType() == DirectArrayTemplate.class);
      Assert.assertSame(value.get(0).getClass(), arrayType.getActualTypeArguments()[0]);
    }
  }

  @Test
  public void testMapSchema()
  {
    for (Map.Entry<DataSchema.Type, Class<? extends DirectMapTemplate<?>>> entry: _dataSchemaTypeToprimitiveMapMap.entrySet())
    {
      final PrimitiveDataSchema valueSchema = DataSchemaUtil.dataSchemaTypeToPrimitiveDataSchema(entry.getKey());
      final MapDataSchema mapSchema = new MapDataSchema(valueSchema);
      final DataMap value = (DataMap) SchemaSampleDataGenerator.buildData(mapSchema, _spec);
      final ParameterizedType mapType = (ParameterizedType) entry.getValue().getGenericSuperclass();
      assert(mapType.getRawType() == DirectMapTemplate.class);
      Assert.assertSame(value.values().iterator().next().getClass(), mapType.getActualTypeArguments()[0]);
    }
  }

  @Test
  public void testFixedSchema()
  {
    final FixedDataSchema schema = (FixedDataSchema) DataTemplateUtil.getSchema(FixedMD5.class);
    final ByteString value = (ByteString) SchemaSampleDataGenerator.buildData(schema, _spec);
    Assert.assertSame(value.length(), schema.getSize());
  }

  @Test
  public void testEnumSchema()
  {
    final EnumDataSchema schema = (EnumDataSchema) DataTemplateUtil.getSchema(EnumFruits.class);
    final String value = (String) SchemaSampleDataGenerator.buildData(schema, _spec);
    Assert.assertSame(schema.getSymbolDocs().size(), EnumFruits.class.getEnumConstants().length - 1/*The $UNKNOWN value*/);
    EnumFruits.valueOf(value);
  }

  @Test
  public void testRecordSchema()
  {
    final RecordDataSchema schema = (RecordDataSchema) DataTemplateUtil.getSchema(Certification.class);
    final DataMap value = SchemaSampleDataGenerator.buildRecordData(schema, _spec);
    final ValidationResult result = ValidateDataAgainstSchema.validate(value, schema, new ValidationOptions());
    Assert.assertTrue(result.isValid(), Arrays.toString(result.getMessages().toArray()));
  }

  @Test
  public void testUnionSchema()
  {
    final UnionDataSchema schema = (UnionDataSchema) DataTemplateUtil.getSchema(UnionTest.UnionWithNull.class);
    final Set<String> memberKeys = new HashSet<String>();
    for (UnionDataSchema.Member member: schema.getMembers())
    {
      memberKeys.add(member.getUnionMemberKey());
    }
    final String nullMemberKey = DataSchemaConstants.NULL_DATA_SCHEMA.getUnionMemberKey();

    for (int i = 0; i < memberKeys.size() * 2; ++i)
    {
      final DataMap value = (DataMap) SchemaSampleDataGenerator.buildData(schema, _spec);
      if (value == null)
      {
        Assert.assertTrue(memberKeys.contains(nullMemberKey));
        continue;
      }

      final String key = value.keySet().iterator().next();
      Assert.assertTrue(memberKeys.contains(key));
    }
  }

  @Test
  public void testTyperefSchema()
  {
    final RecordDataSchema schema = (RecordDataSchema) DataTemplateUtil.getSchema(TyperefTest.class);
    final DataMap value = SchemaSampleDataGenerator.buildRecordData(schema, _spec);

    for(RecordDataSchema.Field field: schema.getFields())
    {
      final DataSchema fieldSchema = field.getType();
      if (!(fieldSchema instanceof TyperefDataSchema))
      {
        continue;
      }

      final TyperefDataSchema fieldTyperefSchema = (TyperefDataSchema) field.getType();
      final Object fieldValue = value.get(field.getName());
      final Object rebuildValue = SchemaSampleDataGenerator.buildData(fieldTyperefSchema.getDereferencedDataSchema(),
                                                                      _spec);
      Assert.assertSame(fieldValue.getClass(), rebuildValue.getClass());
    }
  }

  @Test
  public void testInvalidRecursivelyReferencedSchema() {
    try
    {
      // this schema is invalid because it contains a non-optional reference to itself
      final RecordDataSchema schema = (RecordDataSchema) DataTemplateUtil.getSchema(InvalidSelfReference.class);
      SchemaSampleDataGenerator.buildRecordData(schema, _spec);
      Assert.fail("IllegalArgumentException should be thrown because schema contains schema that references itself and is not optional, or in a list, map or union.");
    }
    catch (IllegalArgumentException e)
    {
    }
  }

  @Test
  public void testRecursivelyReferencedSchema()
  {
    try
    {
      final RecordDataSchema schema = (RecordDataSchema) DataTemplateUtil.getSchema(SelfReference.class);
      final DataMap data = SchemaSampleDataGenerator.buildRecordData(schema, _spec);
      Assert.assertTrue(data.getDataList("listRef").getDataMap(0).getDataList("listRef").isEmpty(), "Self referenced schema in list should not be embedded recursively");
      final String firstKey = data.getDataMap("mapRef").keySet().iterator().next();
      Assert.assertTrue(data.getDataMap("mapRef").getDataMap(firstKey).getDataMap("mapRef").isEmpty(), "Self referenced schema in map should not be embedded recursively");
      Assert.assertFalse(data.getDataMap("indirectRef").containsKey("ref"), "Self referenced schema (via indirect reference) should not be embedded recursively");
      Assert.assertFalse(data.getDataMap("unionRef").containsKey("com.linkedin.pegasus.generator.test.SelfReference"), "Self referenced schema in union should not be embedded recursively");
    }
    catch (StackOverflowError e)
    {
      Assert.fail("Self reference in schema should not cause stack overflow during doc gen.");
    }
  }

  private static final Map<DataSchema.Type, Class<?>> _dataSchemaTypeToPrimitiveJavaTypeMap =
      new IdentityHashMap<DataSchema.Type, Class<?>>();
  private static final Map<DataSchema.Type, Class<? extends DirectArrayTemplate<?>>> _dataSchemaTypeToprimitiveArrayMap =
      new IdentityHashMap<DataSchema.Type, Class<? extends DirectArrayTemplate<?>>>();
  private static final Map<DataSchema.Type, Class<? extends DirectMapTemplate<?>>> _dataSchemaTypeToprimitiveMapMap =
      new IdentityHashMap<DataSchema.Type, Class<? extends DirectMapTemplate<?>>>();
  private static final SchemaSampleDataGenerator.DataGenerationOptions _spec =
      new SchemaSampleDataGenerator.DataGenerationOptions();
  static
  {
    _dataSchemaTypeToPrimitiveJavaTypeMap.put(DataSchema.Type.INT, Integer.class);
    _dataSchemaTypeToPrimitiveJavaTypeMap.put(DataSchema.Type.LONG, Long.class);
    _dataSchemaTypeToPrimitiveJavaTypeMap.put(DataSchema.Type.FLOAT, Float.class);
    _dataSchemaTypeToPrimitiveJavaTypeMap.put(DataSchema.Type.DOUBLE, Double.class);
    _dataSchemaTypeToPrimitiveJavaTypeMap.put(DataSchema.Type.BOOLEAN, Boolean.class);
    _dataSchemaTypeToPrimitiveJavaTypeMap.put(DataSchema.Type.STRING, String.class);
    _dataSchemaTypeToPrimitiveJavaTypeMap.put(DataSchema.Type.BYTES, ByteString.class);

    _dataSchemaTypeToprimitiveArrayMap.put(DataSchema.Type.BOOLEAN, BooleanArray.class);
    _dataSchemaTypeToprimitiveArrayMap.put(DataSchema.Type.INT, IntegerArray.class);
    _dataSchemaTypeToprimitiveArrayMap.put(DataSchema.Type.LONG, LongArray.class);
    _dataSchemaTypeToprimitiveArrayMap.put(DataSchema.Type.FLOAT, FloatArray.class);
    _dataSchemaTypeToprimitiveArrayMap.put(DataSchema.Type.DOUBLE, DoubleArray.class);
    _dataSchemaTypeToprimitiveArrayMap.put(DataSchema.Type.STRING, StringArray.class);
    _dataSchemaTypeToprimitiveArrayMap.put(DataSchema.Type.BYTES, BytesArray.class);

    _dataSchemaTypeToprimitiveMapMap.put(DataSchema.Type.BOOLEAN, BooleanMap.class);
    _dataSchemaTypeToprimitiveMapMap.put(DataSchema.Type.INT, IntegerMap.class);
    _dataSchemaTypeToprimitiveMapMap.put(DataSchema.Type.LONG, LongMap.class);
    _dataSchemaTypeToprimitiveMapMap.put(DataSchema.Type.FLOAT, FloatMap.class);
    _dataSchemaTypeToprimitiveMapMap.put(DataSchema.Type.DOUBLE, DoubleMap.class);
    _dataSchemaTypeToprimitiveMapMap.put(DataSchema.Type.STRING, StringMap.class);
    _dataSchemaTypeToprimitiveMapMap.put(DataSchema.Type.BYTES, BytesMap.class);
  }
}
