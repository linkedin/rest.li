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
import com.linkedin.restli.common.BatchCreateIdResponse;
import com.linkedin.restli.common.ComplexKeySpec;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.CreateIdStatus;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.TypeSpec;
import com.linkedin.restli.internal.common.CreateIdStatusDecoder;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;


/**
 * Decoder for {@link com.linkedin.restli.common.BatchCreateIdResponse}s.
 * Includes information needed to decode keys from serialized form into strongly typed keys.
 *
 * @author Moira Tagle
 */
public class BatchCreateIdDecoder<K> extends RestResponseDecoder<BatchCreateIdResponse<K>>
{
  private final TypeSpec<K> _keyType;
  private final Map<String, CompoundKey.TypeInfo> _keyParts;
  private final ComplexKeySpec<?, ?> _complexKeyType;

  public BatchCreateIdDecoder(TypeSpec<K> keyType,
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
    return CreateIdStatus.class;
  }

  @Override
  protected BatchCreateIdResponse<K> wrapResponse(DataMap dataMap, Map<String, String> headers, ProtocolVersion version)
      throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException
  {
    CreateIdStatusDecoder<K> decoder = new CreateIdStatusDecoder<K>(_keyType, _keyParts, _complexKeyType, version);
    return dataMap == null ? null : new BatchCreateIdResponse<K>(dataMap, decoder);
  }
}
