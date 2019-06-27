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

import com.linkedin.data.ByteString;
import com.linkedin.data.Data;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.element.DataElement;
import com.linkedin.data.message.Message;
import com.linkedin.data.message.MessageList;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaConstants;
import com.linkedin.data.schema.EnumDataSchema;
import com.linkedin.data.schema.FixedDataSchema;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.UnionDataSchema;
import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericFixed;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.util.Utf8;


/**
 * Translate from Pegasus data to and from Avro {@link GenericRecord}.
 *
 * @see SchemaTranslationException
 */
public class DataTranslator implements DataTranslatorContext
{
  protected DataTranslationOptions _dataTranslationOptions;

  /**
   * Convert the given {@link DataMap} conforming to the provided {@link RecordDataSchema} to a {@link GenericRecord}.
   *
   * <p>
   * The Avro schema of the output {@link GenericRecord} is derived from the provided {@link RecordDataSchema}.
   *
   * @param map provides the {@link DataMap} to translate.
   * @param dataSchema provides the {@link RecordDataSchema} for the {@link DataMap}.
   * @return a translated {@link GenericRecord}.
   * @throws DataTranslationException if there are errors that prevent translation.
   */
  public static GenericRecord dataMapToGenericRecord(DataMap map, RecordDataSchema dataSchema) throws DataTranslationException
  {
    Schema avroSchema = SchemaTranslator.dataToAvroSchema(dataSchema);
    return dataMapToGenericRecord(map, dataSchema, avroSchema, null);
  }

  /**
   * Convert the given {@link DataMap} conforming to the provided {@link RecordDataSchema}
   * to a {@link GenericRecord} with the provided Avro {@link Schema}.
   *
   * <p>
   * The provided Avro {@link Schema} should be generated from a record schema that
   * is compatible with the provided {@link RecordDataSchema} using {@link SchemaTranslator}.
   * If this is not the case, then data translation is likely to fail.
   *
   * @param map provides the {@link DataMap} to translate.
   * @param dataSchema provides the {@link RecordDataSchema} for the {@link DataMap}.
   * @param avroSchema the Avro {@link Schema} for the resulting {@link GenericRecord}.
   * @param options the DataMapToAvroRecordTranslationOptions {@link DataMapToAvroRecordTranslationOptions}
   * @return a translated {@link GenericRecord}.
   * @throws DataTranslationException if there are errors that prevent translation.
   */
  public static GenericRecord dataMapToGenericRecord(DataMap map, RecordDataSchema dataSchema, Schema avroSchema, DataMapToAvroRecordTranslationOptions options) throws DataTranslationException
  {
    DataMapToGenericRecordTranslator translator = new DataMapToGenericRecordTranslator(options);
    try
    {
      GenericRecord avroRecord = (GenericRecord) translator.translate(map, dataSchema, avroSchema);
      translator.checkMessageListForErrorsAndThrowDataTranslationException();
      return avroRecord;
    }
    catch (RuntimeException e)
    {
      throw translator.dataTranslationException(e);
    }
  }

  /**
   * Convert the given {@link DataMap} conforming to the provided {@link RecordDataSchema}
   * to a {@link GenericRecord} with the provided Avro {@link Schema}.
   *
   * <p>
   * The provided Avro {@link Schema} should be generated from a record schema that
   * is compatible with the provided {@link RecordDataSchema} using {@link SchemaTranslator}.
   * If this is not the case, then data translation is likely to fail.
   *
   * @param map provides the {@link DataMap} to translate.
   * @param dataSchema provides the {@link RecordDataSchema} for the {@link DataMap}.
   * @param avroSchema the Avro {@link Schema} for the resulting {@link GenericRecord}.
   * @return a translated {@link GenericRecord}.
   * @throws DataTranslationException if there are errors that prevent translation.
   */
  public static GenericRecord dataMapToGenericRecord(DataMap map, RecordDataSchema dataSchema, Schema avroSchema) throws DataTranslationException
  {
    return dataMapToGenericRecord(map, dataSchema, avroSchema, null);
  }

