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

package com.linkedin.data.schema;

import com.linkedin.data.ByteString;
import com.linkedin.data.element.DataElement;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * @author Keren Jin
 */
public class DataSchemaUtil
{
  public static PrimitiveDataSchema typeStringToPrimitiveDataSchema(String type)
  {
    return _TYPE_STRING_TO_PRIMITIVE_DATA_SCHEMA_MAP.get(type);
  }

  public static PrimitiveDataSchema classToPrimitiveDataSchema(Class<?> clazz)
  {
    return _JAVA_TYPE_TO_PRIMITIVE_DATA_SCHEMA_TYPE.get(clazz);
  }

  public static PrimitiveDataSchema dataSchemaTypeToPrimitiveDataSchema(DataSchema.Type type)
  {
    return _DATA_SCHEMA_TYPE_TO_PRIMITIVE_DATA_SCHEMA_MAP.get(type);
  }

  public static Class<?> dataSchemaTypeToPrimitiveDataSchemaClass(DataSchema.Type type)
  {
    return _DATA_SCHEMA_TYPE_TO_PRIMITIVE_JAVA_TYPE_MAP.get(type);
  }

  public static DataSchema.Type typeStringToComplexDataSchemaType(String type)
  {
    return _TYPE_STRING_TO_COMPLEX_DATA_SCHEMA_TYPE_MAP.get(type);
  }

  /**
   * Determines whether a path is valid according to the given data schema.
   * The path must be a field of a record, and not an enum constant, a member of a union, etc.
   * Wild card (*) for array indices and map keys are accepted.
   */
  public static boolean containsPath(DataSchema schema, String path)
  {
    return getField(schema, path) != null;
  }

  /**
   * Same as {@link #getField(DataSchema, Object[])}, but with the path as string.
   */
  public static RecordDataSchema.Field getField(DataSchema schema, String path)
  {
    // Discard the initial / character if present
    if (path.length() > 0 && path.charAt(0) == DataElement.SEPARATOR)
    {
      path = path.substring(1);
    }
    return getField(schema, path.split(DataElement.SEPARATOR.toString()));
  }

  /**
   * Finds the {@link RecordDataSchema.Field} corresponding to the specified path by
   * following the path starting from the provided {@link DataSchema} as the root.
   *
   * @return field object, or null if the field is not found
   */
  public static RecordDataSchema.Field getField(DataSchema schema, Object[] path)
  {
    RecordDataSchema.Field field = null;
    DataSchema dataSchema = schema;
    for (int i = 0; i < path.length; i++)
    {
      dataSchema = dataSchema.getDereferencedDataSchema();
      switch (dataSchema.getType())
      {
        case MAP:
          dataSchema = ((MapDataSchema) dataSchema).getValues();
          break;
        case ARRAY:
          dataSchema = ((ArrayDataSchema) dataSchema).getItems();
          break;
        case RECORD:
          field = ((RecordDataSchema) dataSchema).getField(path[i].toString());
          if (i == path.length - 1) return field;
          if (field == null) return null;
          dataSchema = field.getType();
          break;
        case UNION:
          UnionDataSchema unionDataSchema = (UnionDataSchema) dataSchema;
          dataSchema = unionDataSchema.getTypeByMemberKey(path[i].toString());
          if (dataSchema == null) return null;
          break;
        default:
          // Parent schema cannot be a primitive
          return null;
      }
    }
    return null;
  }

  private DataSchemaUtil() {}

  static final Map<String, PrimitiveDataSchema> _TYPE_STRING_TO_PRIMITIVE_DATA_SCHEMA_MAP;
  static final Map<Class<?>, PrimitiveDataSchema> _JAVA_TYPE_TO_PRIMITIVE_DATA_SCHEMA_TYPE;
  static final Map<DataSchema.Type, PrimitiveDataSchema> _DATA_SCHEMA_TYPE_TO_PRIMITIVE_DATA_SCHEMA_MAP;
  static final Map<DataSchema.Type, Class<?>> _DATA_SCHEMA_TYPE_TO_PRIMITIVE_JAVA_TYPE_MAP;
  static final Map<String, DataSchema.Type> _TYPE_STRING_TO_COMPLEX_DATA_SCHEMA_TYPE_MAP;

