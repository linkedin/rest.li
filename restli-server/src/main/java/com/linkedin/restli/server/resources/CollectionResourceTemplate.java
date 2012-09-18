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

import java.util.Map;
import java.util.Set;

import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.server.BatchCreateRequest;
import com.linkedin.restli.server.BatchCreateResult;
import com.linkedin.restli.server.BatchDeleteRequest;
import com.linkedin.restli.server.BatchPatchRequest;
import com.linkedin.restli.server.BatchUpdateRequest;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.RoutingException;
import com.linkedin.restli.server.UpdateResponse;



/**
 * Base {@link CollectionResource} implementation. All implementations should extend this
 *
 * @author dellamag
 */
public class CollectionResourceTemplate<K, V extends RecordTemplate> extends
    ResourceContextHolder implements CollectionResource<K, V>
{
  /**
   * @see com.linkedin.restli.server.resources.CollectionResource#create(com.linkedin.data.template.RecordTemplate)
   */
  @Override
  public CreateResponse create(final V entity)
  {
    throw new RoutingException("'create' not implemented", 400);
  }

  /**
   * @see com.linkedin.restli.server.resources.CollectionResource#batchGet(java.util.Set)
   */
  @Override
  public Map<K, V> batchGet(final Set<K> ids)
  {
    throw new RoutingException("'batch_get' not implemented", 400);
  }

  @Override
  public BatchUpdateResult<K, V> batchUpdate(final BatchUpdateRequest<K, V> entities)
  {
    throw new RoutingException("'batch_update' not implemented", 400);
  }

  @Override
  public BatchUpdateResult<K, V> batchUpdate(final BatchPatchRequest<K, V> entityUpdates)
  {
    throw new RoutingException("'batch_patch' not implemented", 400);
  }

  @Override
  public BatchCreateResult<K, V> batchCreate(final BatchCreateRequest<K, V> entities)
  {
    throw new RoutingException("'batch_create' not implemented", 400);
  }

  @Override
  public BatchUpdateResult<K, V> batchDelete(final BatchDeleteRequest<K, V> ids)
  {
    throw new RoutingException("'batch_delete' not implemented", 400);
  }

  /**
   * @see CollectionResource#get(Object)
   */
  @Override
  public V get(final K key)
  {
    throw new RoutingException("'get' not implemented", 400);
  }

  /**
   * @see CollectionResource#update
   */
  @Override
  public UpdateResponse update(final K key, final V entity)
  {
    throw new RoutingException("'update' not implemented", 400);
  }

  /**
   * @see CollectionResource#update
   */
  @Override
  public UpdateResponse update(final K key, final PatchRequest<V> patch)
  {
    throw new RoutingException("'update' not implemented", 400);
  }

  /**
   * @see CollectionResource#delete(Object)
   */
  @Override
  public UpdateResponse delete(final K key)
  {
    throw new RoutingException("'delete' not implemented", 400);
  }

}