  /**
   * Translate the {@link GenericRecord} to a {@link DataMap}.
   *
   * @param record provides the {@link GenericRecord} to translate.
   * @param dataSchema provides the {@link RecordDataSchema} to translate to.
   * @param avroSchema provides the Avro {@link Schema} corresponding to the provided {@link RecordDataSchema}.
   * @return a translated {@link DataMap}.
   * @throws DataTranslationException if there are errors that prevent translation.
   */
  public static DataMap genericRecordToDataMap(GenericRecord record, RecordDataSchema dataSchema, Schema avroSchema) throws DataTranslationException
  {
    return genericRecordToDataMap(record, dataSchema, avroSchema,null);
  }

  /**
   * Translate the {@link GenericRecord} to a {@link DataMap}.
   *
   * @param record provides the {@link GenericRecord} to translate.
   * @param dataSchema provides the {@link RecordDataSchema} to translate to.
   * @param avroSchema provides the Avro {@link Schema} corresponding to the provided {@link RecordDataSchema}.
   * @param options the AvroRecordToDataMapTranslationOptions {@link AvroRecordToDataMapTranslationOptions}
   * @return a translated {@link DataMap}.
   * @throws DataTranslationException if there are errors that prevent translation.
   */
  public static DataMap genericRecordToDataMap(GenericRecord record, RecordDataSchema dataSchema, Schema avroSchema, AvroRecordToDataMapTranslationOptions options) throws DataTranslationException
  {
    AvroGenericToDataTranslator translator = new AvroGenericToDataTranslator(options);
    try
    {
      DataMap dataMap = (DataMap) translator.translate(record, dataSchema, avroSchema);
      translator.checkMessageListForErrorsAndThrowDataTranslationException();
      return dataMap;
    }
    catch (RuntimeException e)
    {
      throw translator.dataTranslationException(e);
    }
  }

  private static final GenericData _genericData = GenericData.get();

  protected final Deque<Object> _path = new ArrayDeque<Object>();
  protected final MessageList<Message> _messageList = new MessageList<Message>();
  protected final AvroOverrideFactory _avroOverrideFactory = new AvroOverrideFactory()
  {
    {
      setInstantiateCustomDataTranslator(true);
    }

    @Override
    void emitMessage(String format, Object... args)
    {
      appendMessage(format, args);
    }
  };

  protected final AvroOverrideMap _avroOverrideMap = new AvroOverrideMap(_avroOverrideFactory);

  protected DataTranslator()
  {
  }

  protected DataTranslator(DataTranslationOptions options)
  {
    _dataTranslationOptions = options;
  }

  @Override
  public void appendMessage(String format, Object... args)
  {
    _messageList.add(new Message(_path.toArray(), format, args));
  }


  protected AvroOverride getAvroOverride(DataSchema schema)
  {
    return _avroOverrideMap.getAvroOverride(schema);
  }

  protected void checkMessageListForErrorsAndThrowDataTranslationException()
    throws DataTranslationException
  {
    if (_messageList.isEmpty() == false)
    {
      Object[] firstErrorPath = _messageList.get(0).getPath();
      throw new DataTranslationException("Error processing " + pathToString(Arrays.asList(firstErrorPath)), _messageList);
    }
  }

  protected DataTranslationException dataTranslationException(RuntimeException e)
  {
    return new DataTranslationException("Error processing " + pathToString(_path), _messageList, e);
  }

  private static class AvroGenericToDataTranslator extends DataTranslator
  {
    private final static Object BAD_RESULT = CustomDataTranslator.DATA_BAD_RESULT;
    private AvroGenericToDataTranslator(DataTranslationOptions options)
    {
      super(options);
    }

