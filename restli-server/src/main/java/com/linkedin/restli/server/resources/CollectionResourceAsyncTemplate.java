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


import com.linkedin.common.callback.Callback;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.server.BatchCreateRequest;
import com.linkedin.restli.server.BatchCreateResult;
import com.linkedin.restli.server.BatchDeleteRequest;
import com.linkedin.restli.server.BatchPatchRequest;
import com.linkedin.restli.server.BatchUpdateRequest;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.RoutingException;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestLiTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;


@RestLiTemplate(expectedAnnotation = RestLiCollection.class)
public class CollectionResourceAsyncTemplate<K, V extends RecordTemplate> extends
    ResourceContextHolder implements CollectionResourceAsync<K, V>
{
  @Override
  public void get(final K key, final Callback<V> callback)
  {
    throw new RoutingException("'get' not implemented", 400);

  }

  @Override
  public void create(final V entity, final Callback<CreateResponse> callback)
  {
    throw new RoutingException("'create' not implemented", 400);
  }

  @Override
  public void batchGet(final Set<K> ids, final Callback<Map<K, V>> callback)
  {
    throw new RoutingException("'batch_get' not implemented", 400);
  }

  @Override
  public void batchUpdate(BatchUpdateRequest<K, V> entities, Callback<BatchUpdateResult<K, V>> callback)
  {
    throw new RoutingException("'batch_update' not implemented", 400);
  }

  @Override
  public void batchUpdate(BatchPatchRequest<K, V> patches, Callback<BatchUpdateResult<K, V>> callback)
  {
    throw new RoutingException("'batch_patch' not implemented", 400);
  }

  @Override
  public void batchCreate(BatchCreateRequest<K, V> entities, Callback<BatchCreateResult<K, V>> callback)
  {
    throw new RoutingException("'batch_create' not implemented", 400);
  }

  @Override
  public void batchDelete(BatchDeleteRequest<K, V> ids, Callback<BatchUpdateResult<K, V>> callback)
  {
    throw new RoutingException("'batch_delete' not implemented", 400);
  }

  @Override
  public void update(final K key, final V entity, final Callback<UpdateResponse> callback)
  {
    throw new RoutingException("'update' not implemented", 400);
  }

  @Override
  public void update(final K key,
                     final PatchRequest<V> patch,
                     final Callback<UpdateResponse> callback)
  {
    throw new RoutingException("'update' not implemented", 400);
  }

  @Override
  public void delete(final K key, final Callback<UpdateResponse> callback)
  {
    throw new RoutingException("'delete' not implemented", 400);
  }

  @Override
  public void getAll(PagingContext ctx, Callback<List<V>> callback)
  {
    throw new RoutingException("'get_all' not implemented", 400);
  }
}
