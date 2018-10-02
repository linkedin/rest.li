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
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.ComplexKeySpec;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.TypeSpec;
import com.linkedin.restli.common.UpdateStatus;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Converts a raw batch update response {@link DataMap} into a {@link BatchKVResponse}.
 *
 * @author Keren Jin
 */
public class BatchUpdateResponseDecoder<K> extends RestResponseDecoder<BatchKVResponse<K, UpdateStatus>>
{
  private final TypeSpec<K> _keyType;
  private final Map<String, CompoundKey.TypeInfo> _keyParts;
  private final ComplexKeySpec<?, ?> _complexKeyType;

  /**
   * @param keyType provides the class identifying the key type.
   *   <ul>
   *     <li>For collection resources must be a primitive or a typeref to a primitive.</li>
   *     <li>For an association resources must be {@link com.linkedin.restli.common.CompoundKey} and keyParts must contain an entry for each association key field.</li>
   *     <li>For complex resources must be {@link com.linkedin.restli.common.ComplexResourceKey}, keyKeyClass must contain the
   *           key's record template class and if the resource has a key params their record template type keyParamsClass must be provided.</li>
   * @param keyParts provides a map for association keys of each key name to {@link com.linkedin.restli.common.CompoundKey.TypeInfo}, for non-association resources must be an empty map.
   * @param complexKeyType provides the type of the key for complex key resources, otherwise null.
   */
  public BatchUpdateResponseDecoder(TypeSpec<K> keyType, Map<String, CompoundKey.TypeInfo> keyParts, ComplexKeySpec<?, ?> complexKeyType)
  {
    _keyType = keyType;
    _keyParts = keyParts;
    _complexKeyType = complexKeyType;
  }

  @Override
  public Class<?> getEntityClass()
  {
    return UpdateStatus.class;
  }

  @Override
  public BatchKVResponse<K, UpdateStatus> wrapResponse(DataMap dataMap, Map<String, String> headers, ProtocolVersion version)
  {
    if (dataMap == null)
    {
      return null;
    }

    DataMap responseData = buildBatchUpdateResponseData(dataMap);

    return new BatchKVResponse<>(responseData, _keyType, new TypeSpec<>(UpdateStatus.class), _keyParts, _complexKeyType, version);
  }

  /**
   * Helper method to assist BATCH_UPDATE, BATCH_PARTIAL_UPDATE, and BATCH_DELETE response decoders in transforming the
   * raw payload data map received over-the-wire to a data map suitable for instantiation of a {@link BatchKVResponse}.
   * @param dataMap received in the response payload
   * @return data map suitable for {@link BatchKVResponse}
   */
  static DataMap buildBatchUpdateResponseData(DataMap dataMap)
  {
    final DataMap mergedResults = new DataMap();
    final DataMap inputResults = dataMap.containsKey(BatchResponse.RESULTS) ? dataMap.getDataMap(BatchResponse.RESULTS)
        : new DataMap();
    final DataMap inputErrors = dataMap.containsKey(BatchResponse.ERRORS) ? dataMap.getDataMap(BatchResponse.ERRORS)
        : new DataMap();

    final Set<String> mergedKeys = new HashSet<>(inputResults.keySet());
    mergedKeys.addAll(inputErrors.keySet());

    for (String key : mergedKeys)
    {
      // DataMap for UpdateStatus
      final DataMap updateData;

      // status field is mandatory
      if (inputResults.containsKey(key))
      {
        updateData = inputResults.getDataMap(key);
      }
      else
      {
        updateData = new DataMap();
      }

      // DataMap for ErrorResponse
      final DataMap errorData = (DataMap) inputErrors.get(key);
      if (errorData != null)
      {
        // The status from ErrorResponse overwrites the one in UpdateResponse. However, results and
        // errors are not expected to have overlapping key. See BatchUpdateResponseBuilder.
        updateData.put("status", errorData.get("status"));
        updateData.put("error", errorData);
      }

      mergedResults.put(key, updateData);
    }

    final DataMap responseData = new DataMap();
    responseData.put(BatchKVResponse.RESULTS, mergedResults);
    responseData.put(BatchKVResponse.ERRORS, inputErrors);

    return responseData;
  }
}
