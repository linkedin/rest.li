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

import com.linkedin.data.ByteString;
import com.linkedin.data.Data;
import com.linkedin.data.DataComplex;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.codec.DataCodec;
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
import com.linkedin.restli.internal.common.DataMapConverter;
import com.linkedin.restli.internal.server.RestLiInternalException;
import com.linkedin.restli.server.RoutingException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.activation.MimeTypeParseException;

public class DataMapUtils
{
  private static final JacksonDataCodec CODEC = new JacksonDataCodec();
  private static final PsonDataCodec PSON_DATA_CODEC = new PsonDataCodec();
  private static final JacksonDataTemplateCodec TEMPLATE_CODEC = new JacksonDataTemplateCodec();

  /**
   * Read JSON encoded {@link DataMap} from InputStream.
   *
   * @param stream input stream
   * @return {@link DataMap}
   *
   * @deprecated due to assuming JSON encoding. Use {@link #readMap(InputStream, Map)} instead.
   */
  @Deprecated
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
   * @param headers Request or response headers
   * @return {@link DataMap}
   */
  public static DataMap readMap(final InputStream stream, final Map<String, String> headers)
  {
    try
    {
      return DataMapConverter.getContentType(headers).getCodec().readMap(stream);
    }
    catch (IOException | MimeTypeParseException e)
    {
      throw new RestLiInternalException(e);
    }
  }

