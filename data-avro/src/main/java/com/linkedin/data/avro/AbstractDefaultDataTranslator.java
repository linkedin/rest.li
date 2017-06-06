package com.linkedin.data.avro;

import com.linkedin.data.ByteString;
import com.linkedin.data.Data;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.message.Message;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.EnumDataSchema;
import com.linkedin.data.schema.FixedDataSchema;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.UnionDataSchema;
import java.util.List;
import java.util.Map;


/**
 * Abstract class for translating default values from Pegasus and Avro format and vice versa. This will be used while
 * translating the schema from one format to the other. The concrete implementation will have to provide specif
 */
abstract class AbstractDefaultDataTranslator
{
  protected abstract Object translateField(List<Object> path, Object fieldValue, RecordDataSchema.Field field);
  protected abstract Object translateUnion(List<Object> path, Object value, UnionDataSchema unionDataSchema);

  protected Object translate(List<Object> path, Object value, DataSchema dataSchema)
  {
    dataSchema = dataSchema.getDereferencedDataSchema();
    DataSchema.Type type = dataSchema.getType();
    Object result;
    switch (type)
    {
      case NULL:
        if (value != Data.NULL)
        {
          throw new IllegalArgumentException(message(path, "value must be null for null schema"));
        }
        result = value;
        break;
      case BOOLEAN:
        result = ((Boolean) value).booleanValue();
        break;
      case INT:
        result = ((Number) value).intValue();
        break;
      case LONG:
        result = ((Number) value).longValue();
        break;
      case FLOAT:
        result = ((Number) value).floatValue();
        break;
      case DOUBLE:
        result = ((Number) value).doubleValue();
        break;
      case STRING:
        result = (String) value;
        break;
      case BYTES:
        Class<?> clazz = value.getClass();
        if (clazz != String.class && clazz != ByteString.class)
        {
          throw new IllegalArgumentException(message(path, "bytes value %1$s is not a String or ByteString", value));
        }
        result = value;
        break;
      case ENUM:
        String enumValue = (String) value;
        EnumDataSchema enumDataSchema = (EnumDataSchema) dataSchema;
        if (!enumDataSchema.getSymbols().contains(enumValue))
        {
          throw new IllegalArgumentException(message(path, "enum value %1$s not one of %2$s", value, enumDataSchema.getSymbols()));
        }
        result = value;
        break;
      case FIXED:
        clazz = value.getClass();
        ByteString byteString;
        if (clazz == String.class)
        {
          byteString = ByteString.copyAvroString((String) value, true);
        }
        else if (clazz == ByteString.class)
        {
          byteString = (ByteString) value;
        }
        else
        {
          throw new IllegalArgumentException(message(path, "fixed value %1$s is not a String or ByteString", value));
        }
        FixedDataSchema fixedDataSchema = (FixedDataSchema) dataSchema;
        if (fixedDataSchema.getSize() != byteString.length())
        {
          throw new IllegalArgumentException(message(path,
              "ByteString size %1$d != FixedDataSchema size %2$d",
              byteString.length(),
              fixedDataSchema.getSize()));
        }
        result = byteString;
        break;
      case MAP:
        DataMap map = (DataMap) value;
        DataSchema valueDataSchema = ((MapDataSchema) dataSchema).getValues();
        Map<String, Object> resultMap = new DataMap(map.size() * 2);
        for (Map.Entry<String, Object> entry : map.entrySet())
        {
          String key = entry.getKey();
          path.add(key);
          Object entryAvroValue = translate(path, entry.getValue(), valueDataSchema);
          path.remove(path.size() - 1);
          resultMap.put(key, entryAvroValue);
        }
        result = resultMap;
        break;
      case ARRAY:
        DataList list = (DataList) value;
        DataList resultList = new DataList(list.size());
        DataSchema elementDataSchema = ((ArrayDataSchema) dataSchema).getItems();
        for (int i = 0; i < list.size(); i++)
        {
          path.add(i);
          Object entryAvroValue = translate(path, list.get(i), elementDataSchema);
          path.remove(path.size() - 1);
          resultList.add(entryAvroValue);
        }
        result = resultList;
        break;
      case RECORD:
        DataMap recordMap = (DataMap) value;
        RecordDataSchema recordDataSchema = (RecordDataSchema) dataSchema;
        DataMap resultRecordMap = new DataMap(recordDataSchema.getFields().size() * 2);
        for (RecordDataSchema.Field field : recordDataSchema.getFields())
        {
          String fieldName = field.getName();
          Object fieldValue = recordMap.get(fieldName);
          path.add(fieldName);
          Object resultFieldValue = translateField(path, fieldValue, field);
          path.remove(path.size() - 1);
          if (resultFieldValue != null)
          {
            resultRecordMap.put(fieldName, resultFieldValue);
          }
        }
        result = resultRecordMap;
        break;
      case UNION:
        result = translateUnion(path, value, (UnionDataSchema) dataSchema);
        break;
      default:
        throw new IllegalStateException(message(path, "schema type unknown %1$s", type));
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  final List<Object> pathList(List<String> path)
  {
    return (List<Object>) ((List) path);
  }

  final String message(List<?> path, String format, Object... args)
  {
    Message message = new Message(path.toArray(), format, args);
    return message.toString();
  }
}
