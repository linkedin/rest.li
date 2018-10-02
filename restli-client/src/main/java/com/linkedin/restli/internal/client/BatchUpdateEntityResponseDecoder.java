/*
   Copyright (c) 2018 LinkedIn Corp.

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
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.common.ComplexKeySpec;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.TypeSpec;
import com.linkedin.restli.common.UpdateEntityStatus;
import com.linkedin.restli.internal.client.response.BatchUpdateEntityResponse;
import java.util.Map;


/**
 * Converts a raw batch update response {@link DataMap} into a {@link BatchUpdateEntityResponse} containing
 * the returned entities.
 *
 * @author Evan Williams
 */
public class BatchUpdateEntityResponseDecoder<K, V extends RecordTemplate>
    extends RestResponseDecoder<BatchKVResponse<K, UpdateEntityStatus<V>>>
{
  private final TypeSpec<V> _entityType;
  private final TypeSpec<K> _keyType;
  private final Map<String, CompoundKey.TypeInfo> _keyParts;
  private final ComplexKeySpec<?, ?> _complexKeyType;

  /**
   * @param entityType provides the class identifying the entity type.
   * @param keyType provides the class identifying the key type.
   *   <ul>
   *     <li>For collection resources must be a primitive or a typeref to a primitive.</li>
   *     <li>For an association resources must be {@link CompoundKey} and keyParts must contain an entry for each association key field.</li>
   *     <li>For complex resources must be {@link com.linkedin.restli.common.ComplexResourceKey}, keyKeyClass must contain the
   *           key's record template class and if the resource has a key params their record template type keyParamsClass must be provided.</li>
   *   </ul>
   * @param keyParts provides a map for association keys of each key name to {@link CompoundKey.TypeInfo}, for non-association resources must be an empty map.
   * @param complexKeyType provides the type of the key for complex key resources, otherwise null.
   */
  public BatchUpdateEntityResponseDecoder(TypeSpec<V> entityType, TypeSpec<K> keyType, Map<String, CompoundKey.TypeInfo> keyParts, ComplexKeySpec<?, ?> complexKeyType)
  {
    _entityType = entityType;
    _keyType = keyType;
    _keyParts = keyParts;
    _complexKeyType = complexKeyType;
  }

  @Override
  @SuppressWarnings({"unchecked"})
  public Class<?> getEntityClass()
  {
    return _entityType.getType();
  }

  @Override
  @SuppressWarnings({"unchecked"})
  public BatchKVResponse<K, UpdateEntityStatus<V>> wrapResponse(DataMap dataMap, Map<String, String> headers, ProtocolVersion version)
  {
    if (dataMap == null)
    {
      return null;
    }

    DataMap responseData = ResponseDecoderUtil.mergeUpdateStatusResponseData(dataMap);

    return new BatchUpdateEntityResponse<>(responseData, _keyType, _entityType, _keyParts, _complexKeyType, version);
  }
}
