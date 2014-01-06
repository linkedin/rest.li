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
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.KeyValueRecord;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.URLEscaper;
import java.util.Map;


/**
 * @author kparikh
 */
public class CollectionRequestUtil
{
  public static <V extends RecordTemplate> BatchRequest<V> convertToBatchRequest(CollectionRequest<KeyValueRecord> elementList,
                                                                                 Class<?> keyClass,
                                                                                 Class<? extends RecordTemplate> keyKeyClass,
                                                                                 Class<? extends RecordTemplate> keyParamsClass,
                                                                                 Map<String, CompoundKey.TypeInfo> keyParts,
                                                                                 Class<V> valueClass)
  {
    return convertToBatchRequest(elementList,
                                 keyClass,
                                 keyKeyClass,
                                 keyParamsClass,
                                 keyParts,
                                 valueClass,
                                 RestConstants.DEFAULT_PROTOCOL_VERSION);
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
   * @param version protocol version to use for encoding
   * @param <V>
   * @return a data map with one key, "entities". "entities" maps to another data map (as in the old body encoding)
   */
  @SuppressWarnings("unchecked")
  public static <V extends RecordTemplate> BatchRequest<V> convertToBatchRequest(CollectionRequest<KeyValueRecord> elementList,
                                                                                Class<?> keyClass,
                                                                                Class<? extends RecordTemplate> keyKeyClass,
                                                                                Class<? extends RecordTemplate> keyParamsClass,
                                                                                Map<String, CompoundKey.TypeInfo> keyParts,
                                                                                Class<V> valueClass,
                                                                                ProtocolVersion version)
  {
    BatchRequest<V> batchRequest = new BatchRequest<V>(new DataMap(), valueClass);

    for (KeyValueRecord keyValueRecord: elementList.getElements())
    {
      V value = (V) keyValueRecord.getValue(valueClass);
      Object key;

      if (keyClass.equals(ComplexResourceKey.class))
      {
        // complex keys
        key = keyValueRecord.getComplexKey(keyKeyClass, keyParamsClass);
      }

      else if (CompoundKey.class.isAssignableFrom(keyClass))
      {
        key = keyValueRecord.getCompoundKey(keyParts);
      }

      else
      {
        // primitive keys
        key = keyValueRecord.getPrimitiveKey(keyClass);
      }

      batchRequest.getEntities().put(RestliBuilderUtils.keyToString(key, URLEscaper.Escaping.NO_ESCAPING, version), value);
    }

    return batchRequest;
  }
}