  static
  {
    _TYPE_STRING_TO_PRIMITIVE_DATA_SCHEMA_MAP = new HashMap<String, PrimitiveDataSchema>();
    _TYPE_STRING_TO_PRIMITIVE_DATA_SCHEMA_MAP.put(DataSchemaConstants.NULL_TYPE, DataSchemaConstants.NULL_DATA_SCHEMA);
    _TYPE_STRING_TO_PRIMITIVE_DATA_SCHEMA_MAP.put(DataSchemaConstants.BOOLEAN_TYPE, DataSchemaConstants.BOOLEAN_DATA_SCHEMA);
    _TYPE_STRING_TO_PRIMITIVE_DATA_SCHEMA_MAP.put(DataSchemaConstants.INTEGER_TYPE, DataSchemaConstants.INTEGER_DATA_SCHEMA);
    _TYPE_STRING_TO_PRIMITIVE_DATA_SCHEMA_MAP.put(DataSchemaConstants.LONG_TYPE, DataSchemaConstants.LONG_DATA_SCHEMA);
    _TYPE_STRING_TO_PRIMITIVE_DATA_SCHEMA_MAP.put(DataSchemaConstants.FLOAT_TYPE, DataSchemaConstants.FLOAT_DATA_SCHEMA);
    _TYPE_STRING_TO_PRIMITIVE_DATA_SCHEMA_MAP.put(DataSchemaConstants.DOUBLE_TYPE, DataSchemaConstants.DOUBLE_DATA_SCHEMA);
    _TYPE_STRING_TO_PRIMITIVE_DATA_SCHEMA_MAP.put(DataSchemaConstants.BYTES_TYPE, DataSchemaConstants.BYTES_DATA_SCHEMA);
    _TYPE_STRING_TO_PRIMITIVE_DATA_SCHEMA_MAP.put(DataSchemaConstants.STRING_TYPE, DataSchemaConstants.STRING_DATA_SCHEMA);

    _JAVA_TYPE_TO_PRIMITIVE_DATA_SCHEMA_TYPE = new HashMap<Class<?>, PrimitiveDataSchema>(32);
    _JAVA_TYPE_TO_PRIMITIVE_DATA_SCHEMA_TYPE.put(Integer.class, DataSchemaConstants.INTEGER_DATA_SCHEMA);
    _JAVA_TYPE_TO_PRIMITIVE_DATA_SCHEMA_TYPE.put(int.class, DataSchemaConstants.INTEGER_DATA_SCHEMA);
    _JAVA_TYPE_TO_PRIMITIVE_DATA_SCHEMA_TYPE.put(Long.class, DataSchemaConstants.LONG_DATA_SCHEMA);
    _JAVA_TYPE_TO_PRIMITIVE_DATA_SCHEMA_TYPE.put(long.class, DataSchemaConstants.LONG_DATA_SCHEMA);
    _JAVA_TYPE_TO_PRIMITIVE_DATA_SCHEMA_TYPE.put(Short.class, DataSchemaConstants.INTEGER_DATA_SCHEMA);
    _JAVA_TYPE_TO_PRIMITIVE_DATA_SCHEMA_TYPE.put(short.class, DataSchemaConstants.INTEGER_DATA_SCHEMA);
    _JAVA_TYPE_TO_PRIMITIVE_DATA_SCHEMA_TYPE.put(Float.class, DataSchemaConstants.FLOAT_DATA_SCHEMA);
    _JAVA_TYPE_TO_PRIMITIVE_DATA_SCHEMA_TYPE.put(float.class, DataSchemaConstants.FLOAT_DATA_SCHEMA);
    _JAVA_TYPE_TO_PRIMITIVE_DATA_SCHEMA_TYPE.put(Double.class, DataSchemaConstants.DOUBLE_DATA_SCHEMA);
    _JAVA_TYPE_TO_PRIMITIVE_DATA_SCHEMA_TYPE.put(double.class, DataSchemaConstants.DOUBLE_DATA_SCHEMA);
    _JAVA_TYPE_TO_PRIMITIVE_DATA_SCHEMA_TYPE.put(Boolean.class, DataSchemaConstants.BOOLEAN_DATA_SCHEMA);
    _JAVA_TYPE_TO_PRIMITIVE_DATA_SCHEMA_TYPE.put(boolean.class, DataSchemaConstants.BOOLEAN_DATA_SCHEMA);
    _JAVA_TYPE_TO_PRIMITIVE_DATA_SCHEMA_TYPE.put(String.class, DataSchemaConstants.STRING_DATA_SCHEMA);
    _JAVA_TYPE_TO_PRIMITIVE_DATA_SCHEMA_TYPE.put(ByteString.class, DataSchemaConstants.BYTES_DATA_SCHEMA);

    _DATA_SCHEMA_TYPE_TO_PRIMITIVE_DATA_SCHEMA_MAP = new IdentityHashMap<DataSchema.Type, PrimitiveDataSchema>();
    _DATA_SCHEMA_TYPE_TO_PRIMITIVE_DATA_SCHEMA_MAP.put(DataSchema.Type.NULL, DataSchemaConstants.NULL_DATA_SCHEMA);
    _DATA_SCHEMA_TYPE_TO_PRIMITIVE_DATA_SCHEMA_MAP.put(DataSchema.Type.BOOLEAN, DataSchemaConstants.BOOLEAN_DATA_SCHEMA);
    _DATA_SCHEMA_TYPE_TO_PRIMITIVE_DATA_SCHEMA_MAP.put(DataSchema.Type.INT, DataSchemaConstants.INTEGER_DATA_SCHEMA);
    _DATA_SCHEMA_TYPE_TO_PRIMITIVE_DATA_SCHEMA_MAP.put(DataSchema.Type.LONG, DataSchemaConstants.LONG_DATA_SCHEMA);
    _DATA_SCHEMA_TYPE_TO_PRIMITIVE_DATA_SCHEMA_MAP.put(DataSchema.Type.FLOAT, DataSchemaConstants.FLOAT_DATA_SCHEMA);
    _DATA_SCHEMA_TYPE_TO_PRIMITIVE_DATA_SCHEMA_MAP.put(DataSchema.Type.DOUBLE, DataSchemaConstants.DOUBLE_DATA_SCHEMA);
    _DATA_SCHEMA_TYPE_TO_PRIMITIVE_DATA_SCHEMA_MAP.put(DataSchema.Type.BYTES, DataSchemaConstants.BYTES_DATA_SCHEMA);
    _DATA_SCHEMA_TYPE_TO_PRIMITIVE_DATA_SCHEMA_MAP.put(DataSchema.Type.STRING, DataSchemaConstants.STRING_DATA_SCHEMA);

    _DATA_SCHEMA_TYPE_TO_PRIMITIVE_JAVA_TYPE_MAP = new IdentityHashMap<DataSchema.Type, Class<?>>();
    _DATA_SCHEMA_TYPE_TO_PRIMITIVE_JAVA_TYPE_MAP.put(DataSchema.Type.INT, Integer.class);
    _DATA_SCHEMA_TYPE_TO_PRIMITIVE_JAVA_TYPE_MAP.put(DataSchema.Type.LONG, Long.class);
    _DATA_SCHEMA_TYPE_TO_PRIMITIVE_JAVA_TYPE_MAP.put(DataSchema.Type.FLOAT, Float.class);
    _DATA_SCHEMA_TYPE_TO_PRIMITIVE_JAVA_TYPE_MAP.put(DataSchema.Type.DOUBLE, Double.class);
    _DATA_SCHEMA_TYPE_TO_PRIMITIVE_JAVA_TYPE_MAP.put(DataSchema.Type.BOOLEAN, Boolean.class);
    _DATA_SCHEMA_TYPE_TO_PRIMITIVE_JAVA_TYPE_MAP.put(DataSchema.Type.STRING, String.class);
    _DATA_SCHEMA_TYPE_TO_PRIMITIVE_JAVA_TYPE_MAP.put(DataSchema.Type.BYTES, ByteString.class);

    _TYPE_STRING_TO_COMPLEX_DATA_SCHEMA_TYPE_MAP = new HashMap<String, DataSchema.Type>();
    _TYPE_STRING_TO_COMPLEX_DATA_SCHEMA_TYPE_MAP.put(DataSchemaConstants.ARRAY_TYPE, DataSchema.Type.ARRAY);
    _TYPE_STRING_TO_COMPLEX_DATA_SCHEMA_TYPE_MAP.put(DataSchemaConstants.ENUM_TYPE, DataSchema.Type.ENUM);
    _TYPE_STRING_TO_COMPLEX_DATA_SCHEMA_TYPE_MAP.put(DataSchemaConstants.ERROR_TYPE, DataSchema.Type.RECORD);
    _TYPE_STRING_TO_COMPLEX_DATA_SCHEMA_TYPE_MAP.put(DataSchemaConstants.FIXED_TYPE, DataSchema.Type.FIXED);
    _TYPE_STRING_TO_COMPLEX_DATA_SCHEMA_TYPE_MAP.put(DataSchemaConstants.MAP_TYPE, DataSchema.Type.MAP);
    _TYPE_STRING_TO_COMPLEX_DATA_SCHEMA_TYPE_MAP.put(DataSchemaConstants.RECORD_TYPE, DataSchema.Type.RECORD);
    _TYPE_STRING_TO_COMPLEX_DATA_SCHEMA_TYPE_MAP.put(DataSchemaConstants.TYPEREF_TYPE,  DataSchema.Type.TYPEREF);
  }
}
