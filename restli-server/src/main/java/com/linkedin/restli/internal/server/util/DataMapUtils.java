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

package com.linkedin.restli.internal.server.util;


import com.linkedin.data.Data;
import com.linkedin.data.DataComplex;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.codec.PsonDataCodec;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.template.DataTemplate;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.JacksonDataTemplateCodec;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.message.rest.RestMessage;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.server.RestLiInternalException;
import com.linkedin.restli.server.RoutingException;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;

public class DataMapUtils
{
  private static final JacksonDataCodec CODEC = new JacksonDataCodec();
  private static final PermissiveJacksonDataCodec PERMISSIVE_JACKSON_DATA_CODEC = new PermissiveJacksonDataCodec();
  private static final PsonDataCodec PSON_DATA_CODEC = new PsonDataCodec();
  private static final JacksonDataTemplateCodec TEMPLATE_CODEC = new JacksonDataTemplateCodec();
  private static final Logger LOG = LoggerFactory.getLogger(DataMapUtils.class);

  /**
   * Read {@link DataMap} from InputStream.
   *
   * @param stream input stream
   * @return {@link DataMap}
   */
  public static DataMap readMap(final InputStream stream)
  {
    try
    {
      return CODEC.readMap(stream);
    }
    catch (IOException e)
    {
      throw new RestLiInternalException(e);
    }
  }

  /**
   * Read {@link DataMap} from InputStream.
   *
   * @param stream input stream
   * @return {@link DataMap}
   */
  public static DataMap readMapPson(final InputStream stream)
  {
    try
    {
      return PSON_DATA_CODEC.readMap(stream);
    }
    catch (IOException e)
    {
      throw new RestLiInternalException(e);
    }
  }

  /**
   * Read {@link DataMap} from a {@link RestMessage}, using the message's headers to determine the
   * correct encoding type.
   *
   * @param message {@link RestMessage}
   * @return {@link DataMap}
   */
  public static DataMap readMap(final RestMessage message)
  {
    try
    {
      return readMapWithExceptions(message);
    }
    catch (IOException e)
    {
      throw new RestLiInternalException(e);
    }
  }

  /**
   * Similar to {@link #readMap(com.linkedin.r2.message.rest.RestMessage)}, but will throw an
   * {@link IOException} instead of a {@link RestLiInternalException}
   *
   * @throws IOException if the message entity cannot be parsed.
   */
  private static DataMap readMapWithExceptions(final RestMessage message) throws IOException
  {
    String header = message.getHeader(RestConstants.HEADER_CONTENT_TYPE);
    if (header == null)
    {
      return CODEC.readMap(message.getEntity().asInputStream());
    }

    ContentType contentType;
    try
    {
      contentType = new ContentType(header);
    }
    catch (ParseException e)
    {
      throw new RoutingException("Unable to parse Content-Type: " + header, HttpStatus.S_400_BAD_REQUEST.getCode(), e);
    }

    if (contentType.getBaseType().equalsIgnoreCase(RestConstants.HEADER_VALUE_APPLICATION_JSON))
    {
      return CODEC.readMap(message.getEntity().asInputStream());
    }
    else if (contentType.getBaseType().equalsIgnoreCase(RestConstants.HEADER_VALUE_APPLICATION_PSON))
    {
      return PSON_DATA_CODEC.readMap(message.getEntity().asInputStream());
    }
    else
    {
      throw new RoutingException("Unknown Content-Type: " + contentType.toString(), HttpStatus.S_415_UNSUPPORTED_MEDIA_TYPE.getCode());
    }
  }

  /**
   * Construct an object of a provided {@link RecordTemplate}-derived type from the
   * provided {@link DataMap}.
   *
   * @param data {@link DataMap} to construct the object from
   * @param recordClass class of the requested type
   * @param <T> requested object type
   * @return a new object of the requested type constructed with the provided DataMap
   */
  public static <T extends RecordTemplate> T convert(final DataMap data,
                                                     final Class<T> recordClass)
  {
    try
    {
      return recordClass.getConstructor(DataMap.class).newInstance(data);
    }
    catch (SecurityException e)
    {
      throw new RestLiInternalException(e);
    }
    catch (InstantiationException e)
    {
      throw new RestLiInternalException(e);
    }
    catch (IllegalAccessException e)
    {
      throw new RestLiInternalException(e);
    }
    catch (InvocationTargetException e)
    {
      throw new RestLiInternalException(e);
    }
    catch (NoSuchMethodException e)
    {
      throw new RestLiInternalException(e);
    }
  }

