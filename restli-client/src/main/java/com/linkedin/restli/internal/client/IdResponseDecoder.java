/*
   Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.restli.internal.client;

import com.linkedin.data.DataMap;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.RestLiDecodingException;
import com.linkedin.restli.common.ComplexKeySpec;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.IdResponse;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.TypeSpec;
import com.linkedin.restli.internal.common.HeaderUtil;
import com.linkedin.restli.internal.common.ResponseUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Moira Tagle
 */
public class IdResponseDecoder<K> extends RestResponseDecoder<IdResponse<K>>
{
  private final TypeSpec<K> _keyType;
  private final Map<String, CompoundKey.TypeInfo> _keyParts;
  private final ComplexKeySpec<?, ?> _complexKeyType;

  public IdResponseDecoder(TypeSpec<K> keyType,
                           Map<String, CompoundKey.TypeInfo> keyParts,
                           ComplexKeySpec<?, ?> complexKeyType)
  {
    _keyType = keyType;
    _keyParts = keyParts;
    _complexKeyType = complexKeyType;
  }

  @Override
  public Class<IdResponse> getEntityClass()
  {
    return IdResponse.class;
  }

  @Override
  public Response<IdResponse<K>> decodeResponse(RestResponse restResponse)
    throws RestLiDecodingException
  {
    final Response<IdResponse<K>> rawResponse = super.decodeResponse(restResponse);

    // ResponseImpl will make the headers unmodifiable
    final Map<String, String> modifiableHeaders = new HashMap<String, String>(rawResponse.getHeaders());

    // remove ID header to prevent user to access the weakly typed ID
    modifiableHeaders.remove(RestConstants.HEADER_ID);
    modifiableHeaders.remove(RestConstants.HEADER_RESTLI_ID);

    return new ResponseImpl<IdResponse<K>>(rawResponse.getStatus(), modifiableHeaders, rawResponse.getEntity(), rawResponse.getError());
  }

  @Override
  protected IdResponse<K> wrapResponse(DataMap dataMap, Map<String, String> headers, ProtocolVersion version)
          throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, IOException
  {
    String id = HeaderUtil.getIdHeaderValue(headers);
    K key;
    if (id == null)
    {
      key = null;
    }
    else
    {
      key = ResponseUtils.convertKey(id, _keyType, _keyParts, _complexKeyType, version);
    }
    return new IdResponse<K>(key);
  }
}
