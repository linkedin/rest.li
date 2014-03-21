/*
   Copyright (c) 2013 LinkedIn Corp.

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
import com.linkedin.restli.client.util.RestliBuilderUtils;
import com.linkedin.restli.common.BatchRequest;
import com.linkedin.restli.common.CollectionRequest;
import com.linkedin.restli.common.ComplexKeySpec;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.KeyValueRecord;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.TypeSpec;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.URLEscaper;

import java.util.Map;


/**
 * @author kparikh
 */
public class CollectionRequestUtil
{
  @SuppressWarnings({"unchecked", "rawtypes"})
  public static <V extends RecordTemplate> BatchRequest<V> convertToBatchRequest(CollectionRequest<KeyValueRecord> elementList,
                                                                                 TypeSpec<?> keyType,
                                                                                 ComplexKeySpec<?, ?> complexKeyType,
                                                                                 Map<String, CompoundKey.TypeInfo> keyParts,
                                                                                 TypeSpec<V> valueType)
  {
    return convertToBatchRequest(elementList,
                                 keyType, complexKeyType,
                                 keyParts, valueType,
                                 AllProtocolVersions.BASELINE_PROTOCOL_VERSION);
  }

  /**
   * Converts the new way of representing {@link com.linkedin.restli.client.BatchUpdateRequest}s and
   * {@link com.linkedin.restli.client.BatchPartialUpdateRequest}s bodies into the old way
   * @param elementList new style encoding
   * @param keyClass
   * @param keyKeyClass
   * @param keyParamsClass
   * @param keyParts
   * @param valueClass
   * @param <V>
   * @return a data map with one key, "entities". "entities" maps to another data map (as in the old body encoding)
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public static <V extends RecordTemplate> BatchRequest<V> convertToBatchRequest(CollectionRequest<KeyValueRecord> elementList,
                                                                                 Class<?> keyClass,
                                                                                 Class<? extends RecordTemplate> keyKeyClass,
                                                                                 Class<? extends RecordTemplate> keyParamsClass,
                                                                                 Map<String, CompoundKey.TypeInfo> keyParts,
                                                                                 Class<V> valueClass)
  {
    return convertToBatchRequest(elementList,
                                 TypeSpec.forClassMaybeNull(keyClass),
                                 ComplexKeySpec.forClassesMaybeNull(keyKeyClass, keyParamsClass),
                                 keyParts,
                                 TypeSpec.forClassMaybeNull(valueClass));
  }

  /**
   * Converts the new way of representing {@link com.linkedin.restli.client.BatchUpdateRequest}s and
   * {@link com.linkedin.restli.client.BatchPartialUpdateRequest}s bodies into the old way
   * @param elementList new style encoding
   * @param keyType
   * @param complexKeyType
   * @param keyParts
   * @param valueType
   * @param version protocol version to use for encoding
   * @param <V>
   * @return a data map with one key, "entities". "entities" maps to another data map (as in the old body encoding)
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public static <V extends RecordTemplate> BatchRequest<V> convertToBatchRequest(CollectionRequest<KeyValueRecord> elementList,
                                                                                TypeSpec<?> keyType,
                                                                                ComplexKeySpec<?, ?> complexKeyType,
                                                                                Map<String, CompoundKey.TypeInfo> keyParts,
                                                                                TypeSpec<V> valueType,
                                                                                ProtocolVersion version)
  {
    BatchRequest<V> batchRequest = new BatchRequest<V>(new DataMap(), valueType);

    for (KeyValueRecord keyValueRecord: elementList.getElements())
    {
      V value = (V) keyValueRecord.getValue(valueType);
      Object key = null;

      if(keyType != null)
      {
        if (keyType.getType().equals(ComplexResourceKey.class))
        {
          // complex keys
          key = keyValueRecord.getComplexKey(complexKeyType);
        }

        else if (CompoundKey.class.isAssignableFrom(keyType.getType()))
        {
          key = keyValueRecord.getCompoundKey(keyParts);
        }

        else
        {
          // primitive keys
          key = keyValueRecord.getPrimitiveKey(keyType);
        }
      }

      batchRequest.getEntities().put(RestliBuilderUtils.keyToString(key, URLEscaper.Escaping.NO_ESCAPING, version), value);
    }

    return batchRequest;
  }
}