  /**
   * Effectively a combination of {@link #readMap(InputStream)} and
   * {@link #convert(DataMap, Class)}.
   *
   * @param stream input stream
   * @param recordClass class of the requested type
   * @param <T> requested object type
   * @return a new object of the requested type constructed with DataMap read from input
   *         stream
   * @throws IOException on error reading input stream
   */
  public static <T extends RecordTemplate> T read(final InputStream stream,
                                                  final Class<T> recordClass) throws IOException
  {
    try
    {
      DataMap dataMap = CODEC.readMap(stream);
      return DataTemplateUtil.wrap(dataMap, recordClass);
    }
    catch (IllegalArgumentException e)
    {
      throw new RestLiInternalException(e);
    }
    catch (SecurityException e)
    {
      throw new RestLiInternalException(e);
    }
  }

  /**
   * Effectively a combination of {@link #readMap(com.linkedin.r2.message.rest.RestMessage)} and
   * {@link #convert(DataMap, Class)}.
   *
   * @param message {@link RestMessage}
   * @param recordClass class of the requested type
   * @param <T> requested object type
   * @return a new object of the requested type constructed with DataMap read from message entity
   * @throws IOException on error reading input stream
   */
  public static <T extends RecordTemplate> T read(final RestMessage message,
                                                  final Class<T> recordClass)
    throws IOException
  {
    try
    {
      DataMap dataMap = readMapWithExceptions(message);
      return DataTemplateUtil.wrap(dataMap, recordClass);
    }
    catch (IllegalArgumentException e)
    {
      throw new RestLiInternalException(e);
    }
    catch (SecurityException e)
    {
      throw new RestLiInternalException(e);
    }
  }

  /**
   * A combination of {@link #readMap(java.io.InputStream)} and
   * {@link #convert(com.linkedin.data.DataMap, Class)} for collection responses.
   *
   * @param stream input stream
   * @param recordClass class of the requested type
   * @param <T> requested object type
   * @return a new object of the requested type constructed with DataMap read from input
   *         stream
   */
  public static <T extends RecordTemplate> CollectionResponse<T> readCollectionResponse(final InputStream stream,
                                                                                        final Class<T> recordClass)
  {
    try
    {
      DataMap dataMap = CODEC.readMap(stream);
      return new CollectionResponse<T>(dataMap, recordClass);
    }
    catch (IOException e)
    {
      throw new RestLiInternalException(e);
    }
  }

  /**
   * A combination of {@link #readMap(java.io.InputStream)} and
   * {@link #convert(com.linkedin.data.DataMap, Class)} for collection responses.
   *
   * @param message {@link RestMessage}
   * @param recordClass class of the requested type
   * @param <T> requested object type
   * @return a new object of the requested type constructed with DataMap read from message entity
   */
  public static <T extends RecordTemplate> CollectionResponse<T> readCollectionResponse(final RestMessage message,
                                                                                        final Class<T> recordClass)
  {
    DataMap dataMap = readMap(message);
    return new CollectionResponse<T>(dataMap, recordClass);
  }


  public static void write(final DataTemplate<?> record,
                           final OutputStream stream,
                           final boolean orderFields)
  {
    try
    {
      TEMPLATE_CODEC.writeDataTemplate(record, stream, orderFields);
    }
    catch (IOException e)
    {
      throw new RestLiInternalException(e);
    }
  }

  public static void write(final Object data,
                           final DataSchema schema,
                           final OutputStream stream,
                           final boolean orderFields)
  {
    try
    {
      TEMPLATE_CODEC.writeDataTemplate(data, schema, stream, orderFields);
    }
    catch (IOException e)
    {
      throw new RestLiInternalException(e);
    }
  }

