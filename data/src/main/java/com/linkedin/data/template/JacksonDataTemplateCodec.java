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


import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

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
import java.io.StringWriter;
import java.io.Writer;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;


public class JacksonDataTemplateCodec extends JacksonDataCodec
{
  public JacksonDataTemplateCodec()
  {
    super();
  }

  public JacksonDataTemplateCodec(JsonFactory jsonFactory)
  {
    super(jsonFactory);
  }

  /**
   * Serialize the provided {@link java.lang.Object} to JSON and, if order is set to true, sort and order the output
   * using {@link com.linkedin.data.template.JacksonDataTemplateCodec.SchemaOrderTraverseCallback} with the specified
   * {@link com.linkedin.data.schema.DataSchema}. The output is then written using the provided
   * {@link com.fasterxml.jackson.core.JsonGenerator}. The most typical use case of this method is to
   * feed a {@link com.linkedin.data.template.DataTemplate} into a {@link com.fasterxml.jackson.core.JsonGenerator}.
   *
   * <p><i>Note</i> that the provided {@link com.fasterxml.jackson.core.JsonGenerator} will NOT close its underlying output,
   * whether its a {@link java.io.Writer} or an {@link java.io.OutputStream}, after the completion of this
   * method.
   *
   * @param  data the data to serialize. Note that data here must be any of the acceptable Pegasus Data types. For example,
   *         {@link java.lang.Integer}, {@link java.lang.String}, {@link com.linkedin.data.DataList}, etc.
   * @param  schema the schema to use to sort and order the JSON output if order is set to true.
   * @param  generator the underlying JsonGenerator to call when the value is to be output.
   * @param  order whether or not to apply ordering to the serialized JSON based on the schema.
   * @throws IOException
   */
  protected void dataTemplateToJsonGenerator(Object data,
                                             DataSchema schema,
                                             JsonGenerator generator,
                                             boolean order) throws IOException
  {
    if (order)
    {
      JsonTraverseCallback callback = new SchemaOrderTraverseCallback(schema, generator);
      Data.traverse(data, callback);
    }
    else
    {
      objectToJsonGenerator(data, generator);
    }
  }

  /**
   * Serializes the provided {@link com.linkedin.data.template.DataTemplate} to JSON and writes it using the provided
   * {@link com.fasterxml.jackson.core.JsonGenerator}. If the order is set to to true, then the resulting serialization
   * of the DataTemplate will use {@link com.linkedin.data.template.JacksonDataTemplateCodec.SchemaOrderTraverseCallback}
   * to order the output.
   *
   * Refer to the documentation of {@link JacksonDataTemplateCodec#dataTemplateToJsonGenerator(java.lang.Object, com.linkedin.data.schema.DataSchema, com.fasterxml.jackson.core.JsonGenerator, boolean)
   *
   * @param  template the template to write.
   * @param  generator the underlying JsonGenerator to call when the value is to be output.
   * @param  order whether or not to apply ordering to the serialized JSON based on the schema.
   * @throws IOException
   */
  public void dataTemplateToJsonGenerator(DataTemplate<?> template,
                                          JsonGenerator generator,
                                          boolean order) throws IOException
  {
    dataTemplateToJsonGenerator(template.data(), template.schema(), generator, order);
  }

  /**
   * Serialize the provided {@link java.lang.Object} to JSON and, if order is set to true, sort and order the output
   * using {@link com.linkedin.data.template.JacksonDataTemplateCodec.SchemaOrderTraverseCallback} with the specified
   * {@link com.linkedin.data.schema.DataSchema}. The output is then written using the provided
   * {@link com.fasterxml.jackson.core.JsonGenerator}. The most typical use case of this method is to
   * write a {@link com.linkedin.data.template.DataTemplate}.
   *
   * <p><i>Note</i> that the provided {@link com.fasterxml.jackson.core.JsonGenerator} will have its underlying output,
   * whether its a {@link java.io.Writer} or an {@link java.io.OutputStream}, closed after the completion of this
   * method.
   *
   * @param  data the data to serialize. Note that data here must be any of the acceptable Pegasus Data types. For example,
   *         {@link java.lang.Integer}, {@link java.lang.String}, {@link com.linkedin.data.DataList}, etc.
   * @param  schema the schema to use to sort and order the JSON output if order is set to true.
   * @param  generator the underlying JsonGenerator to call when the value is to be output.
   * @param  order whether or not to apply ordering to the serialized JSON based on the schema.
   * @throws IOException
   */
  protected void writeDataTemplate(Object data,
                                   DataSchema schema,
                                   JsonGenerator generator,
                                   boolean order) throws IOException
  {
    if (order)
    {
      JsonTraverseCallback callback = new SchemaOrderTraverseCallback(schema, generator);
      Data.traverse(data, callback);
      generator.flush();
      generator.close();
    }
    else
    {
      writeObject(data, generator);
    }
  }