    private Object translate(Object value, DataSchema dataSchema, Schema avroSchema)
    {
      AvroOverride avroOverride = getAvroOverride(dataSchema);
      if (avroOverride != null)
      {
        return avroOverride.getCustomDataTranslator().avroGenericToData(this, value, avroSchema, dataSchema);
      }

      DataSchema dereferencedDataSchema = dataSchema.getDereferencedDataSchema();
      DataSchema.Type type = dereferencedDataSchema.getType();
      Object result;
      switch (type)
      {
        case NULL:
          if (value != null)
          {
            appendMessage("value must be null for null schema");
            result = BAD_RESULT;
            break;
          }
          result = Data.NULL;
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
          result = value.toString();
          break;
        case BYTES:
          ByteBuffer byteBuffer = (ByteBuffer) value;
          ByteString byteString = ByteString.copy(byteBuffer);
          byteBuffer.rewind();
          result = byteString;
          break;
        case ENUM:
          String enumValue = value.toString();
          EnumDataSchema enumDataSchema = (EnumDataSchema) dereferencedDataSchema;
          if (enumDataSchema.getSymbols().contains(enumValue) == false)
          {
            appendMessage("enum value %1$s not one of %2$s", enumValue, enumDataSchema.getSymbols());
            result = BAD_RESULT;
            break;
          }
          result = enumValue;
          break;
        case FIXED:
          GenericFixed fixed = (GenericFixed) value;
          byte[] fixedBytes = fixed.bytes();
          FixedDataSchema fixedDataSchema = (FixedDataSchema) dereferencedDataSchema;
          if (fixedDataSchema.getSize() != fixedBytes.length)
          {
            appendMessage("GenericFixed size %1$d != FixedDataSchema size %2$d",
                          fixedBytes.length,
                          fixedDataSchema.getSize());
            result = BAD_RESULT;
            break;
          }
          byteString = ByteString.copy(fixedBytes);
          result = byteString;
          break;
        case MAP:
          @SuppressWarnings("unchecked")
          Map<?, Object> map = (Map<?, Object>) value;
          DataSchema valueDataSchema = ((MapDataSchema) dereferencedDataSchema).getValues();
          Schema valueAvroSchema = avroSchema.getValueType();
          DataMap dataMap = new DataMap(map.size());
          for (Map.Entry<?, Object> entry : map.entrySet())
          {
            String key = entry.getKey().toString();
            _path.addLast(key);
            Object entryValue = translate(entry.getValue(), valueDataSchema, valueAvroSchema);
            _path.removeLast();
            dataMap.put(key, entryValue);
          }
          result = dataMap;
          break;
        case ARRAY:
          List<?> list = (List<?>) value;
          DataSchema elementDataSchema = ((ArrayDataSchema) dereferencedDataSchema).getItems();
          Schema elementAvroSchema = avroSchema.getElementType();
          DataList dataList = new DataList(list.size());
          for (int i = 0; i < list.size(); i++)
          {
            _path.addLast(i);
            Object entryValue = translate(list.get(i), elementDataSchema, elementAvroSchema);
            _path.removeLast();
            dataList.add(entryValue);
          }
          result = dataList;
          break;
        case RECORD:
          GenericRecord record = (GenericRecord) value;
          RecordDataSchema recordDataSchema = (RecordDataSchema) dereferencedDataSchema;
          dataMap = new DataMap(avroSchema.getFields().size());
          for (RecordDataSchema.Field field : recordDataSchema.getFields())
          {
            String fieldName = field.getName();
            Object fieldValue = record.get(fieldName);
            // fieldValue could be null if the Avro schema does not contain the named field or
            // the field is present with a null value. In either case we do not add a value
            // to the translated DataMap. We do not consider optional/required/default here
            // either (i.e. it is not an error if a required field is missing); the user can
            // later call ValidateDataAgainstSchema with various
            // settings for RequiredMode to obtain the desired behaviour.
            if (fieldValue == null)
            {
              continue;
            }
            boolean isOptional = field.getOptional();
            DataSchema fieldDataSchema = field.getType();
            Schema fieldAvroSchema = avroSchema.getField(fieldName).schema();
            if (isOptional && (fieldDataSchema.getDereferencedType() != DataSchema.Type.UNION))
            {
              // Avro schema should be union with 2 types: null and the field's type.
              Map.Entry<String, Schema> fieldAvroEntry = findUnionMember(fieldDataSchema, fieldAvroSchema);
              if (fieldAvroEntry == null)
              {
                continue;
              }
              fieldAvroSchema = fieldAvroEntry.getValue();
            }
            _path.addLast(fieldName);
            dataMap.put(fieldName, translate(fieldValue, fieldDataSchema, fieldAvroSchema));
            _path.removeLast();
          }
          result = dataMap;
          break;
        case UNION:
          UnionDataSchema unionDataSchema = (UnionDataSchema) dereferencedDataSchema;
          if (unionDataSchema.areMembersAliased())
          {
            // Since Pegasus 'union with aliases' are represented as an Avro record, the translation
            // is handled separately.
            result = translateAvroRecordToPegasusUnionWithAliases(value, unionDataSchema, avroSchema);
          }
          else
          {
            Map.Entry<DataSchema, Schema> memberSchemas = findUnionMemberSchema(value, unionDataSchema, avroSchema);
            if (memberSchemas == null)
            {
              result = BAD_RESULT;
              break;
            }
            if (value == null)
            {
              // schema must be "null" schema
              result = Data.NULL;
            }
            else
            {
              DataSchema memberDataSchema = memberSchemas.getKey();
              Schema memberAvroSchema = memberSchemas.getValue();
              String key = memberDataSchema.getUnionMemberKey();
              dataMap = new DataMap(1);
              _path.addLast(key);
              dataMap.put(key, translate(value, memberDataSchema, memberAvroSchema));
              _path.removeLast();
              result = dataMap;
            }
          }
          break;
        default:
          appendMessage("schema type unknown %1$s", dereferencedDataSchema.getType()) ;
          result = BAD_RESULT;
          break;
      }
      return result;
    }

