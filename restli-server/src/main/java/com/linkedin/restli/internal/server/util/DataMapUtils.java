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


import com.linkedin.data.DataComplex;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.codec.PsonDataCodec;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.template.DataTemplate;
import com.linkedin.data.template.JacksonDataTemplateCodec;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.internal.server.RestLiInternalException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;

public class DataMapUtils
{
  private static final JacksonDataCodec CODEC = new JacksonDataCodec();
  private static final PsonDataCodec PSON_DATA_CODEC = new PsonDataCodec();
  private static final JacksonDataTemplateCodec TEMPLATE_CODEC = new JacksonDataTemplateCodec();

  /**
   * Read {@link DataMap} from InputStream.
   *
   * @param stream input stream
   * @return {link @DataMap}
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
   * @return {link @DataMap}
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
   * @return a new object of the requested type constructed with DataMap read from input
   *         stream
   * @param <T> requested object type
   * @throws IOException on error reading input stream
   */
  public static <T extends RecordTemplate> T read(final InputStream stream,
                                                  final Class<T> recordClass) throws IOException
  {
    try
    {
      DataMap dataMap = CODEC.readMap(stream);
      return recordClass.getConstructor(DataMap.class).newInstance(dataMap);
    }
    catch (IllegalArgumentException e)
    {
      throw new RestLiInternalException(e);
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
    try
    {
      return CODEC.mapToBytes(dataMap);
    }
    catch (IOException e)
    {
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
}
