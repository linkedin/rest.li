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

package com.linkedin.data.codec;

import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.Instantiatable;

/**
 * A JSON codec that uses Jackson for serialization and de-serialization.
 *
 * @author slim
 */
public class JacksonDataCodec extends AbstractJacksonDataCodec implements TextDataCodec
{
  protected boolean _allowComments;
  protected PrettyPrinter _prettyPrinter;
  protected JsonEncoding _jsonEncoding = JsonEncoding.UTF8;

  public JacksonDataCodec()
  {
    this(new JsonFactory());
  }

  public JacksonDataCodec(JsonFactory jsonFactory)
  {
    super(jsonFactory);
    jsonFactory.disable(JsonFactory.Feature.INTERN_FIELD_NAMES);
    setAllowComments(true);
  }

  public void setAllowComments(boolean allowComments)
  {
    _factory.configure(JsonParser.Feature.ALLOW_COMMENTS, allowComments);
    _allowComments = allowComments;
  }

  /**
   * Gets an instance of {@link PrettyPrinter}. If the PrettyPrinter is stateless and doesn't implement {@link Instantiatable},
   * the same instance is returned every time. Otherwise, an instance created by {@link Instantiatable#createInstance()}
   * is returned.
   */
  @SuppressWarnings("unchecked")
  private PrettyPrinter getPrettyPrinter()
  {
    return _prettyPrinter instanceof Instantiatable
        ? ((Instantiatable<? extends PrettyPrinter>) _prettyPrinter).createInstance()
        : _prettyPrinter;
  }

  /**
   * Sets a PrettyPrinter. Note that a stateful PrettyPrinter should implement Instantiatable to allow a new instance
   * to be used for every JSON generation.
   */
  public void setPrettyPrinter(PrettyPrinter prettyPrinter)
  {
    _prettyPrinter = prettyPrinter;
  }

  @Override
  public String getStringEncoding()
  {
    return _jsonEncoding.getJavaName();
  }

  @Override
  public String mapToString(DataMap map) throws IOException
  {
    return objectToString(map);
  }

  @Override
  public String listToString(DataList list) throws IOException
  {
    return objectToString(list);
  }

  protected String objectToString(Object object) throws IOException
  {
    StringWriter out = new StringWriter(DEFAULT_BUFFER_SIZE);
    writeObject(object, createJsonGenerator(out));
    return out.toString();
  }

  protected JsonGenerator createJsonGenerator(OutputStream out) throws IOException
  {
    JsonGenerator generator = _factory.createGenerator(out);
    if (_prettyPrinter != null)
    {
      generator.setPrettyPrinter(getPrettyPrinter());
    }
    return generator;
  }

  protected JsonGenerator createJsonGenerator(Writer out) throws IOException
  {
    JsonGenerator generator = _factory.createGenerator(out);
    if (_prettyPrinter != null)
    {
      generator.setPrettyPrinter(getPrettyPrinter());
    }
    return generator;
  }

  @Override
  public DataMap stringToMap(String input) throws IOException
  {
    return parse(_factory.createParser(input), DataMap.class);
  }

  @Override
  public DataList stringToList(String input) throws IOException
  {
    return parse(_factory.createParser(input), DataList.class);
  }

  @Override
  public void writeMap(DataMap map, Writer out) throws IOException
  {
    writeObject(map, createJsonGenerator(out));
  }

  @Override
  public void writeList(DataList list, Writer out) throws IOException
  {
    writeObject(list, createJsonGenerator(out));
  }

  @Override
  public DataMap readMap(Reader in) throws IOException
  {
    return parse(_factory.createParser(in), DataMap.class);
  }

  @Override
  public DataList readList(Reader in) throws IOException
  {
    return parse(_factory.createParser(in), DataList.class);
  }

  /**
   * Reads an {@link Reader} and parses its contents into a list of Data objects.
   *
   * @param in provides the {@link Reader}
   * @param mesg provides the {@link StringBuilder} to store validation error messages,
   *             such as duplicate keys in the same {@link DataMap}.
   * @param locationMap provides where to store the mapping of a Data object
   *                    to its location in the in the {@link Reader}. may be
   *                    {@code null} if this mapping is not needed by the caller.
   *                    This map should usually be an {@link IdentityHashMap}.
   * @return the list of Data objects parsed from the {@link Reader}.
   * @throws IOException if there is a syntax error in the input.
   */
  public List<Object> parse(Reader in, StringBuilder mesg, Map<Object, DataLocation> locationMap)
      throws IOException
  {
    return parse(_factory.createParser(in), mesg, locationMap);
  }
}
