/*
   Copyright (c) 2015 LinkedIn Corp.

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
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.RestLiDecodingException;

import com.linkedin.restli.common.IdEntityResponse;
import com.linkedin.restli.common.TypeSpec;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.ComplexKeySpec;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.internal.common.HeaderUtil;
import com.linkedin.restli.internal.common.ResponseUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Boyang Chen
 */
public class IdEntityResponseDecoder<K, V extends RecordTemplate> extends RestResponseDecoder<IdEntityResponse<K, V>>
{
  private final TypeSpec<K> _keyType;
  private final Map<String, CompoundKey.TypeInfo> _keyParts;
  private final ComplexKeySpec<?, ?> _complexKeyType;
  private final Class<V> _entityClass;

  public IdEntityResponseDecoder(TypeSpec<K> keyType,
                                 Map<String, CompoundKey.TypeInfo> keyParts,
                                 ComplexKeySpec<?, ?> complexKeyType,
                                 Class<V> entityClass)
  {
    _keyType = keyType;
    _keyParts = keyParts;
    _complexKeyType = complexKeyType;
    _entityClass = entityClass;
  }

  @Override
  public Class<?> getEntityClass()
  {
    return _entityClass;
  }

  @Override
  public Response<IdEntityResponse<K, V>> decodeResponse(RestResponse restResponse)
      throws RestLiDecodingException
  {
    final Response<IdEntityResponse<K, V>> rawResponse = super.decodeResponse(restResponse);

    final Map<String, String> modifiableHeaders = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
    modifiableHeaders.putAll(rawResponse.getHeaders());

    modifiableHeaders.remove(RestConstants.HEADER_ID);
    modifiableHeaders.remove(RestConstants.HEADER_RESTLI_ID);

    return new ResponseImpl<>(rawResponse.getStatus(), modifiableHeaders, rawResponse.getCookies(), rawResponse.getEntity(), rawResponse.getError());
  }

  @Override
  @SuppressWarnings("unchecked")
  public IdEntityResponse<K, V> wrapResponse(DataMap dataMap, Map<String, String> headers, ProtocolVersion version)
      throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException
  {
    String id = HeaderUtil.getIdHeaderValue(headers);
    K key = id == null ? null : (K) ResponseUtils.convertKey(id, _keyType, _keyParts, _complexKeyType, version);
    V entity = dataMap == null ? null : _entityClass.getConstructor(DataMap.class).newInstance(dataMap);
    return new IdEntityResponse<K, V>(key, entity);
  }
}
