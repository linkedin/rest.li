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

package com.linkedin.restli.client.testutils;


import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.URIParamUtils;

import java.util.Collections;
import java.util.Map;


/**
 * Factory for creating a {@link BatchKVResponse} that can be used for tests.
 *
 * @author jflorencio
 * @author kparikh
 */
public class MockBatchKVResponseFactory
{
  private MockBatchKVResponseFactory() { }

  /**
   * Create a {@link BatchKVResponse} where the key is a class that extends {@link CompoundKey}
   *
   * @param keyClass the class of the key
   * @param keyParts the pieces that make up the {@link CompoundKey}
   * @param valueClass the value class
   * @param recordTemplates the data that will be returned for a call to {@link com.linkedin.restli.client.response.BatchKVResponse#getResults()}
   * @param errorResponses the data that will be returned for a call to {@link com.linkedin.restli.client.response.BatchKVResponse#getErrors()}
   * @param <K>
   * @param <V>
   * @return
   */
  public static <K extends CompoundKey, V extends RecordTemplate> BatchKVResponse<K, V> createWithCompoundKey
      (Class<K> keyClass,
       Map<String, CompoundKey.TypeInfo> keyParts,
       Class<V> valueClass,
       Map<K, V> recordTemplates,
       Map<K, ErrorResponse> errorResponses)
  {
    return create(keyClass, valueClass, keyParts, recordTemplates, errorResponses, AllProtocolVersions.BASELINE_PROTOCOL_VERSION);
  }

  /**
   * Create a {@link BatchKVResponse} where the key is a {@link ComplexResourceKey}
   *
   * @param valueClass the value class
   * @param keyKeyClass the class of the key part of the {@link ComplexResourceKey}
   * @param keyParamsClass the class of the params part of the {@link ComplexResourceKey}
   * @param recordTemplates the data that will be returned for a call to {@link com.linkedin.restli.client.response.BatchKVResponse#getResults()}
   *                        NOTE: the params part of the {@link ComplexResourceKey} is removed in this map. A new
   *                        instance of the params class is created with no data in it.
   * @param errorResponses the data that will be returned for a call to {@link com.linkedin.restli.client.response.BatchKVResponse#getErrors()}
   *                       NOTE: the params part of the {@link ComplexResourceKey} is removed in this map. A new
   *                       instance of the params class is created with no data in it.
   * @param <V>
   * @return
   */
  @SuppressWarnings("rawtypes")
  public static <KK extends RecordTemplate, KP extends RecordTemplate, V extends RecordTemplate> BatchKVResponse<ComplexResourceKey, V> createWithComplexKey
      (Class<V> valueClass,
       Class<KK> keyKeyClass,
       Class<KP> keyParamsClass,
       Map<ComplexResourceKey<KK, KP>, V> recordTemplates,
       Map<ComplexResourceKey<KK, KP>, ErrorResponse> errorResponses)
  {
    ProtocolVersion version = AllProtocolVersions.BASELINE_PROTOCOL_VERSION;

    DataMap batchResponseDataMap = buildDataMap(recordTemplates, errorResponses, version);

    return new BatchKVResponse<ComplexResourceKey, V>(batchResponseDataMap,
                                                      ComplexResourceKey.class,
                                                      valueClass,
                                                      null,
                                                      keyKeyClass,
                                                      keyParamsClass,
                                                      version);
  }

  /**
   * Creates a {@link BatchKVResponse} where the key is a primitive, or a typeref to a primitive.
   *
   * @param keyClass class for the key
   * @param valueClass class for the value
   * @param recordTemplates the data that will be returned for a call to {@link com.linkedin.restli.client.response.BatchKVResponse#getResults()}
   * @param errorResponses the data that will be returned for a call to {@link com.linkedin.restli.client.response.BatchKVResponse#getErrors()}
   * @param <K>
   * @param <V>
   * @return
   */
  public static <K, V extends RecordTemplate> BatchKVResponse<K, V> createWithPrimitiveKey(Class<K> keyClass,
                                                                                           Class<V> valueClass,
                                                                                           Map<K, V> recordTemplates,
                                                                                           Map<K, ErrorResponse> errorResponses)
  {
    return create(keyClass, valueClass, null, recordTemplates, errorResponses, AllProtocolVersions.BASELINE_PROTOCOL_VERSION);
  }

  /**
   * Creates a {@link BatchKVResponse} where the key is a typeref to a custom Java class.
   *
   * @param keyClass the custom Java class
   * @param typerefClass the typeref class (the generated class that extends {@link RecordTemplate})
   * @param valueClass class for the value
   * @param recordTemplates the data that will be returned for a call to {@link com.linkedin.restli.client.response.BatchKVResponse#getResults()}
   * @param errorResponses the data that will be returned for a call to {@link com.linkedin.restli.client.response.BatchKVResponse#getErrors()}
   * @param <K>
   * @param <TK>
   * @param <V>
   * @return
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public static <K, TK, V extends RecordTemplate> BatchKVResponse<K, V> createWithCustomTyperefKey(Class<K> keyClass,
                                                                                                   Class<TK> typerefClass,
                                                                                                   Class<V> valueClass,
                                                                                                   Map<K, V> recordTemplates,
                                                                                                   Map<K, ErrorResponse> errorResponses)
  {
    ProtocolVersion version = AllProtocolVersions.BASELINE_PROTOCOL_VERSION;

    DataMap batchResponseDataMap = buildDataMap(recordTemplates, errorResponses, version);

    return new BatchKVResponse(batchResponseDataMap,
                               typerefClass,
                               valueClass,
                               Collections.<String, CompoundKey.TypeInfo>emptyMap(),
                               version);
  }

  private static <K, V extends RecordTemplate> DataMap buildDataMap(Map<K, V> recordTemplates,
                                                                    Map<K, ErrorResponse> errorResponses,
                                                                    ProtocolVersion version)
  {
    DataMap batchResponseDataMap = new DataMap();
    DataMap rawBatchData = new DataMap();
    for (Map.Entry<K, V> entry : recordTemplates.entrySet())
    {
      String stringKey = URIParamUtils.encodeKeyForBody(entry.getKey(), false, version);
      rawBatchData.put(stringKey, entry.getValue().data());
    }
    batchResponseDataMap.put(BatchResponse.RESULTS, rawBatchData);

    DataMap rawErrorData = new DataMap();
    for (Map.Entry<K, ErrorResponse> errorResponse : errorResponses.entrySet())
    {
      rawErrorData.put(String.valueOf(errorResponse.getKey()), errorResponse.getValue().data());
    }
    batchResponseDataMap.put(BatchResponse.ERRORS, rawErrorData);
    return batchResponseDataMap;
  }

  private static <K, V extends RecordTemplate> BatchKVResponse<K, V> create
      (Class<K> keyClass,
       Class<V> valueClass,
       Map<String, CompoundKey.TypeInfo> keyParts,
       Map<K, V> recordTemplates,
       Map<K, ErrorResponse> errorResponses,
       ProtocolVersion version)
  {
    DataMap batchResponseDataMap = buildDataMap(recordTemplates, errorResponses, version);

    return new BatchKVResponse<K, V>(batchResponseDataMap,
                                     keyClass,
                                     valueClass,
                                     keyParts,
                                     null,
                                     null,
                                     version);
  }
}
