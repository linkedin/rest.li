/*
   Copyright (c) 2012 LinkedIn Corp.

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

/**
 * $Id: $
 */

package com.linkedin.restli.internal.client;

import java.util.Map;

import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.common.ComplexKeySpec;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.CompoundKey.TypeInfo;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.TypeSpec;

/**
 * Converts a raw RestResponse into a type-bound batch response.
 * @author Steven Ihde
 * @version $Revision: $
 */

public class BatchKVResponseDecoder<K, V extends RecordTemplate> extends RestResponseDecoder<BatchKVResponse<K, V>>
{
  private final TypeSpec<V> _elementType;
  private final TypeSpec<K> _keyType;
  private final Map<String, CompoundKey.TypeInfo> _keyParts;
  private final ComplexKeySpec<?, ?> _complexKeyType;

  /**
   * @param elementClass provides the entity type of the collection.
   *
   * @param keyClass provides the class identifying the key type.
   *   <ul>
   *     <li>For collection resources must be a primitive or a typeref to a primitive.</li>
   *     <li>For an association resources must be {@link CompoundKey} and keyParts must contain an entry for each association key field.</li>
   *     <li>For complex resources must be {@link ComplexResourceKey}, keyKeyClass must contain the
   *           key's record template class and if the resource has a key params their record template type keyParamsClass must be provided.</li>
   * @param keyParts provides a map for association keys of each key name to {@link TypeInfo}, for non-association resources must be an empty map.
   * @param keyKeyClass provides the record template class for the key for complex key resources, otherwise null.
   * @param keyParamsClass provides the record template class for the key params for complex key resources, otherwise null.
   */
  public BatchKVResponseDecoder(Class<V> elementClass,
                                Class<K> keyClass,
                                Map<String, CompoundKey.TypeInfo> keyParts,
                                Class<? extends RecordTemplate> keyKeyClass,
                                Class<? extends RecordTemplate> keyParamsClass)
  {
    this(TypeSpec.forClassMaybeNull(elementClass),
         TypeSpec.forClassMaybeNull(keyClass),
         keyParts,
         ComplexKeySpec.forClassesMaybeNull(keyKeyClass, keyParamsClass));
  }

  /**
   * @param elementType provides the entity type of the collection.
   *
   * @param keyType provides the class identifying the key type.
   *   <ul>
   *     <li>For collection resources must be a primitive or a typeref to a primitive.</li>
   *     <li>For an association resources must be {@link CompoundKey} and keyParts must contain an entry for each association key field.</li>
   *     <li>For complex resources must be {@link ComplexResourceKey}, keyKeyClass must contain the
   *           key's record template class and if the resource has a key params their record template type keyParamsClass must be provided.</li>
   * @param keyParts provides a map for association keys of each key name to {@link TypeInfo}, for non-association resources must be an empty map.
   * @param complexKeyType provides the type of the key for complex key resources, otherwise null.
   */
  public BatchKVResponseDecoder(TypeSpec<V> elementType,
                                TypeSpec<K> keyType,
                                Map<String, CompoundKey.TypeInfo> keyParts,
                                ComplexKeySpec<?, ?> complexKeyType)
  {
    _elementType = elementType;
    _keyType = keyType;
    _keyParts = keyParts;
    _complexKeyType = complexKeyType;
  }

  @Override
  public Class<?> getEntityClass()
  {
    return _elementType.getType();
  }

  @Override
  public BatchKVResponse<K, V> wrapResponse(DataMap dataMap, Map<String, String> headers, ProtocolVersion version)
  {
    return dataMap == null ? null : new BatchKVResponse<>(dataMap,
        _keyType,
        _elementType,
        _keyParts,
        _complexKeyType,
        version);
  }
}
