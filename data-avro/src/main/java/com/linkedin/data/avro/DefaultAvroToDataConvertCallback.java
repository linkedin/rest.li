package com.linkedin.data.avro;

import com.linkedin.data.Data;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaConstants;
import com.linkedin.data.schema.DataSchemaTraverse;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.UnionDataSchema;
import java.util.List;


/**
 * Translate values from Avro {@link org.apache.avro.Schema} format to Pegasus {@link DataSchema} format.
 */
class DefaultAvroToDataConvertCallback extends AbstractDefaultDataTranslator implements DataSchemaTraverse.Callback
{
  static final DefaultAvroToDataConvertCallback INSTANCE = new DefaultAvroToDataConvertCallback();

  private DefaultAvroToDataConvertCallback()
  {
  }

  @Override
  public void callback(List<String> path, DataSchema schema)
  {
    if (schema.getType() != DataSchema.Type.RECORD)
    {
      return;
    }
    RecordDataSchema recordSchema = (RecordDataSchema) schema;
    for (RecordDataSchema.Field field : recordSchema.getFields())
    {
      Object defaultData = field.getDefault();
      if (defaultData != null)
      {
        path.add(DataSchemaConstants.DEFAULT_KEY);
        Object newDefault = translateField(pathList(path), defaultData, field);
        path.remove(path.size() - 1);
        field.setDefault(newDefault);
      }
    }
  }

  private static final DataMap unionDefaultValue(UnionDataSchema.Member member, Object value)
  {
    DataMap dataMap = new DataMap(2);
    dataMap.put(member.getUnionMemberKey(), value);
    return dataMap;
  }

  @Override
  protected Object translateUnion(List<Object> path, Object value, UnionDataSchema unionDataSchema)
  {
    Object result;
    if (value == Data.NULL)
    {
      result = value;
    }
    else
    {
      // member type is always the 1st member of the union.
      UnionDataSchema.Member member = unionDataSchema.getMembers().get(0);
      result = unionDefaultValue(member, value);
      path.add(member.getUnionMemberKey());
      translate(path, value, member.getType());
      path.remove(path.size() - 1);
    }
    return result;
  }

  @Override
  protected Object translateField(List<Object> path, Object fieldValue, RecordDataSchema.Field field)
  {
    DataSchema fieldDataSchema = field.getType();
    boolean isOptional = field.getOptional();
    Object result;
    if (isOptional && ((fieldValue == null) || (fieldValue == Data.NULL)))
    {
      // for optional fields,
      // null union members have been removed from translated union schema
      // default value of null should also be removed, make it so that there is no default

      result = null;
    }
    else
    {
      result = translate(path, fieldValue, fieldDataSchema);
    }

    return result;
  }
}
