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

/**
 * Converts a raw RestResponse into a type-bound batch response.
 * @author Steven Ihde
 * @version $Revision: $
 */

public class BatchKVResponseDecoder<K, V extends RecordTemplate> extends RestResponseDecoder<BatchKVResponse<K, V>>
{
  private final Class<V> _elementClass;
  private final Class<K> _keyClass;
  private final Map<String, Class<?>> _keyParts;
  private final Class<? extends RecordTemplate> _keyKeyClass;
  private final Class<? extends RecordTemplate> _keyParamsClass;

  public BatchKVResponseDecoder(Class<V> elementClass,
                                Class<K> keyClass,
                                Map<String, Class<?>> keyParts,
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