  /**
   * Serializes the provided {@link com.linkedin.data.template.DataTemplate} to JSON and writes it to the provided
   * {@link java.io.OutputStream}. If the order is set to to true, then the resulting serialization of the DataTemplate
   * will use {@link com.linkedin.data.template.JacksonDataTemplateCodec.SchemaOrderTraverseCallback} to order the output.
   *
   * Refer to the documentation of {@link JacksonDataTemplateCodec#writeDataTemplate(java.lang.Object, com.linkedin.data.schema.DataSchema, com.fasterxml.jackson.core.JsonGenerator, boolean)
   *
   * @param  template the template to write.
   * @param  out the OutputStream to write the serialized JSON to.
   * @param  order whether or not to apply ordering to the serialized JSON based on the schema.
   * @throws IOException
   */
  public void writeDataTemplate(DataTemplate<?> template, OutputStream out, boolean order) throws IOException
  {
    writeDataTemplate(template.data(), template.schema(), out, order);
  }

  /**
   * Serializes the provided {@link com.linkedin.data.template.DataTemplate} to JSON and writes it to the provided
   * {@link java.io.Writer}. If the order is set to to true, then the resulting serialization of the DataTemplate
   * will use {@link com.linkedin.data.template.JacksonDataTemplateCodec.SchemaOrderTraverseCallback} to order the output.
   *
   * Refer to the documentation of {@link JacksonDataTemplateCodec#writeDataTemplate(java.lang.Object, com.linkedin.data.schema.DataSchema, com.fasterxml.jackson.core.JsonGenerator, boolean)
   *
   * @param  template the template to write.
   * @param  out the Writer to write the serialized JSON to.
   * @param  order whether or not to apply ordering to the serialized JSON based on the schema.
   * @throws IOException
   */
  public void writeDataTemplate(DataTemplate<?> template, Writer out, boolean order) throws IOException
  {
    writeDataTemplate(template.data(), template.schema(), out, order);
  }

  /**
   * Serialize the provided {@link java.lang.Object} to JSON and, if order is set to true, sort and order the output
   * using {@link com.linkedin.data.template.JacksonDataTemplateCodec.SchemaOrderTraverseCallback} with the specified
   * {@link com.linkedin.data.schema.DataSchema}. The output is then written to the provided {@link java.io.OutputStream}.
   * The most typical use case of this method is to write a {@link com.linkedin.data.template.DataTemplate}.
   *
   * Refer to the documentation of {@link JacksonDataTemplateCodec#writeDataTemplate(java.lang.Object, com.linkedin.data.schema.DataSchema, com.fasterxml.jackson.core.JsonGenerator, boolean)
   *
   * @param  data the data to serialize. Note that data here must be any of the acceptable Pegasus Data types. For example,
   *         {@link java.lang.Integer}, {@link java.lang.String}, {@link com.linkedin.data.DataList}, etc.
   * @param  schema the schema to use to sort and order the JSON output if order is set to true.
   * @param  out the OutputStream to write the serialized JSON to.
   * @param  order whether or not to apply ordering to the serialized JSON based on the schema.
   * @throws IOException
   */
  public void writeDataTemplate(Object data,
                                DataSchema schema,
                                OutputStream out,
                                boolean order) throws IOException
  {
    JsonGenerator generator = createJsonGenerator(out);
    writeDataTemplate(data, schema, generator, order);
  }