  public static byte[] dataTemplateToBytes(final DataTemplate<?> record,
                                           final boolean orderFields)
  {
    try
    {
      return TEMPLATE_CODEC.dataTemplateToBytes(record, orderFields);
    }
    catch (IOException e)
    {
      throw new RestLiInternalException(e);
    }
  }

  /**
   * Encode {@link DataMap} as a byte array using {@link JacksonDataCodec}.
   *
   * @param dataMap input {@link DataMap}
   * @return byte array
   */
  public static byte[] mapToBytes(final DataMap dataMap)
  {
    return mapToBytes(dataMap, false);
  }

  public static byte[] mapToBytes(final DataMap dataMap, boolean permissive)
  {
    try
    {
      return CODEC.mapToBytes(dataMap);
    }
    catch (IOException e)
    {
      if (permissive)
      {
        LOG.info("Failed to serialize dataMap due to encoding error. Attempt to fix by replacing.");
        try
        {
          return PERMISSIVE_JACKSON_DATA_CODEC.mapToBytes(dataMap);
        }
        catch (IOException innerEx)
        {
          // our best-effort attempt to fix failed
          // do nothing; continue to throw the original exception
          LOG.info("Fixing encoding error failed. Please sanitize your input data.");
        }
      }
      throw new RestLiInternalException(e);
    }
  }

  public static byte[] listToBytes(final DataList dataList)
  {
    try
    {
      return CODEC.listToBytes(dataList);
    }
    catch (IOException e)
    {
      throw new RestLiInternalException(e);
    }
  }

  public static byte[] dataComplexToBytes(DataComplex value)
  {
    if (value instanceof DataMap)
    {
      return DataMapUtils.mapToBytes((DataMap) value);
    }
    else if (value instanceof DataList)
    {
      return DataMapUtils.listToBytes((DataList) value);
    }
    else
    {
      throw new IllegalStateException("Unknown DataComplex type: " + value.getClass());
    }
  }

  /**
   * Encode the {@link DataMap} as a byte array using {@link PsonDataCodec}.
   *
   * @param dataMap input {@link DataMap}
   * @return byte array
   */
  public static byte[] mapToPsonBytes(final DataMap dataMap)
  {
    try
    {
      return PSON_DATA_CODEC.mapToBytes(dataMap);
    }
    catch (IOException e)
    {
      throw new RestLiInternalException(e);
    }
  }

  /**
   * This codec extends JacksonDataCodec and does an extra passing for string values
   * to fix any malformed data or unmappable characters. It is relatively more costly
   * than the original JacksonDataCodec.
   *
   * @author Zhenkai Zhu
   */
  private static class PermissiveJacksonDataCodec extends JacksonDataCodec
  {
    @Override
    protected void writeObject(Object object, JsonGenerator generator) throws IOException
    {
      JsonTraverseCallback callback = new PermissiveJsonTraverseCallback(generator);
      Data.traverse(object, callback);
      generator.flush();
      generator.close();
    }

    private static class PermissiveJsonTraverseCallback extends JsonTraverseCallback
    {
      private static final Charset CHARSET = Charset.forName("UTF-8");
      private static final CharsetDecoder CHARSET_DECODER =
          CHARSET.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);

      protected PermissiveJsonTraverseCallback(JsonGenerator jsonGenerator)
      {
        super(jsonGenerator);
      }

      @Override
      public void stringValue(String value) throws JsonGenerationException, IOException
      {
        super.stringValue(fix(value));
      }

      /**
       * @param value A string that is malformed or has unmappable character
       * @return A string who's malformed part of unmappable character has been replaced.
       *         The default replacement is "\uFFFD".
       * @throws IOException
       */
      private String fix(String value) throws IOException
      {
        InputStream is = new ByteArrayInputStream(value.getBytes(CHARSET));
        BufferedReader br = new BufferedReader(new InputStreamReader(is, CHARSET_DECODER));
        StringBuilder sb = new StringBuilder();
        String line;
        while((line = br.readLine()) != null)
        {
          sb.append(line);
        }
        return sb.toString();
      }
    }
  }
}
