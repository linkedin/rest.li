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
import com.linkedin.data.collections.CheckedUtil;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.ComplexKeySpec;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.EntityResponse;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.TypeSpec;
import com.linkedin.restli.internal.client.response.BatchEntityResponse;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Converts a raw batch get response {@link DataMap} into a {@link BatchEntityResponse}.
 *
 * @author Keren Jin
 */
public class BatchEntityResponseDecoder<K, V extends RecordTemplate> extends RestResponseDecoder<BatchKVResponse<K, EntityResponse<V>>>
{
  private final TypeSpec<V> _entityType;
  private final TypeSpec<K> _keyType;
  private final Map<String, CompoundKey.TypeInfo> _keyParts;
  private final ComplexKeySpec<?, ?> _complexKeyType;

  /**
   * @param keyType provides the class identifying the key type.
   *   <ul>
   *     <li>For collection resources must be a primitive or a typeref to a primitive.</li>
   *     <li>For an association resources must be {@link CompoundKey} and keyParts must contain an entry for each association key field.</li>
   *     <li>For complex resources must be {@link ComplexResourceKey}, keyKeyClass must contain the
   *           key's record template class and if the resource has a key params their record template type keyParamsClass must be provided.</li>
   * @param keyParts provides a map for association keys of each key name to {@link CompoundKey.TypeInfo}, for non-association resources must be an empty map.
   * @param complexKeyType provides the type of the key for complex key resources, otherwise null.
   */
  public BatchEntityResponseDecoder(TypeSpec<V> entityType, TypeSpec<K> keyType, Map<String, CompoundKey.TypeInfo> keyParts, ComplexKeySpec<?, ?> complexKeyType)
  {
    _entityType = entityType;
    _keyType = keyType;
    _keyParts = keyParts;
    _complexKeyType = complexKeyType;
  }

  @Override
  public Class<?> getEntityClass()
  {
    return EntityResponse.class;
  }

  @Override
  public BatchKVResponse<K, EntityResponse<V>> wrapResponse(DataMap dataMap, Map<String, String> headers, ProtocolVersion version)
    throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, IOException
  {
    final DataMap mergedResults = new DataMap();
    final DataMap inputResults = dataMap.containsKey(BatchResponse.RESULTS) ? dataMap.getDataMap(BatchResponse.RESULTS)
                                                                            : new DataMap();
    final DataMap inputStatuses = dataMap.containsKey(BatchResponse.STATUSES) ? dataMap.getDataMap(BatchResponse.STATUSES)
                                                                              : new DataMap();
    final DataMap inputErrors = dataMap.containsKey(BatchResponse.ERRORS) ? dataMap.getDataMap(BatchResponse.ERRORS)
                                                                          : new DataMap();

    final Set<String> mergedKeys = new HashSet<String>(inputResults.keySet());
    mergedKeys.addAll(inputStatuses.keySet());
    mergedKeys.addAll(inputErrors.keySet());

    for (String key : mergedKeys)
    {
      final DataMap entityResponseData = new DataMap();

      final Object entityData = inputResults.get(key);
      if (entityData != null)
      {
        CheckedUtil.putWithoutChecking(entityResponseData, EntityResponse.ENTITY, entityData);
      }

      final Object statusData = inputStatuses.get(key);
      if (statusData != null)
      {
        CheckedUtil.putWithoutChecking(entityResponseData, EntityResponse.STATUS, statusData);
      }

      final Object errorData = inputErrors.get(key);
      if (errorData != null)
      {
        CheckedUtil.putWithoutChecking(entityResponseData, EntityResponse.ERROR, errorData);
      }

      CheckedUtil.putWithoutChecking(mergedResults, key, entityResponseData);
    }

    final DataMap responseData = new DataMap();
    CheckedUtil.putWithoutChecking(responseData, BatchKVResponse.RESULTS, mergedResults);
    CheckedUtil.putWithoutChecking(responseData, BatchKVResponse.ERRORS, inputErrors);

    return new BatchEntityResponse<K, V>(responseData, _keyType, _entityType, _keyParts, _complexKeyType, version);
  }
}
