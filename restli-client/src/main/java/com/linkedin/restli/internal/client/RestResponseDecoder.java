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

/**
 * $Id: $
 */

package com.linkedin.restli.internal.client;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpCookie;
import java.util.Collections;
import java.util.Map;

import com.linkedin.data.ByteString;
import com.linkedin.data.DataMap;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.codec.PsonDataCodec;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.RestLiDecodingException;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.CookieUtil;
import com.linkedin.restli.internal.common.ProtocolVersionUtil;

/**
 * Converts a raw RestResponse into a type-bound response.  The class is abstract
 * and must be subclassed according to the expected response type.
 * @author Steven Ihde
 * @version $Revision: $
 */

public abstract class RestResponseDecoder<T>
{
  private static final JacksonDataCodec JACKSON_DATA_CODEC = new JacksonDataCodec();
  private static final PsonDataCodec    PSON_DATA_CODEC    = new PsonDataCodec();

  public Response<T> decodeResponse(RestResponse restResponse) throws RestLiDecodingException
  {
    ResponseImpl<T> response = new ResponseImpl<T>(restResponse.getStatus(), restResponse.getHeaders(), CookieUtil.decodeSetCookies(restResponse.getCookies()));

    ByteString entity = restResponse.builder().getEntity();

    try
    {
      DataMap dataMap;
      if (entity.length() == 0)
      {
        dataMap = null;
      }
      else
      {
        InputStream inputStream = entity.asInputStream();
        if ((RestConstants.HEADER_VALUE_APPLICATION_PSON)
                .equalsIgnoreCase(restResponse.getHeader(RestConstants.HEADER_CONTENT_TYPE)))
        {
          dataMap = PSON_DATA_CODEC.readMap(inputStream);
        }
        else
        {
          dataMap = JACKSON_DATA_CODEC.readMap(inputStream);
        }
        inputStream.close();
      }
      response.setEntity(wrapResponse(dataMap, restResponse.getHeaders(), ProtocolVersionUtil.extractProtocolVersion(response.getHeaders())));
      return response;
    }
    catch (IOException e)
    {
      throw new RestLiDecodingException("Could not decode REST response", e);
    }
    catch (InstantiationException e)
    {
      throw new IllegalStateException(e);
    }
    catch (IllegalAccessException e)
    {
      throw new IllegalStateException(e);
    }
    catch (InvocationTargetException e)
    {
      throw new IllegalStateException(e);
    }
    catch (NoSuchMethodException e)
    {
      throw new IllegalStateException(e);
    }
  }

  public abstract Class<?> getEntityClass();

  /**
   * @deprecated use {@link #wrapResponse(com.linkedin.data.DataMap, java.util.Map, com.linkedin.restli.common.ProtocolVersion)}
   */
  @Deprecated
  public T wrapResponse(DataMap dataMap)
                  throws InvocationTargetException, NoSuchMethodException, InstantiationException, IOException, IllegalAccessException
  {
    return wrapResponse(dataMap, Collections.<String, String>emptyMap(), AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion());
  }

  protected abstract T wrapResponse(DataMap dataMap, Map<String, String> headers, ProtocolVersion version)
                  throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, IOException;
}
