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


import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.ComplexKeySpec;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.EntityResponse;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.TypeSpec;
import com.linkedin.restli.internal.client.response.BatchEntityResponse;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.URIParamUtils;


/**
 * Factory for creating a {@link BatchKVResponse} that can be used for tests.
 *
 * @author xma
 */
public class MockBatchEntityResponseFactory
{
  private MockBatchEntityResponseFactory() { }

  /**
   * Create a {@link BatchKVResponse} where the key is a class that extends {@link CompoundKey}
   *
   * @param keyClass the class of the key
   * @param keyParts the pieces that make up the {@link CompoundKey}
   * @param valueClass the value class
   * @param statuses The HTTP status codes that will be returned as part of {@link EntityResponse}s returned in {@link com.linkedin.restli.client.response.BatchKVResponse#getResults()}
   * @param recordTemplates the data that will be returned for a call to {@link com.linkedin.restli.client.response.BatchKVResponse#getResults()}
   * @param errorResponses the data that will be returned for a call to {@link com.linkedin.restli.client.response.BatchKVResponse#getErrors()}
   * @param <K>
   * @param <V>
   * @return
   */
  public static <K extends CompoundKey, V extends RecordTemplate> BatchKVResponse<K, EntityResponse<V>> createWithCompoundKey(Class<K> keyClass,
                                                                                                                              Map<String, CompoundKey.TypeInfo> keyParts,
                                                                                                                              Class<V> valueClass,
                                                                                                                              Map<K, V> recordTemplates,
                                                                                                                              Map<K, HttpStatus> statuses,
                                                                                                                              Map<K, ErrorResponse> errorResponses)
  {
    ProtocolVersion version = AllProtocolVersions.BASELINE_PROTOCOL_VERSION;

    return create(keyClass, valueClass, keyParts, recordTemplates, statuses, errorResponses, version);
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
   * @param statuses The HTTP status codes that will be returned as part of {@link EntityResponse}s
   *                 returned in {@link com.linkedin.restli.client.response.BatchKVResponse#getResults()}
   * @param errorResponses the data that will be returned for a call to {@link com.linkedin.restli.client.response.BatchKVResponse#getErrors()}
   *                       NOTE: the params part of the {@link ComplexResourceKey} is removed in this map. A new
   *                       instance of the params class is created with no data in it.
   * @param <V>
   * @return
   */
  @SuppressWarnings("rawtypes")
  public static <KK extends RecordTemplate, KP extends RecordTemplate, V extends RecordTemplate> BatchKVResponse<ComplexResourceKey<KK, KP>, EntityResponse<V>> createWithComplexKey(Class<V> valueClass,
                                                                                                                                                                                     Class<KK> keyKeyClass,
                                                                                                                                                                                     Class<KP> keyParamsClass,
                                                                                                                                                                                     Map<ComplexResourceKey<KK, KP>, V> recordTemplates,
                                                                                                                                                                                     Map<ComplexResourceKey<KK, KP>, HttpStatus> statuses,
                                                                                                                                                                                     Map<ComplexResourceKey<KK, KP>, ErrorResponse> errorResponses)
  {
    ProtocolVersion version = AllProtocolVersions.BASELINE_PROTOCOL_VERSION;

    DataMap batchResponseDataMap = buildDataMap(recordTemplates, statuses, errorResponses, version);

    @SuppressWarnings("unchecked")
    BatchKVResponse<ComplexResourceKey<KK, KP>, EntityResponse<V>> response =
      (BatchKVResponse<ComplexResourceKey<KK, KP>, EntityResponse<V>>) (Object) new BatchEntityResponse<ComplexResourceKey, V>(batchResponseDataMap,
                                                                                                                               new TypeSpec<ComplexResourceKey>(ComplexResourceKey.class),
                                                                                                                               TypeSpec.forClassMaybeNull(valueClass),
                                                                                                                               null,
                                                                                                                               ComplexKeySpec.forClassesMaybeNull(keyKeyClass, keyParamsClass),
                                                                                                                               version);
    return response;
  }

  /**
   * Creates a {@link BatchKVResponse} where the key is a primitive, or a typeref to a primitive.
   *
   * @param keyClass class for the key
   * @param valueClass class for the value
   * @param recordTemplates the data that will be returned for a call to {@link com.linkedin.restli.client.response.BatchKVResponse#getResults()}
   * @param statuses The HTTP status codes that will be returned as part of {@link EntityResponse}s returned in {@link com.linkedin.restli.client.response.BatchKVResponse#getResults()}
   * @param errorResponses the data that will be returned for a call to {@link com.linkedin.restli.client.response.BatchKVResponse#getErrors()}
   * @param <K>
   * @param <V>
   * @return
   */
  public static <K, V extends RecordTemplate> BatchKVResponse<K, EntityResponse<V>> createWithPrimitiveKey(Class<K> keyClass,
                                                                                           Class<V> valueClass,
                                                                                           Map<K, V> recordTemplates,
                                                                                           Map<K, HttpStatus> statuses,
                                                                                           Map<K, ErrorResponse> errorResponses)
  {
    ProtocolVersion version = AllProtocolVersions.BASELINE_PROTOCOL_VERSION;

    return create(keyClass, valueClass, null, recordTemplates, statuses, errorResponses, version);
  }

  /**
   * Creates a {@link BatchKVResponse} where the key is a typeref to a custom Java class.
   *
   * @param keyClass the custom Java class
   * @param typerefClass the typeref class (the generated class that extends {@link RecordTemplate})
   * @param valueClass class for the value
   * @param recordTemplates the data that will be returned for a call to {@link com.linkedin.restli.client.response.BatchKVResponse#getResults()}
   * @param statuses The HTTP status codes that will be returned as part of {@link EntityResponse}s returned in {@link com.linkedin.restli.client.response.BatchKVResponse#getResults()}
   * @param errorResponses the data that will be returned for a call to {@link com.linkedin.restli.client.response.BatchKVResponse#getErrors()}
   * @param <K>
   * @param <TK>
   * @param <V>
   * @return
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public static <K, TK, V extends RecordTemplate> BatchKVResponse<K, EntityResponse<V>> createWithCustomTyperefKey(Class<K> keyClass,
                                                                                                                   Class<TK> typerefClass,
                                                                                                                   Class<V> valueClass,
                                                                                                                   Map<K, V> recordTemplates,
                                                                                                                   Map<K, HttpStatus> statuses,
                                                                                                                   Map<K, ErrorResponse> errorResponses)
  {
    ProtocolVersion version = AllProtocolVersions.BASELINE_PROTOCOL_VERSION;

    DataMap batchResponseDataMap = buildDataMap(recordTemplates, statuses, errorResponses, version);

    return new BatchEntityResponse(batchResponseDataMap,
                                   TypeSpec.forClassMaybeNull(typerefClass),
                                   TypeSpec.forClassMaybeNull(valueClass),
                                   Collections.<String, CompoundKey.TypeInfo>emptyMap(),
                                   null,
                                   version);
  }

  private static <K, V extends RecordTemplate> DataMap buildDataMap(Map<K, V> recordTemplates,
                                                                    Map<K, HttpStatus> statuses,
                                                                    Map<K, ErrorResponse> errorResponses,
                                                                    ProtocolVersion version)
  {
    DataMap batchResponseDataMap = new DataMap();
    DataMap resultData = new DataMap();
    for (K key : recordTemplates.keySet())
    {
      String stringKey = URIParamUtils.encodeKeyForBody(key, false, version);
      RecordTemplate recordTemplate = recordTemplates.get(key);
      if (recordTemplate != null)
      {
        resultData.put(stringKey, recordTemplate.data());
      }
    }
    DataMap statusData = new DataMap();
    for(K key : statuses.keySet())
    {
      String stringKey = URIParamUtils.encodeKeyForBody(key, false, version);
      HttpStatus status = statuses.get(key);
      if (status != null)
      {
        statusData.put(stringKey, status.getCode());
      }
    }
    DataMap errorData = new DataMap();
    for(K key : errorResponses.keySet())
    {
      String stringKey = URIParamUtils.encodeKeyForBody(key, false, version);
      ErrorResponse errorResponse = errorResponses.get(key);
      if (errorResponse != null)
      {
        errorData.put(stringKey, errorResponse.data());
      }
    }
    batchResponseDataMap.put(BatchResponse.RESULTS, resultData);
    batchResponseDataMap.put(BatchResponse.STATUSES, statusData);
    batchResponseDataMap.put(BatchResponse.ERRORS, errorData);
    return batchResponseDataMap;
  }

  private static <K, V extends RecordTemplate> BatchKVResponse<K, EntityResponse<V>> create(Class<K> keyClass,
                                                                                            Class<V> valueClass,
                                                                                            Map<String, CompoundKey.TypeInfo> keyParts,
                                                                                            Map<K, V> recordTemplates,
                                                                                            Map<K, HttpStatus> statuses,
                                                                                            Map<K, ErrorResponse> errorResponses,
                                                                                            ProtocolVersion version)
  {
    DataMap batchResponseDataMap = buildDataMap(recordTemplates, statuses, errorResponses, version);

    return new BatchEntityResponse<K, V>(batchResponseDataMap,
                                         TypeSpec.forClassMaybeNull(keyClass),
                                         TypeSpec.forClassMaybeNull(valueClass),
                                         keyParts,
                                         null,
                                         version);
  }
}