    private final Map.Entry<DataSchema, Schema> findUnionMemberSchema(Object value, UnionDataSchema unionDataSchema, Schema avroSchema)
    {
      int index = _genericData.resolveUnion(avroSchema, value);
      Schema memberAvroSchema = avroSchema.getTypes().get(index);
      String key;
      switch (memberAvroSchema.getType())
      {
        case ENUM:
        case FIXED:
        case RECORD:
          key = getUnionMemberKey(memberAvroSchema);
          break;
        default:
          key = memberAvroSchema.getType().toString().toLowerCase();
      }
      DataSchema memberDataSchema = unionDataSchema.getTypeByMemberKey(key);
      if (memberDataSchema == null)
      {
        for (UnionDataSchema.Member member : unionDataSchema.getMembers())
        {
          AvroOverride avroOverride = getAvroOverride(member.getType());
          if (avroOverride != null)
          {
            if (avroOverride.getAvroSchemaFullName().equals(key))
            {
              memberDataSchema = member.getType();
              break;
            }
          }
        }
      }
      if (memberDataSchema == null)
      {
        appendMessage("cannot find %1$s in union %2$s for value %3$s", key, unionDataSchema, value);
        return null;
      }
      return new AbstractMap.SimpleEntry<DataSchema, Schema>(memberDataSchema, memberAvroSchema);
    }

    private Object translateAvroRecordToPegasusUnionWithAliases(Object value, UnionDataSchema unionDataSchema, Schema avroSchema)
    {
      Schema recordAvroSchema = extractNonnullSchema(avroSchema);

      GenericRecord record = (GenericRecord) value;
      Object fieldDiscriminatorValue = record.get(DataSchemaConstants.DISCRIMINATOR_FIELD);
      if (fieldDiscriminatorValue == null)
      {
        appendMessage("cannot find required field %1$s in record %2$s", DataSchemaConstants.DISCRIMINATOR_FIELD, record);
        return BAD_RESULT;
      }
      String fieldDiscriminator = fieldDiscriminatorValue.toString();

      if (DataSchemaConstants.NULL_TYPE.equals(fieldDiscriminator))
      {
        return Data.NULL;
      }
      else
      {
        Object fieldValue = record.get(fieldDiscriminator);
        Schema fieldAvroSchema = recordAvroSchema.getField(fieldDiscriminator).schema();
        DataSchema memberDataSchema = unionDataSchema.getTypeByMemberKey(fieldDiscriminator);

        DataMap result = new DataMap(1);
        _path.add(fieldDiscriminator);
        result.put(fieldDiscriminator, translate(fieldValue, memberDataSchema, extractNonnullSchema(fieldAvroSchema)));
        _path.removeLast();
        return result;
      }
    }
  }

