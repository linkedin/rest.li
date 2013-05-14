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
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.CompoundKey.TypeInfo;

/**
 * Converts a raw RestResponse into a type-bound batch response.
 * @author Steven Ihde
 * @version $Revision: $
 */

public class BatchKVResponseDecoder<K, V extends RecordTemplate> extends RestResponseDecoder<BatchKVResponse<K, V>>
{
  private final Class<V> _elementClass;
  private final Class<K> _keyClass;
  private final Map<String, CompoundKey.TypeInfo> _keyParts;
  private final Class<? extends RecordTemplate> _keyKeyClass;
  private final Class<? extends RecordTemplate> _keyParamsClass;

  /**
   *
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
    _elementClass = elementClass;
    _keyClass = keyClass;
    _keyParts = keyParts;
    _keyKeyClass = keyKeyClass;
    _keyParamsClass = keyParamsClass;
  }

  @Override
  public Class<?> getEntityClass()
  {
    return _elementClass;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected BatchKVResponse<K, V> wrapResponse(DataMap dataMap)
  {
     return new BatchKVResponse<K, V>(dataMap,
                                     _keyClass,
                                     _elementClass,
                                     _keyParts,
                                     _keyKeyClass,
                                     _keyParamsClass);
  }
}
