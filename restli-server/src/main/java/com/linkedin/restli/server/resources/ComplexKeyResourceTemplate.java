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
 *
 */
package com.linkedin.restli.server.resources;

import java.util.Map;
import java.util.Set;

import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.ComplexResourceKey;
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
 * @author adubman
 *
 */
public abstract class ComplexKeyResourceTemplate<K extends RecordTemplate, P extends RecordTemplate, V extends RecordTemplate> extends
    ResourceContextHolder implements ComplexKeyResource<K, P, V>
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
  public Map<ComplexResourceKey<K, P>, V> batchGet(final Set<ComplexResourceKey<K, P>> ids)
  {
    throw new RoutingException("'batch_get' not implemented", 400);
  }

  /**
   * @see CollectionResource#get(Object)
   */
  @Override
  public V get(final ComplexResourceKey<K, P> key)
  {
    throw new RoutingException("'get' not implemented", 400);
  }

  /**
   * @see CollectionResource#update
   */
  @Override
  public UpdateResponse update(final ComplexResourceKey<K, P> key, final V entity)
  {
    throw new RoutingException("'update' not implemented", 400);
  }

  /**
   * @see CollectionResource#update
   */
  @Override
  public UpdateResponse update(final ComplexResourceKey<K, P> key,
                               final PatchRequest<V> patch)
  {
    throw new RoutingException("'update' not implemented", 400);
  }

  /**
   * @see CollectionResource#delete(Object)
   */
  @Override
  public UpdateResponse delete(final ComplexResourceKey<K, P> key)
  {
    throw new RoutingException("'delete' not implemented", 400);
  }

  @Override
  public BatchUpdateResult<ComplexResourceKey<K, P>, V> batchUpdate(final BatchUpdateRequest<ComplexResourceKey<K, P>, V> entities)
  {
    throw new RoutingException("'batch_update' not implemented", 400);
  }

  @Override
  public BatchUpdateResult<ComplexResourceKey<K, P>, V> batchUpdate(final BatchPatchRequest<ComplexResourceKey<K, P>, V> patches)
  {
    throw new RoutingException("'batch_update' not implemented", 400);
  }

  @Override
  public BatchCreateResult<ComplexResourceKey<K, P>, V> batchCreate(final BatchCreateRequest<ComplexResourceKey<K, P>, V> entities)
  {
    throw new RoutingException("'batch_create' not implemented", 400);
  }

  @Override
  public BatchUpdateResult<ComplexResourceKey<K, P>, V> batchDelete(final BatchDeleteRequest<ComplexResourceKey<K, P>, V> ids)
  {
    throw new RoutingException("'batch_delete' not implemented", 400);
  }
}
