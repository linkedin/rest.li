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
import com.linkedin.restli.client.response.CreateResponse;
import com.linkedin.restli.common.ComplexKeySpec;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.TypeSpec;
import com.linkedin.restli.internal.common.HeaderUtil;
import com.linkedin.restli.internal.common.ResponseUtils;

import java.util.HashMap;
import java.util.Map;


/**
 * Decoder for responses returned by 1.0 builder creates.
 *
 * @author Moira Tagle
 */
public class CreateResponseDecoder<K> extends EmptyResponseDecoder
{
  private final TypeSpec<K> _keyType;
  private final Map<String, CompoundKey.TypeInfo> _keyParts;
  private final ComplexKeySpec<?, ?> _complexKeyType;

  /**
   * @param keyType provides the class identifying the key type.
   *   <ul>
   *     <li>For collection resources must be a primitive or a typeref to a primitive.</li>
   *     <li>For an association resources must be {@link CompoundKey} and keyParts must contain an entry for each association key field.</li>
   *     <li>For complex resources must be {@link com.linkedin.restli.common.ComplexResourceKey}, keyKeyClass must contain the
   *         key's record template class and if the resource has a key params their record template type keyParamsClass must be provided.</li>
   * @param keyParts provides a map for association keys of each key name to {@link com.linkedin.restli.common.CompoundKey.TypeInfo}, for non-association resources must be an empty map.
   * @param complexKeyType provides the type of the key for complex key resources, otherwise null.
   */
  public CreateResponseDecoder(TypeSpec<K> keyType,
                               Map<String, CompoundKey.TypeInfo> keyParts,
                               ComplexKeySpec<?, ?> complexKeyType)
  {
    _keyType = keyType;
    _keyParts = keyParts;
    _complexKeyType = complexKeyType;
  }

  @Override
  public Class<?> getEntityClass()
  {
    return CreateResponse.class;
  }

  @Override
  public Response<EmptyRecord> decodeResponse(RestResponse restResponse)
    throws RestLiDecodingException
  {
    final Response<EmptyRecord> rawResponse = super.decodeResponse(restResponse);

    // ResponseImpl will make the headers unmodifiable
    final Map<String, String> modifiableHeaders = new HashMap<String, String>(rawResponse.getHeaders());

    // remove ID header to prevent user to access the weakly typed ID
    modifiableHeaders.remove(RestConstants.HEADER_ID);
    modifiableHeaders.remove(RestConstants.HEADER_RESTLI_ID);

    return new ResponseImpl<EmptyRecord>(rawResponse.getStatus(), modifiableHeaders, rawResponse.getEntity(), rawResponse.getError());
  }

  @Override
  public CreateResponse<K> wrapResponse(DataMap dataMap, Map<String, String> headers, ProtocolVersion version)
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

    return new CreateResponse<K>(key);
  }
}
