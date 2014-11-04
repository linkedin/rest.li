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

package com.linkedin.restli.server.resources;


import com.linkedin.data.template.RecordTemplate;
import com.linkedin.parseq.promise.Promise;
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


/**
 * Base template class for async Rest.li collections that return {@link Promise}s.
 *
 * @author kparikh
 */
@RestLiTemplate(expectedAnnotation = RestLiCollection.class)
public class CollectionResourcePromiseTemplate<K, V extends RecordTemplate>
    extends ResourceContextHolder
    implements CollectionResourcePromise<K, V>
{
  @Override
  public Promise<CreateResponse> create(V entity)
  {
    throw new RoutingException("'create' not implemented", 400);
  }

  @Override
  public Promise<Map<K, V>> batchGet(Set<K> ids)
  {
    throw new RoutingException("'batch_get' not implemented", 400);
  }

  @Override
  public Promise<BatchUpdateResult<K, V>> batchUpdate(BatchUpdateRequest<K, V> entities)
  {
    throw new RoutingException("'batch_update' not implemented", 400);
  }

  @Override
  public Promise<BatchUpdateResult<K, V>> batchUpdate(BatchPatchRequest<K, V> patches)
  {
    throw new RoutingException("'batch_partial_update' not implemented", 400);
  }

  @Override
  public Promise<BatchCreateResult<K, V>> batchCreate(BatchCreateRequest<K, V> entities)
  {
    throw new RoutingException("'batch_create' not implemented", 400);
  }

  @Override
  public Promise<BatchUpdateResult<K, V>> batchDelete(BatchDeleteRequest<K, V> ids)
  {
    throw new RoutingException("'batch_delete' not implemented", 400);
  }

  @Override
  public Promise<V> get(K key)
  {
    throw new RoutingException("'get' not implemented", 400);
  }

  @Override
  public Promise<UpdateResponse> update(K key, V entity)
  {
    throw new RoutingException("'update' not implemented", 400);
  }

  @Override
  public Promise<UpdateResponse> update(K key, PatchRequest<V> patch)
  {
    throw new RoutingException("'partial_update' not implemented", 400);
  }

  @Override
  public Promise<UpdateResponse> delete(K key)
  {
    throw new RoutingException("'delete' not implemented", 400);
  }

  @Override
  public Promise<List<V>> getAll(PagingContext pagingContext)
  {
    throw new RoutingException("'get_all' not implemented", 400);
  }
}
