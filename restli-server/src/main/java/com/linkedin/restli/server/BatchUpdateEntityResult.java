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

package com.linkedin.restli.server;

import com.linkedin.data.template.RecordTemplate;
import java.util.Collections;
import java.util.Map;


/**
 * Class returned by BATCH_PARTIAL_UPDATE resource methods that support returning the patched entities. It's a very
 * thin wrapper around {@link BatchUpdateResult} because the primary function is simply to let the response builder
 * know whether the entities are being returned or not.
 *
 * @param <K> - the key type of the resource. When using {@link com.linkedin.restli.common.ComplexResourceKey}, K should
 *              be the entire {@code ComplexResourceKey} and not just the Key part of the complex key.
 * @param <V> - the value type of the resource.
 *
 * @author Evan Williams
 */
public class BatchUpdateEntityResult<K, V extends RecordTemplate> extends BatchUpdateResult<K, V>
{
  public BatchUpdateEntityResult(final Map<K, UpdateEntityResponse<V>> results)
  {
    this(results, Collections.emptyMap());
  }

  /**
   * Constructs a <code>BatchUpdateEntityResult</code> with the given results and errors. It is expected
   * that, if a <code>RestLiServiceException</code> is provided for a given key in the errors map,
   * no <code>UpdateEntityResponse</code> should be provided for the same key in the results map. In case
   * both an <code>UpdateEntityResponse</code> and a <code>RestLiServiceException</code> are provided for
   * the same key, the <code>RestLiServiceException</code> takes precedence.
   */
  @SuppressWarnings("unchecked")
  public BatchUpdateEntityResult(final Map<K, UpdateEntityResponse<V>> results,
                           final Map<K, RestLiServiceException> errors) {
    super((Map<K, UpdateResponse>) (Map<K, ? extends UpdateResponse>) results, errors);
  }
}
