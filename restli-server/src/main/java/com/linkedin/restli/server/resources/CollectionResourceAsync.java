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

package com.linkedin.restli.server.resources;

import com.linkedin.restli.server.BatchCreateRequest;
import com.linkedin.restli.server.BatchCreateResult;
import com.linkedin.restli.server.BatchDeleteRequest;
import com.linkedin.restli.server.BatchPatchRequest;
import com.linkedin.restli.server.BatchUpdateRequest;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.PagingContext;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.linkedin.common.callback.Callback;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.UpdateResponse;

public interface CollectionResourceAsync<K, V extends RecordTemplate> extends
    BaseResource,
    KeyValueResource<K, V>
{

  void get(K key, Callback<V> callback);

  void create(V entity, Callback<CreateResponse> callback);

  void batchGet(Set<K> ids, Callback<Map<K, V>> callback);

  void batchUpdate(BatchUpdateRequest<K, V> entities, Callback<BatchUpdateResult<K, V>> callback);

  void batchUpdate(BatchPatchRequest<K, V> patches, Callback<BatchUpdateResult<K, V>> callback);

  void batchCreate(BatchCreateRequest<K, V> entities, Callback<BatchCreateResult<K, V>> callback);

  void batchDelete(BatchDeleteRequest<K, V> ids, Callback<BatchUpdateResult<K, V>> callback);

  void update(K key, V entity, Callback<UpdateResponse> callback);

  void update(K key, PatchRequest<V> patch, Callback<UpdateResponse> callback);

  void delete(K key, Callback<UpdateResponse> callback);

  void getAll(PagingContext ctx, Callback<List<V>> callback);

}
