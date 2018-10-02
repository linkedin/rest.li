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

package com.linkedin.restli.client;


import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.CollectionRequest;
import com.linkedin.restli.common.KeyValueRecord;
import com.linkedin.restli.common.KeyValueRecordFactory;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.RestConstants;

import java.util.Collections;
import java.util.Map;


/**
 * @author Keren Jin
 */
public abstract class BatchKVRequestBuilder<K, V extends RecordTemplate, R extends BatchRequest<?>> extends
  RestfulRequestBuilder<K, V, R>
{
  protected BatchKVRequestBuilder(String baseURITemplate, ResourceSpec resourceSpec, RestliRequestOptions requestOptions)
  {
    super(baseURITemplate, resourceSpec, requestOptions);
  }

  protected void ensureBatchKeys()
  {
    if (!hasParam(RestConstants.QUERY_BATCH_IDS_PARAM))
    {
      addKeys(Collections.emptySet());
    }
  }

  protected <E extends RecordTemplate> CollectionRequest<KeyValueRecord<K, E>> buildReadOnlyInput(
      Map<K, E> readOnlyInputEntities, Map<K, E> inputMap, KeyValueRecordFactory<K, E> keyValueRecordFactory)
  {
    try
    {
      DataMap map = new DataMap();
      @SuppressWarnings({ "unchecked", "rawtypes" })
      CollectionRequest<KeyValueRecord<K, E>> input = new CollectionRequest(map, KeyValueRecord.class);

      for (Map.Entry<K, E> inputEntityEntry : inputMap.entrySet())
      {
        K key = getReadOnlyOrCopyKey(inputEntityEntry.getKey());
        E entity = getReadOnlyOrCopyDataTemplate(inputEntityEntry.getValue());
        readOnlyInputEntities.put(key, entity);
        KeyValueRecord<K, E> keyValueRecord = keyValueRecordFactory.create(key, entity);
        keyValueRecord.data().setReadOnly();
        input.getElements().add(keyValueRecord);
      }

      map.setReadOnly();
      return input;
    }
    catch (CloneNotSupportedException cloneException)
    {
      throw new IllegalArgumentException("Entity cannot be copied.", cloneException);
    }
  }
}