  private static class DataMapToGenericRecordTranslator extends DataTranslator
  {
    private static final Object BAD_RESULT = CustomDataTranslator.AVRO_BAD_RESULT;
    private final AvroAdapter _avroAdapter = AvroAdapterFinder.getAvroAdapter();
    private DataMapToGenericRecordTranslator(DataTranslationOptions options)
    {
      super(options);
    }

    private Object translate(Object value, DataSchema dataSchema, Schema avroSchema)
    {
      AvroOverride avroOverride = getAvroOverride(dataSchema);
      if (avroOverride != null)
      {
        return avroOverride.getCustomDataTranslator().dataToAvroGeneric(this, value, dataSchema, avroSchema);
      }

      DataSchema dereferencedDataSchema = dataSchema.getDereferencedDataSchema();
      DataSchema.Type type = dereferencedDataSchema.getType();
      Object result;
      switch (type)
      {
        case NULL:
          if (value != Data.NULL)
          {
            appendMessage("value must be null for null schema");
            result = BAD_RESULT;
            break;
          }
          result = null;
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
          result = new Utf8((String) value);
          break;
        case BYTES:
          result = ByteBuffer.wrap(translateBytes(value));
          break;
        case ENUM:
          String enumValue = value.toString();
          EnumDataSchema enumDataSchema = (EnumDataSchema) dereferencedDataSchema;
          if (enumDataSchema.getSymbols().contains(enumValue) == false)
          {
            appendMessage("enum value %1$s not one of %2$s", enumValue, enumDataSchema.getSymbols());
            result = BAD_RESULT;
            break;
          }
          result = _avroAdapter.createEnumSymbol(avroSchema, enumValue);
          break;
        case FIXED:
          byte[] bytes = translateBytes(value);
          FixedDataSchema fixedDataSchema = (FixedDataSchema) dereferencedDataSchema;
          if (fixedDataSchema.getSize() != bytes.length)
          {
            appendMessage("ByteString size %1$d != FixedDataSchema size %2$d",
                          bytes.length,
                          fixedDataSchema.getSize());
            result = null;
            break;
          }
          GenericData.Fixed fixed = new GenericData.Fixed(avroSchema);
          fixed.bytes(bytes);
          result = fixed;
          break;
        case MAP:
          DataMap map = (DataMap) value;
          DataSchema valueDataSchema = ((MapDataSchema) dereferencedDataSchema).getValues();
          Schema valueAvroSchema = avroSchema.getValueType();
          Map<String, Object> avroMap = new HashMap<String, Object>(map.size());
          for (Map.Entry<String, Object> entry : map.entrySet())
          {
            String key = entry.getKey();
            _path.addLast(key);
            Object entryAvroValue = translate(entry.getValue(), valueDataSchema, valueAvroSchema);
            _path.removeLast();
            avroMap.put(key, entryAvroValue);
          }
          result = avroMap;
          break;
        case ARRAY:
          DataList list = (DataList) value;
          DataSchema elementDataSchema = ((ArrayDataSchema) dereferencedDataSchema).getItems();
          Schema elementAvroSchema = avroSchema.getElementType();
          GenericData.Array<Object> avroList = new GenericData.Array<Object>(list.size(), avroSchema);
          for (int i = 0; i < list.size(); i++)
          {
            _path.addLast(i);
            Object entryAvroValue = translate(list.get(i), elementDataSchema, elementAvroSchema);
            _path.removeLast();
            avroList.add(entryAvroValue);
          }
          result = avroList;
          break;
        case RECORD:
          map = (DataMap) value;
          RecordDataSchema recordDataSchema = (RecordDataSchema) dereferencedDataSchema;
          GenericData.Record avroRecord = new GenericData.Record(avroSchema);
          for (RecordDataSchema.Field field : recordDataSchema.getFields())
          {
            String fieldName = field.getName();
            DataSchema fieldDataSchema = field.getType();
            Schema.Field avroField = avroSchema.getField(fieldName);
            if (avroField == null)
            {
              // field present in input but there is no field for it in Avro schema.
              // TODO: Whether and how to indicate this condition to clients.
              continue;
            }
            _path.addLast(fieldName);
            Schema fieldAvroSchema = avroField.schema();
            Object fieldValue = map.get(fieldName);
            boolean isOptional = field.getOptional();
            if (isOptional)
            {
              if (fieldDataSchema.getDereferencedType() != DataSchema.Type.UNION)
              {
                if (fieldValue == null)
                {
                  fieldValue = Data.NULL;
                  fieldDataSchema = DataSchemaConstants.NULL_DATA_SCHEMA;
                }
                Map.Entry<String, Schema> fieldAvroEntry = findUnionMember(fieldDataSchema, fieldAvroSchema);
                if (fieldAvroEntry == null)
                {
                  _path.removeLast();
                  continue;
                }
                fieldAvroSchema = fieldAvroEntry.getValue();
              }
              else
              {
                // already a union
                if (fieldValue == null)
                {
                  // field is not present
                  fieldValue = Data.NULL;
                  fieldDataSchema = DataSchemaConstants.NULL_DATA_SCHEMA;
                }
              }
            }
            else if (fieldValue == null)
            {
              Object defaultValue = field.getDefault();
              if (defaultValue != null)
              {
                fieldValue = defaultValue;
              }
              else
              {
                appendMessage("required field is absent");
                _path.removeLast();
                continue;
              }
            }
            Object fieldAvroValue = translate(fieldValue, fieldDataSchema, fieldAvroSchema);
            avroRecord.put(fieldName, fieldAvroValue);
            _path.removeLast();
          }
          result = avroRecord;
          break;
        case UNION:
          UnionDataSchema unionDataSchema = (UnionDataSchema) dereferencedDataSchema;

          String key;
          Object memberValue;
          if (value == Data.NULL)
          {
            key = DataSchemaConstants.NULL_TYPE;
            memberValue = Data.NULL;
          }
          else
          {
            map = (DataMap) value;
            Map.Entry<String, Object> entry = map.entrySet().iterator().next();
            key = entry.getKey();
            memberValue = entry.getValue();
          }

          if (unionDataSchema.areMembersAliased())
          {
            // Since Pegasus 'union with aliases' are represented as an Avro record, the translation
            // is handled separately.
            result = translatePegasusUnionWithAliasesToAvroRecord(key, memberValue, unionDataSchema, avroSchema);
          }
          else
          {
            DataSchema memberDataSchema = unionDataSchema.getTypeByMemberKey(key);
            Map.Entry<String, Schema> memberAvroEntry = findUnionMember(memberDataSchema, avroSchema);
            if (memberAvroEntry == null) {
              result = BAD_RESULT;
              break;
            }
            Schema memberAvroSchema = memberAvroEntry.getValue();
            _path.addLast(memberAvroEntry.getKey());
            Object memberAvroValue = translate(memberValue, memberDataSchema, memberAvroSchema);
            _path.removeLast();
            result = memberAvroValue;
          }
          break;
        default:
          appendMessage("schema type unknown %1$s", dereferencedDataSchema.getType());
          result = BAD_RESULT;
          break;
      }
      return result;
    }

