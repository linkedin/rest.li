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
import com.linkedin.restli.common.ComplexKeySpec;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.CreateIdEntityStatus;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.TypeSpec;

import com.linkedin.restli.common.BatchCreateIdEntityResponse;
import com.linkedin.restli.internal.common.CreateIdEntityStatusDecoder;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class BatchCreateIdEntityDecoder<K, V extends RecordTemplate> extends RestResponseDecoder<BatchCreateIdEntityResponse<K, V>>
{

  private final TypeSpec<K> _keyType;
  private final Map<String, CompoundKey.TypeInfo> _keyParts;
  private final ComplexKeySpec<?, ?> _complexKeyType;
  private final TypeSpec<V> _valueType;

  public BatchCreateIdEntityDecoder(TypeSpec<K> keyType,
                                    TypeSpec<V> valueType,
                                    Map<String, CompoundKey.TypeInfo> keyParts,
                                    ComplexKeySpec<?, ?> complexKeyType)
  {
    _keyType = keyType;
    _keyParts = keyParts;
    _complexKeyType = complexKeyType;
    _valueType = valueType;
  }

  public Class<?> getEntityClass()
  {
    return CreateIdEntityStatus.class;
  }

  @Override
  public BatchCreateIdEntityResponse<K, V> wrapResponse(DataMap dataMap, Map<String, String> headers, ProtocolVersion version)
      throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, IOException
  {

    CreateIdEntityStatusDecoder<K, V> decoder = new CreateIdEntityStatusDecoder<>(_keyType, _valueType, _keyParts, _complexKeyType, version);
    return dataMap == null ? null : new BatchCreateIdEntityResponse<>(dataMap, decoder);
  }


}