  /**
   * Read PSON encoded {@link DataMap} from InputStream.
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
   * Read {@link DataMap} from a {@link com.linkedin.r2.message.rest.RestMessage}, using the message's headers to determine the
   * correct encoding type.
   *
   * @param message {@link com.linkedin.r2.message.rest.RestMessage}
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
  public static DataMap readMapWithExceptions(final RestMessage message) throws IOException
  {
    try
    {
      return DataMapConverter.bytesToDataMap(message.getHeaders(), message.getEntity());
    }
    catch (MimeTypeParseException e)
    {
      throw new RoutingException(e.getMessage(), HttpStatus.S_400_BAD_REQUEST.getCode(), e);
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
   * @param stream JSON encoded input stream
   * @param recordClass class of the requested type
   * @param <T> requested object type
   * @return a new object of the requested type constructed with DataMap read from input
   *         stream
   * @throws IOException on error reading input stream
   *
   * @deprecated due to assuming JSON encoding. Use {@link #read(InputStream, Class, Map)} instead.
   */
  @Deprecated
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
   * Effectively a combination of {@link #readMap(InputStream, Map)} and
   * {@link #convert(DataMap, Class)}.
   *
   * @param stream Encoded input stream
   * @param recordClass class of the requested type
   * @param headers Request or response headers
   * @param <T> requested object type
   * @return a new object of the requested type constructed with DataMap read from input
   *         stream
   * @throws IOException on error reading input stream
   */
  public static <T extends RecordTemplate> T read(final InputStream stream,
      final Class<T> recordClass, Map<String, String> headers) throws IOException
  {
    try
    {
      DataMap dataMap = DataMapConverter.getContentType(headers).getCodec().readMap(stream);
      return DataTemplateUtil.wrap(dataMap, recordClass);
    }
    catch (IllegalArgumentException | MimeTypeParseException | SecurityException e)
    {
      throw new RestLiInternalException(e);
    }
  }

  /**
   * Effectively a combination of {@link #readMap(com.linkedin.r2.message.rest.RestMessage)} and
   * {@link #convert(DataMap, Class)}.
   *
   * @param message {@link com.linkedin.r2.message.MessageHeaders}
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
   * {@link #convert(com.linkedin.data.DataMap, Class)} for JSON encoded collection responses.
   *
   * @param stream JSON encoded input stream
   * @param recordClass class of the requested type
   * @param <T> requested object type
   * @return a new object of the requested type constructed with DataMap read from input
   *         stream
   *
   * @deprecated due to assuming JSON encoding. Use {@link #readCollectionResponse(RestMessage,Class)} instead.
   */
  @Deprecated
  public static <T extends RecordTemplate> CollectionResponse<T> readCollectionResponse(final InputStream stream,
                                                                                        final Class<T> recordClass)
  {
    try
    {
      DataMap dataMap = CODEC.readMap(stream);
      return new CollectionResponse<>(dataMap, recordClass);
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
   * @param message {@link com.linkedin.r2.message.MessageHeaders}
   * @param recordClass class of the requested type
   * @param <T> requested object type
   * @return a new object of the requested type constructed with DataMap read from message entity
   */
  public static <T extends RecordTemplate> CollectionResponse<T> readCollectionResponse(final RestMessage message,
                                                                                        final Class<T> recordClass)
  {
    DataMap dataMap = readMap(message);
    return new CollectionResponse<>(dataMap, recordClass);
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

  /**
   * Serialize the write the dataMap to the outputstream.
   *
   * <p>The encoding is determined on the basis of the {@link RestConstants#HEADER_CONTENT_TYPE} header.</p>
   *
   * @param data The {@link DataMap} to serialize
   * @param stream The {@link OutputStream} to serialize to
   * @param headers Request or response headers.
   */
  public static void write(final DataMap data,
                           final OutputStream stream,
                           final Map<String, String> headers)
  {
    try
    {
      DataMapConverter.getContentType(headers).getCodec().writeMap(data, stream);
    }
    catch (IOException | MimeTypeParseException e)
    {
      throw new RestLiInternalException(e);
    }
  }

  @Deprecated
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
   *
   * @deprecated use {@link #mapToBytes(DataMap, Map)} instead.
   */
  @Deprecated
  public static byte[] mapToBytes(final DataMap dataMap)
  {
    try
    {
      return CODEC.mapToBytes(dataMap);
    }
    catch (IOException e)
    {
      throw new RestLiInternalException(e);
    }
  }

  /**
   * Encode {@link DataMap} as a byte array.
   *
   * @param dataMap input {@link DataMap}
   * @param headers Request or response headers. This is used to determine the codec to use to encode.
   * @return byte array
   */
  public static byte[] mapToBytes(final DataMap dataMap, final Map<String, String> headers)
  {
    try
    {
      return mapToBytes(dataMap, DataMapConverter.getContentType(headers).getCodec());
    }
    catch (MimeTypeParseException e)
    {
      throw new RestLiInternalException(e);
    }
  }

  /**
   * Encode {@link DataMap} as a JSON ByteString.
   *
   * @param dataMap input {@link DataMap}
   * @return ByteString
   *
   * @deprecated use {@link #mapToByteString(DataMap, Map)} instead.
   */
  @Deprecated
  public static ByteString mapToByteString(final DataMap dataMap)
  {
    try
    {
      return CODEC.mapToByteString(dataMap);
    }
    catch (IOException e)
    {
      throw new RestLiInternalException(e);
    }
  }

  /**
   * Encode {@link DataMap} as a ByteString.
   *
   * @param dataMap input {@link DataMap}
   * @param headers Request or response headers. This is used to determine the codec to use to encode.
   * @return ByteString
   */
  public static ByteString mapToByteString(final DataMap dataMap, final Map<String, String> headers)
  {
    try
    {
      return DataMapConverter.getContentType(headers).getCodec().mapToByteString(dataMap);
    }
    catch (IOException | MimeTypeParseException e)
    {
      throw new RestLiInternalException(e);
    }
  }

  @Deprecated
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

  @Deprecated
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
   * Encode the {@link DataMap} as a ByteString.
   *
   * @param dataMap input {@link DataMap}
   * @return ByteString
   */
  public static ByteString mapToPsonByteString(final DataMap dataMap)
  {
    try
    {
      return PSON_DATA_CODEC.mapToByteString(dataMap);
    }
    catch (IOException e)
    {
      throw new RestLiInternalException(e);
    }
  }

  /**
   * Encode {@link DataMap} as a byte array using the provided codec.
   *
   * @param dataMap input {@link DataMap}
   * @param customCodec custom CODEC to use for encoding.
   * @return byte array
   */
  public static byte[] mapToBytes(final DataMap dataMap, DataCodec customCodec)
  {
    try
    {
      return customCodec.mapToBytes(dataMap);
    }
    catch (IOException e)
    {
      throw new RestLiInternalException(e);
    }
  }

  /**
   * Encode {@link DataMap} as a {@link ByteString} using the provided codec.
   *
   * @param dataMap input {@link DataMap}
   * @param customCodec custom CODEC to use for encoding.
   * @return encoded {@link ByteString}
   */
  public static ByteString mapToByteString(final DataMap dataMap, DataCodec customCodec)
  {
    try
    {
      return customCodec.mapToByteString(dataMap);
    }
    catch (IOException e)
    {
      throw new RestLiInternalException(e);
    }
  }

  /**
   * Remove {@link Data#NULL} from the input DataMap.
   * @param dataMap input data map which may contain {@link Data#NULL} values.
   */
  public static void removeNulls(DataMap dataMap)
  {
    try
    {
      Data.traverse(dataMap, new NullRemover());
    }
    catch (IOException ioe)
    {
      throw new RuntimeException(ioe);
    }
  }

  /**
   * Data TraverseCallback to remove Data.NULL from data map.
   */
  static class NullRemover implements Data.TraverseCallback
  {
    @Override
    public void startMap(DataMap dataMap)
    {
      // DataMap.values() and DataMap.entrySet() are Unmodifiable collections and hence,
      // we need to add to a list to delete
      List<String> emptyKeys = new ArrayList<>();
      dataMap.forEach((key, value) -> {
        if (value == Data.NULL)
        {
          emptyKeys.add(key);
        }
      });
      emptyKeys.forEach(dataMap::remove);
    }

    @Override
    public void startList(DataList dataList)
    {
      dataList.removeIf(value -> value.equals(Data.NULL));
    }
  }
}