    private Object translatePegasusUnionWithAliasesToAvroRecord(String memberKey, Object memberValue, UnionDataSchema unionDataSchema, Schema avroSchema)
    {
      Schema recordAvroSchema = extractNonnullSchema(avroSchema);

      GenericData.Record avroRecord = new GenericData.Record(recordAvroSchema);

      // Bail out if the pegasus union data has an invalid member key
      DataSchema memberDataSchema = unionDataSchema.getTypeByMemberKey(memberKey);
      if (memberDataSchema == null)
      {
        appendMessage("cannot find member key %1$s in union %2$s", memberKey, unionDataSchema);
        return BAD_RESULT;
      }

      // If the member value is null, don't try to map this to a field as the Avro record will not have
      // a field for a null union member
      if (memberValue != Data.NULL)
      {
        Schema.Field avroField = recordAvroSchema.getField(memberKey);
        if (avroField == null)
        {
          appendMessage("cannot find field %1$s in record %2$s", memberKey, recordAvroSchema);
          return BAD_RESULT;
        }
        _path.add(memberKey);

        Schema fieldAvroSchema = avroField.schema();
        avroRecord.put(memberKey, translate(memberValue, memberDataSchema, extractNonnullSchema(fieldAvroSchema)));
        _path.removeLast();
      }

      Schema.Field avroDiscriminatorField = recordAvroSchema.getField(DataSchemaConstants.DISCRIMINATOR_FIELD);
      if (avroDiscriminatorField == null)
      {
        appendMessage("cannot find field %1$s in record %2$s", DataSchemaConstants.DISCRIMINATOR_FIELD, recordAvroSchema);
        return BAD_RESULT;
      }

      _path.add(DataSchemaConstants.DISCRIMINATOR_FIELD);
      Object fieldDiscriminator = _avroAdapter.createEnumSymbol(avroDiscriminatorField.schema(), memberKey);
      avroRecord.put(DataSchemaConstants.DISCRIMINATOR_FIELD, fieldDiscriminator);
      _path.removeLast();

      return avroRecord;
    }
  }