  /**
   * Serialize the provided {@link java.lang.Object} to JSON and, if order is set to true, sort and order the output
   * using {@link com.linkedin.data.template.JacksonDataTemplateCodec.SchemaOrderTraverseCallback} with the specified
   * {@link com.linkedin.data.schema.DataSchema}. The output is then written to the provided {@link java.io.Writer}.
   * The most typical use case of this method is to write a {@link com.linkedin.data.template.DataTemplate}.
   *
   * Refer to the documentation of {@link JacksonDataTemplateCodec#writeDataTemplate(java.lang.Object, com.linkedin.data.schema.DataSchema, com.fasterxml.jackson.core.JsonGenerator, boolean)
   *
   * @param  data the data to serialize. Note that data here must be any of the acceptable Pegasus Data types. For example,
   *         {@link java.lang.Integer}, {@link java.lang.String}, {@link com.linkedin.data.DataList}, etc.
   * @param  schema the schema to use to sort and order the JSON output if order is set to true.
   * @param  out the Writer to write the serialized JSON output to.
   * @param  order whether or not to apply ordering to the serialized JSON based on the schema.
   * @throws IOException
   */
  public void writeDataTemplate(Object data,
                                DataSchema schema,
                                Writer out,
                                boolean order) throws IOException
  {
    JsonGenerator generator = createJsonGenerator(out);
    writeDataTemplate(data, schema, generator, order);
  }

  /**
   * Serializes the provided {@link com.linkedin.data.template.DataTemplate} to JSON and writes it to the provided
   * {@link java.io.OutputStream}. The serialized JSON is not ordered.
   *
   * Refer to the documentation of {@link JacksonDataTemplateCodec#writeDataTemplate(java.lang.Object, com.linkedin.data.schema.DataSchema, com.fasterxml.jackson.core.JsonGenerator, boolean)
   *
   * @param  template the template to write.
   * @param  out the OutputStream to write the serialized JSON to.
   * @throws IOException
   */
  public void writeDataTemplate(DataTemplate<?> template, OutputStream out) throws IOException
  {
    writeObject(template.data(), createJsonGenerator(out));
  }

  /**
   * Serializes the provided {@link com.linkedin.data.template.DataTemplate} to JSON and writes it to the provided
   * {@link java.io.Writer}. The serialized JSON is not ordered.
   *
   * Refer to the documentation of {@link JacksonDataTemplateCodec#writeDataTemplate(java.lang.Object, com.linkedin.data.schema.DataSchema, com.fasterxml.jackson.core.JsonGenerator, boolean)
   *
   * @param  template the template to write.
   * @param  out the Writer to write the serialized JSON to.
   * @throws IOException
   */
  public void writeDataTemplate(DataTemplate<?> template, Writer out) throws IOException
  {
    writeObject(template.data(), createJsonGenerator(out));
  }

  /**
   * Serializes the provided {@link com.linkedin.data.template.DataTemplate} to JSON and into to a byte array. If
   * the order is set to to true, then the resulting serialization of the DataTemplate will use
   * {@link com.linkedin.data.template.JacksonDataTemplateCodec.SchemaOrderTraverseCallback} to order the output.
   *
   * @param  template the template to serialize.
   * @param  order whether or not to apply ordering to the serialized data template based on the schema.
   * @return the serialized byte array.
   * @throws IOException
   */
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

  /**
   * Serializes the provided {@link com.linkedin.data.template.DataTemplate} to a {@link java.lang.String}. If the
   * order is set to to true, then the resulting serialization of the DataTemplate will use
   * {@link com.linkedin.data.template.JacksonDataTemplateCodec.SchemaOrderTraverseCallback} to order the output.
   *
   * @param  template the template to serialize.
   * @param  order whether or not to apply ordering to the serialized data template based on the schema.
   * @return the serialized String.
   * @throws IOException
   */
  public String dataTemplateToString(DataTemplate<?> template, boolean order) throws IOException
  {
    if (order)
    {
      StringWriter out = new StringWriter(_defaultBufferSize);
      writeDataTemplate(template, out, order);
      return out.toString();
    }
    else
    {
      return dataTemplateToString(template);
    }
  }

  /**
   * Serializes the provided {@link com.linkedin.data.template.DataTemplate} to a byte array. The serialized JSON
   * is not ordered.
   *
   * @param  template the template to serialize.
   * @return the serialized byte array.
   * @throws IOException
   */
  public byte[] dataTemplateToBytes(DataTemplate<?> template) throws IOException
  {
    return objectToBytes(template.data());
  }

  /**
   * Serializes the provided {@link com.linkedin.data.template.DataTemplate} to a {@link java.lang.String}.
   * The serialized JSON is not ordered.
   *
   * @param  template the template to serialize.
   * @return the serialized String.
   * @throws IOException
   */
  public String dataTemplateToString(DataTemplate<?> template) throws IOException
  {
    return objectToString(template.data());
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
