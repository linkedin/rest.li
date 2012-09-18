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
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.schema.DataSchema;
import java.io.IOException;
import java.util.Map;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.util.Utf8;

public class AnyRecordTranslator implements CustomDataTranslator
{
  private static final String TYPE = "type";
  private static final String VALUE = "value";

  private final JacksonDataCodec _codec = new JacksonDataCodec();

  @Override
  public Object avroGenericToData(DataTranslatorContext context, Object avroData, Schema avroSchema, DataSchema schema)
  {
    boolean error = false;
    Object result = null;
    GenericRecord genericRecord = null;
    try
    {
      genericRecord = (GenericRecord) avroData;
    }
    catch (ClassCastException e)
    {
      context.appendMessage("Error translating %1$s, it is not a GenericRecord", avroData);
      error = true;
    }
    if (error == false)
    {
      Utf8 type = null;
      Utf8 value = null;
      try
      {
        type = (Utf8) genericRecord.get(TYPE);
        value = (Utf8) genericRecord.get(VALUE);
      }
      catch (ClassCastException e)
      {
        context.appendMessage("Error translating %1$s, \"type\" or \"value\" is not a %2$s", avroData, Utf8.class.getSimpleName());
        error = true;
      }
      if (error == false)
      {
        if (type == null || value == null)
        {
          context.appendMessage("Error translating %1$s, \"type\" or \"value\" is null", avroData);
        }
        else
        {
          try
          {
            DataMap valueDataMap = _codec.bytesToMap(value.getBytes());
            DataMap anyDataMap = new DataMap(2);
            anyDataMap.put(type.toString(), valueDataMap);
            result = anyDataMap;
          }
          catch (IOException e)
          {
            context.appendMessage("Error translating %1$s, %2$s", avroData, e);
          }
        }
      }
    }
    return result;
  }

  @Override
  public Object dataToAvroGeneric(DataTranslatorContext context, Object data, DataSchema schema, Schema avroSchema)
  {
    Object result = null;
    DataMap dataMap;
    try
    {
      dataMap = (DataMap) data;
    }
    catch (ClassCastException e)
    {
      context.appendMessage("Error translating %1$s, it is not a DataMap", data);
      dataMap = null;
    }
    if (dataMap != null)
    {
      if (dataMap.size() != 1)
      {
        context.appendMessage("Error translating %1$s, DataMap has more than one element", data);
      }
      else
      {
        try
        {
          Map.Entry<String, Object> entry = dataMap.entrySet().iterator().next();
          String key = entry.getKey();
          Object value = entry.getValue();
          GenericRecord record = new GenericData.Record(avroSchema);
          record.put(TYPE, new Utf8(key));
          record.put(VALUE, new Utf8(_codec.mapToBytes((DataMap) value)));
          result = record;
        }
        catch (IOException e)
        {
          context.appendMessage("Error translating %1$s, %2$s", data, e);
        }
      }
    }
    return result;
  }
}
