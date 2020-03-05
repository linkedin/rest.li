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


import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.template.DataTemplate;
import com.linkedin.data.template.JacksonDataTemplateCodec;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.PrettyPrinter;
import java.util.stream.Collectors;


/**
 * A {@link JsonBuilder} is used to build JSON output.
 * <p>
 *
 * It provides methods to write JSON content,
 * common methods for writing common types of fields,
 * tracking the current namespace, and
 * the names have already been dumped.
 * <p>
 *
 * @author slim
 */
public class JsonBuilder
{
  /**
   * Pretty printing format.
   *
   * @author slim
   */
  public static enum Pretty
  {
    /**
     * As compact as possible.
     */
    COMPACT,
    /**
     * With a space between fields, values, fields, items, ... etc.
     */
    SPACES,
    /**
     * Indented by 2 spaces for nested fields, items.
     */
    INDENTED
  }

  /**
   * Constructor.
   *
   * @param pretty is the pretty printing format.
   * @throws IOException if there is an error during construction.
   */
  public JsonBuilder(Pretty pretty) throws IOException
  {
    _writer = new StringWriter();
    _jsonGenerator = _jsonFactory.createGenerator(_writer);
    switch (pretty)
    {
      case SPACES:
        _jsonGenerator.setPrettyPrinter(_spacesPrettyPrinter);
        break;
      case INDENTED:
        _jsonGenerator.useDefaultPrettyPrinter();
        break;
      case COMPACT:
        break;
    }
  }

  /**
   * Get the resulting JSON output.
   * @return the resulting JSON output.
   * @throws IOException if there is an error generating the output.
   */
  public String result() throws IOException
  {
    _jsonGenerator.flush();
    return _writer.toString();
  }

  /**
   * Close underlying objects and release their resources.
   *
   * @throws IOException
   */
  public void close() throws IOException
  {
    _jsonGenerator.close();
    _writer.close();
  }

  /**
   * Close underlying objects and release their resources.
   * This method will log the {@link java.io.IOException} instead of throwing it.
   */
  public void closeQuietly()
  {
    try
    {
      close();
    }
    catch (IOException e)
    {
      // TODO: use Java 7 try-with-resources statement and Throwable.getSuppressed()
    }
  }

  public void writeBoolean(boolean value) throws IOException
  {
    _jsonGenerator.writeBoolean(value);
  }
  public void writeString(String value) throws IOException
  {
    _jsonGenerator.writeString(value);
  }
  public void writeStartObject() throws IOException
  {
    _jsonGenerator.writeStartObject();
  }
  public void writeEndObject() throws IOException
  {
    _jsonGenerator.writeEndObject();
  }
  public void writeFieldName(String fieldName) throws IOException
  {
    _jsonGenerator.writeFieldName(fieldName);
  }
  public void writeStartArray() throws IOException
  {
    _jsonGenerator.writeStartArray();
  }
  public void writeEndArray() throws IOException
  {
    _jsonGenerator.writeEndArray();
  }

  /**
   * Write an array of strings.
   *
   * @param value provides the strings to write.
   */
  public void writeStringArray(List<String> value) throws IOException
  {
    writeStartArray();
    for (String s : value)
    {
      writeString(s);
    }
    writeEndArray();
  }

  /**
   * Write a map with string keys and values.
   *
   * @param value provides the map to write.
   */
  public void writeMap(Map<String, ?> value) throws IOException
  {
    writeStartObject();
    writeProperties(value);
    writeEndObject();
  }

  /**
   * Write a field whose value is a boolean.
   *
   * @param fieldName is the name of the field.
   * @param value of the field.
   * @throws IOException if there is an error writing.
   */
  public void writeBooleanField(String fieldName, boolean value) throws IOException
  {
    _jsonGenerator.writeBooleanField(fieldName, value);
  }
  /**
   * Write a field whose value is an integer.
   *
   * @param fieldName is the name of the field.
   * @param value of the field.
   * @throws IOException if there is an error writing.
   */
  public void writeIntField(String fieldName, int value) throws IOException
  {
    _jsonGenerator.writeNumberField(fieldName, value);
  }

