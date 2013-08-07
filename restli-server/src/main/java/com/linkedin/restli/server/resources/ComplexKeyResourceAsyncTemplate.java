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
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.RoutingException;
import com.linkedin.restli.server.UpdateResponse;


public class ComplexKeyResourceAsyncTemplate<K extends RecordTemplate, P extends RecordTemplate, V extends RecordTemplate> extends
    ResourceContextHolder implements ComplexKeyResourceAsync<K, P, V>
{
  @Override
  public void get(final ComplexResourceKey<K, P> key, final Callback<V> callback)
  {
    throw new RoutingException("'get' not implemented", 400);

  }

  @Override
  public void create(final V entity, final Callback<CreateResponse> callback)
  {
    throw new RoutingException("'create' not implemented", 400);
  }

  @Override
  public void batchGet(final Set<ComplexResourceKey<K, P>> ids, final Callback<Map<ComplexResourceKey<K, P>, V>> callback)
  {
    throw new RoutingException("'batch_get' not implemented", 400);
  }

  @Override
  public void batchCreate(BatchCreateRequest<ComplexResourceKey<K, P>, V> entities,
                          Callback<BatchCreateResult<ComplexResourceKey<K, P>, V>> callback)
  {
    throw new RoutingException("'batch_create' not implemented", 400);
  }

  @Override
  public void batchUpdate(BatchUpdateRequest<ComplexResourceKey<K, P>, V> entities,
                          Callback<BatchUpdateResult<ComplexResourceKey<K, P>, V>> callback)
  {
    throw new RoutingException("'batch_update' not implemented", 400);
  }

  @Override
  public void batchUpdate(BatchPatchRequest<ComplexResourceKey<K, P>, V> entities,
                          Callback<BatchUpdateResult<ComplexResourceKey<K, P>, V>> callback)
  {
    throw new RoutingException("'batch_patch' not implemented", 400);
  }

  @Override
  public void batchDelete(BatchDeleteRequest<ComplexResourceKey<K, P>, V> ids,
                          Callback<BatchUpdateResult<ComplexResourceKey<K, P>, V>> callback)
  {
    throw new RoutingException("'batch_delete' not implemented", 400);
  }

  @Override
  public void getAll(PagingContext ctx, Callback<List<V>> callback)
  {
    throw new RoutingException("'get_all' not implemented", 400);
  }

  @Override
  public void update(final ComplexResourceKey<K, P> key, final V entity, final Callback<UpdateResponse> callback)
  {
    throw new RoutingException("'update' not implemented", 400);
  }

  @Override
  public void update(final ComplexResourceKey<K, P> key,
                     final PatchRequest<V> patch,
                     final Callback<UpdateResponse> callback)
  {
    throw new RoutingException("'update' not implemented", 400);
  }

  @Override
  public void delete(final ComplexResourceKey<K, P> key, final Callback<UpdateResponse> callback)
  {
    throw new RoutingException("'delete' not implemented", 400);
  }
}
