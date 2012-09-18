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

package com.linkedin.data.template;


import com.linkedin.data.ByteString;
import com.linkedin.data.Data;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.codec.DataEncodingException;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.UnionDataSchema;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.codehaus.jackson.JsonGenerator;

public class JacksonDataTemplateCodec extends JacksonDataCodec
{
  public void writeDataTemplate(DataTemplate<?> template, OutputStream out, boolean order) throws IOException
  {
   writeDataTemplate(template.data(), template.schema(), out, order);
  }

  public void writeDataTemplate(Object data,
                                DataSchema schema,
                                OutputStream out,
                                boolean order) throws IOException
  {
    if (order)
    {
      JsonGenerator generator = createJsonGenerator(out);
      JsonTraverseCallback callback = new SchemaOrderTraverseCallback(schema, generator);
      Data.traverse(data, callback);
      generator.flush();
      generator.close();
    }
    else
    {
      writeObject(data, out);
    }
  }

  public void writeDataTemplate(DataTemplate<?> template, OutputStream out) throws IOException
  {
    writeObject(template.data(), out);
  }

  public byte[] dataTemplateToBytes(DataTemplate<?> template, boolean order) throws IOException
  {
    if (order)
    {
      ByteArrayOutputStream out = new ByteArrayOutputStream(_defaultBufferSize);
      writeDataTemplate(template, out, order);
      return out.toByteArray();
    }
    else
    {
      return dataTemplateToBytes(template);
    }
  }

  public byte[] dataTemplateToBytes(DataTemplate<?> template) throws IOException
  {
    return objectToBytes(template.data());
  }

  /**
   * A {@link com.linkedin.data.Data.TraverseCallback} that output record fields in the
   * order the fields are defined by the {@link RecordDataSchema} and
   * output each map sorted by the map's keys.
   */
  public static class SchemaOrderTraverseCallback extends JsonTraverseCallback
  {
    /**
     * Constructor.
     *
     * @param schema provides the {@link DataSchema} of the root object.
     * @param jsonGenerator provides the {@link JsonGenerator} that generates the JSON output.
     */
    SchemaOrderTraverseCallback(DataSchema schema, JsonGenerator jsonGenerator)
    {
      super(jsonGenerator);
      _pendingSchema = schema;
    }

    @Override
    public Iterable<Map.Entry<String, Object>> orderMap(DataMap map)
    {
      if (_currentSchema != null && _currentSchema.getType() == DataSchema.Type.RECORD)
      {
        return orderMapEntries((RecordDataSchema) _currentSchema, map);
      }

      return Data.orderMapEntries(map);
    }

    @Override
    public void nullValue()
      throws IOException
    {
      simpleValue();
      super.nullValue();
    }

    @Override
    public void booleanValue(boolean value)
      throws IOException
    {
      simpleValue();
      super.booleanValue(value);
    }

    @Override
    public void integerValue(int value)
      throws IOException
    {
      simpleValue();
      super.integerValue(value);
    }

    @Override
    public void longValue(long value)
      throws IOException
    {
      simpleValue();
      super.longValue(value);
    }

    @Override
    public void floatValue(float value)
      throws IOException
    {
      simpleValue();
      super.floatValue(value);
    }

    @Override
    public void doubleValue(double value)
      throws IOException
    {
      simpleValue();
      super.doubleValue(value);
    }

    @Override
    public void stringValue(String value)
      throws IOException
    {
      simpleValue();
      super.stringValue(value);
    }

    @Override
    public void byteStringValue(ByteString value)
      throws IOException
    {
      simpleValue();
      super.byteStringValue(value);
    }

    @Override
    public void illegalValue(Object value)
      throws DataEncodingException
    {
      super.illegalValue(value);
    }

    @Override
    public void emptyMap()
      throws IOException
    {
      simpleValue();
      super.emptyMap();
    }

    @Override
    public void startMap(DataMap map)
      throws IOException
    {
      push();
      super.startMap(map);
    }

    @Override
    public void key(String key)
      throws IOException
    {
      DataSchema newSchema = null;
      if (_currentSchema != null)
      {
        switch (_currentSchema.getType())
        {
          case RECORD:
            RecordDataSchema recordSchema = (RecordDataSchema) _currentSchema;
            RecordDataSchema.Field field = recordSchema.getField(key);
            if (field != null)
            {
              newSchema = field.getType();
            }
            break;
          case UNION:
            UnionDataSchema unionSchema = (UnionDataSchema) _currentSchema;
            newSchema = unionSchema.getType(key);
            break;
          case MAP:
            MapDataSchema mapSchema = (MapDataSchema) _currentSchema;
            newSchema = mapSchema.getValues();
            break;
        }
      }
      _pendingSchema = newSchema;
      super.key(key);
    }

    @Override
    public void endMap()
      throws IOException
    {
      super.endMap();
      pop();
    }

    @Override
    public void emptyList()
      throws IOException
    {
      simpleValue();
      super.emptyList();
    }

    @Override
    public void startList(DataList list)
      throws IOException
    {
      push();
      super.startList(list);
    }

    @Override
    public void index(int index)
    {
      DataSchema newSchema = null;
      if (_currentSchema != null && _currentSchema.getType() == DataSchema.Type.ARRAY)
      {
        ArrayDataSchema arraySchema = (ArrayDataSchema) _currentSchema;
        newSchema = arraySchema.getItems();
      }
      _pendingSchema = newSchema;
      super.index(index);
    }

    @Override
    public void endList()
      throws IOException
    {
      super.endList();
      pop();
    }

    private static List<Map.Entry<String,Object>> orderMapEntries(RecordDataSchema schema, DataMap map)
    {
      List<Map.Entry<String,Object>> output = new ArrayList<Map.Entry<String,Object>>(map.size());
      List<RecordDataSchema.Field> fields = schema.getFields();
      // collect fields in the record schema in the order the fields are declared
      for (RecordDataSchema.Field field : fields)
      {
        String fieldName = field.getName();
        Object found = map.get(fieldName);
        if (found != null)
        {
          output.add(new AbstractMap.SimpleImmutableEntry<String,Object>(fieldName, found));
        }
      }
      // collect fields that are in the DataMap that is not in the record schema.
      List<Map.Entry<String,Object>> uncollected = new ArrayList<Map.Entry<String,Object>>(map.size() - output.size());
      for (Map.Entry<String,Object> e : map.entrySet())
      {
        if (schema.contains(e.getKey()) == false)
        {
          uncollected.add(e);
        }
      }
      Collections.sort(uncollected,
                       new Comparator<Map.Entry<String, Object>>()
                       {
                         @Override
                         public int compare(Map.Entry<String, Object> o1, Map.Entry<String, Object> o2)
                         {
                           return o1.getKey().compareTo(o2.getKey());
                         }
                       });
      output.addAll(uncollected);
      return output;
    }

    private void simpleValue()
    {
      _pendingSchema = null;
    }

    private void push()
    {
      _schemaStack.add(_currentSchema);
      _currentSchema = _pendingSchema;
    }

    private void pop()
    {
      _currentSchema = _schemaStack.remove(_schemaStack.size() - 1);
      _pendingSchema = null;
    }

    private DataSchema _currentSchema;
    private DataSchema _pendingSchema;
    private final List<DataSchema> _schemaStack = new ArrayList<DataSchema>();  // use ArrayList because elements may be null
  }
}