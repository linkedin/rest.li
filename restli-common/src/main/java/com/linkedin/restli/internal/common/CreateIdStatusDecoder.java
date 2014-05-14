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

package com.linkedin.restli.internal.common;


import com.linkedin.data.DataMap;
import com.linkedin.restli.common.ComplexKeySpec;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.CreateIdStatus;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.TypeSpec;

import java.util.Map;


/**
 * Decode an individual CreateIdStatus
 * @author Moira Tagle
 */
public class CreateIdStatusDecoder<K>
{
  private final TypeSpec<K> _keyType;
  private final Map<String, CompoundKey.TypeInfo> _keyParts;
  private final ComplexKeySpec<?, ?> _complexKeyType;
  private final ProtocolVersion _version;

  public CreateIdStatusDecoder(TypeSpec<K> keyType,
                               Map<String, CompoundKey.TypeInfo> keyParts,
                               ComplexKeySpec<?, ?> complexKeyType,
                               ProtocolVersion version)
  {
    _keyType = keyType;
    _keyParts = keyParts;
    _complexKeyType = complexKeyType;
    _version = version;
  }

  public CreateIdStatus<K> makeValue(DataMap dataMap)
  {
    K key;
    String id = dataMap.getString("id");
    if (id == null)
    {
      key = null;
    }
    else
    {
      key = ResponseUtils.convertKey(id, _keyType, _keyParts, _complexKeyType, _version);
    }
    return new CreateIdStatus<K>(dataMap, key);
  }
}