  /**
   * Write a field whose value is a string.
   *
   * The field will be written if required is true or the string value is non-empty.
   *
   * @param fieldName is the name of the field.
   * @param value of the field.
   * @param required indicates whether this field will always be written
   * @throws IOException if there is an error writing.
   */
  public void writeStringField(String fieldName, String value, boolean required) throws IOException
  {
    if (required || value.isEmpty() == false)
    {
      _jsonGenerator.writeStringField(fieldName, value);
    }
  }

  /**
   * Write a field whose value is a string array.
   *
   * The field will be written if required is true or the string array value is non-empty.
   *
   * @param fieldName is the name of the field.
   * @param value of the field.
   * @param required indicates whether this field will always be written
   * @throws IOException if there is an error writing.
   */
  public void writeStringArrayField(String fieldName, List<String> value, boolean required) throws IOException
  {
    if (required || value.isEmpty() == false)
    {
      writeFieldName(fieldName);
      writeStringArray(value);
    }
  }

  /**
   * Write a field whose value is a map with string keys and values.
   *
   * The field will be written if required is true or the map value is non-empty.
   *
   * @param fieldName is the name of the field.
   * @param value of the field.
   * @param required indicates whether this field will always be written
   * @throws IOException if there is an error writing.
   */
  public void writeMapField(String fieldName, Map<String, ?> value, boolean required) throws IOException
  {
    if (required || value.isEmpty() == false)
    {
      writeFieldName(fieldName);
      writeMap(value);
    }
  }

  /**
   * Write Data object.
   *
   * @param object is the Data object to write.
   */
  public void writeData(Object object) throws IOException
  {
    _jacksonDataCodec.objectToJsonGenerator(object, _jsonGenerator);
  }

  /**
   * Write properties by adding each property as a field to current JSON object.
   *
   * @param value provides the properties to be written.
   * @throws IOException if there is an error writing.
   */
  public void writeProperties(Map<String, ?> value) throws IOException
  {
    if (value.isEmpty() == false)
    {
      List<Map.Entry<String, ?>> orderedProperties =  value.entrySet().stream()
                                                                     .sorted(Map.Entry.comparingByKey())
                                                                     .collect(Collectors.toList());
        for (Map.Entry<String, ?> entry : orderedProperties)
      {
        _jsonGenerator.writeFieldName(entry.getKey());
        writeData(entry.getValue());
      }
    }
  }

  public void writeDataTemplate(DataTemplate<?> template, Boolean order) throws IOException
  {
    _jacksonDataTemplateCodec.dataTemplateToJsonGenerator(template, _jsonGenerator, order);
  }

  private final StringWriter _writer;
  private final JsonGenerator _jsonGenerator;
  private final JacksonDataCodec _jacksonDataCodec = new JacksonDataCodec();
  private final JacksonDataTemplateCodec _jacksonDataTemplateCodec = new JacksonDataTemplateCodec();

  private static final JsonFactory _jsonFactory = new JsonFactory().disable(JsonFactory.Feature.INTERN_FIELD_NAMES);
  private static final PrettyPrinter _spacesPrettyPrinter = new SpacesPrettyPrinter();

  private static class SpacesPrettyPrinter implements PrettyPrinter
  {
    @Override
    public void beforeArrayValues(JsonGenerator generator) throws IOException
    {
    }

    @Override
    public void beforeObjectEntries(JsonGenerator generator) throws IOException
    {
    }

    @Override
    public void writeArrayValueSeparator(JsonGenerator generator) throws IOException
    {
      generator.writeRaw(", ");
    }

    @Override
    public void writeEndArray(JsonGenerator generator, int arg1) throws IOException
    {
      generator.writeRaw(" ]");
    }

    @Override
    public void writeEndObject(JsonGenerator generator, int arg1) throws IOException
    {
      generator.writeRaw(" }");
    }

    @Override
    public void writeObjectEntrySeparator(JsonGenerator generator) throws IOException
    {
      generator.writeRaw(", ");
    }

    @Override
    public void writeObjectFieldValueSeparator(JsonGenerator generator) throws IOException
    {
      generator.writeRaw(" : ");
    }

    @Override
    public void writeRootValueSeparator(JsonGenerator generator) throws IOException
    {
    }

    @Override
    public void writeStartArray(JsonGenerator generator) throws IOException
    {
      generator.writeRaw("[ ");
    }

    @Override
    public void writeStartObject(JsonGenerator generator) throws IOException
    {
      generator.writeRaw("{ ");
    }
  }
}
