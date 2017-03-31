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

package com.linkedin.restli.internal.client.response;

import com.linkedin.data.DataMap;
import com.linkedin.data.collections.CheckedUtil;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.ComplexKeySpec;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.EntityResponse;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.TypeSpec;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Specialized {@link BatchKVResponse} whose value class is {@link EntityResponse}.
 * <p/>
 * Note: The format of DataMap returned by {@link #data()} changed in version 11.* and above of pegasus. It now returns
 * the DataMap returned by the server before the results, statuses and errors are merged into EntityResponse. <p/>
 *
 * @author Keren Jin
 */
public class BatchEntityResponse<K, E extends RecordTemplate> extends BatchKVResponse<K, EntityResponse<E>>
{
  private final TypeSpec<E> _entityType;

  public BatchEntityResponse(DataMap data,
                             TypeSpec<K> keyType,
                             TypeSpec<E> entityType,
                             Map<String, CompoundKey.TypeInfo> keyParts,
                             ComplexKeySpec<?, ?> complexKeyType,
                             ProtocolVersion version)
  {
    super(data);

    _entityType = entityType;

    createSchema(getEntityResponseValueClass());
    deserializeData(keyType, keyParts, complexKeyType, version);
  }

  @Override
  protected void deserializeData(TypeSpec<K> keyType, Map<String, CompoundKey.TypeInfo> keyParts,
      ComplexKeySpec<?, ?> complexKeyType, ProtocolVersion version)
  {
    DataMap dataMap = data();
    final DataMap convertedData = new DataMap();
    final DataMap mergedResults = new DataMap();
    final DataMap inputResults = dataMap.containsKey(BatchResponse.RESULTS) ? dataMap.getDataMap(BatchResponse.RESULTS)
        : new DataMap();
    final DataMap inputStatuses = dataMap.containsKey(BatchResponse.STATUSES) ? dataMap.getDataMap(BatchResponse.STATUSES)
        : new DataMap();
    final DataMap inputErrors = dataMap.containsKey(BatchResponse.ERRORS) ? dataMap.getDataMap(BatchResponse.ERRORS)
        : new DataMap();

    final Set<String> mergedKeys = new HashSet<>(inputResults.keySet());
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

    CheckedUtil.putWithoutChecking(convertedData, RESULTS, mergedResults);
    CheckedUtil.putWithoutChecking(convertedData, ERRORS, inputErrors);

    super.deserializeData(convertedData, keyType, keyParts, complexKeyType, version);
  }

  @Override
  protected EntityResponse<E> deserializeValue(Object valueData)
  {
    return new EntityResponse<>((DataMap) valueData, _entityType.getType());
  }

  private Class<EntityResponse<E>> getEntityResponseValueClass()
  {
    @SuppressWarnings("unchecked")
    final Class<EntityResponse<E>> valueClass = (Class<EntityResponse<E>>) (Object) EntityResponse.class;
    return valueClass;
  }
}