  /**
   * Avro's optional fields are defined as an Union. This method can be used to extract the non-null type
   * embedded in the union. If the passed in avro schema is not a Union type, it is returned as is.
   *
   * @param avroSchema
   * @return
   */
  protected Schema extractNonnullSchema(Schema avroSchema)
  {
    // If the unionDataSchema is from an optional field, the avroSchema will be an union with two members -
    // the translated record and a null.
    if (avroSchema.getType() != Schema.Type.UNION)
    {
      return avroSchema;
    }
    else
    {
      List<Schema> memberTypes = avroSchema.getTypes();
      if (memberTypes.size() != 2)
      {
        appendMessage("found more than two types in an union with null for an optional field %1$s", avroSchema);
        return avroSchema;
      }

      for (Schema memberType : memberTypes)
      {
        if (memberType.getType() != Schema.Type.NULL)
        {
          return memberType;
        }
      }

      appendMessage("cannot find a non-null type in an union with null for an optional field %1$s", avroSchema);
      return null;
    }
  }

  protected Map.Entry<String, Schema> findUnionMember(DataSchema dataSchema, Schema avroSchema)
  {
    AvroOverride avroOverride = getAvroOverride(dataSchema);
    String key = (avroOverride == null ? dataSchema.getUnionMemberKey() : avroOverride.getAvroSchemaFullName());
    List<Schema> members = avroSchema.getTypes();
    for (Schema member : members)
    {
      String name;
      switch (member.getType())
      {
        case ENUM:
        case FIXED:
        case RECORD:
          name = getUnionMemberKey(member);
          break;
        default:
          name = member.getType().toString().toLowerCase();
      }
      if (name.equals(key))
        return new AbstractMap.SimpleEntry<String, Schema>(name, member);
    }
    appendMessage("cannot find %1$s in union %2$s", key, avroSchema);
    return null;
  }

  /**
   * This method helps to find the right union member key for Avro Schema,
   * when the data translation happens between schemas with overridden namespaces.
   * Users pass the Avro - Pegasus namespaceOverrideMapping in DataTranslationOptions,
   * from the map, data translator is able to match the overridden Avro namespace to the correlated Pegasus namespace.
   *
   * @param schema Avro Schema
   * @return the union member key string
   */
  protected String getUnionMemberKey(Schema schema)
  {
    if (_dataTranslationOptions != null && _dataTranslationOptions.getAvroToDataSchemaNamespaceMapping() != null)
    {
      Map<String, String> namespaceOverrideMapping = _dataTranslationOptions.getAvroToDataSchemaNamespaceMapping();
      if (namespaceOverrideMapping.containsKey(schema.getNamespace()))
      {
        return schema.getFullName().replaceFirst(schema.getNamespace(), namespaceOverrideMapping.get(schema.getNamespace()));
      }
    }
    return schema.getFullName();
  }

  private static byte[] translateBytes(Object value)
  {
    byte[] bytes = (value.getClass() == ByteString.class) ?
      ((ByteString) value).copyBytes() :
      Data.stringToBytes((String) value, true);
    return bytes;
  }

  private static String pathToString(Collection<?> path)
  {
    StringBuilder sb = new StringBuilder();
    for (Object o : path)
    {
      sb.append(DataElement.SEPARATOR);
      sb.append(o);
    }
    return sb.toString();
  }
}
