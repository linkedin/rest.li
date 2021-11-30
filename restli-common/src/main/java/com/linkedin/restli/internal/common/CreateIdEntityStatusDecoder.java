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

package com.linkedin.restli.internal.common;

import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.collections.CheckedUtil;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.ComplexKeySpec;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.CreateIdEntityStatus;
import com.linkedin.restli.common.CreateIdStatus;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.TypeSpec;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class CreateIdEntityStatusDecoder<K, V extends RecordTemplate> extends CreateIdStatusDecoder<K>
{
  private final TypeSpec<V> _valueType;


  public CreateIdEntityStatusDecoder( TypeSpec<K> keyType, TypeSpec<V> valueType,
                                      Map<String, CompoundKey.TypeInfo> keyParts,
                                      ComplexKeySpec<?, ?> complexKeyType,
                                      ProtocolVersion version)
  {
    super(keyType, keyParts, complexKeyType, version);
    _valueType = valueType;
  }

  @SuppressWarnings("unchecked")
  @Override
  public CreateIdEntityStatus<K, V> makeValue(DataMap dataMap)
      throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException
  {
    CreateIdStatus<K> idStatus = super.makeValue(dataMap);
    K key = idStatus.getKey();
    DataMap finalMap = new DataMap(idStatus.data());
    DataList listElements = new DataList();
    V entity = null;
    if (dataMap.get("error") == null && dataMap.get("entity") != null)
    {
      entity = _valueType.getType().getConstructor(DataMap.class).newInstance(dataMap.get("entity"));
      CheckedUtil.addWithoutChecking(listElements, entity.data());
    }
    finalMap.put("entity", listElements);

    return new CreateIdEntityStatus<>(finalMap, key, entity);
  }

}
