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
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.server.BatchDeleteRequest;
import com.linkedin.restli.server.BatchPatchRequest;
import com.linkedin.restli.server.BatchUpdateRequest;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.RoutingException;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.RestLiAssociation;
import com.linkedin.restli.server.annotations.RestLiTemplate;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * @deprecated Use {@link AssociationResourceTaskTemplate} instead.
 * Base template class for async Rest.li associations that return {@link Promise}s
 *
 * @author kparikh
 */
@Deprecated
@RestLiTemplate(expectedAnnotation = RestLiAssociation.class)
public class AssociationResourcePromiseTemplate<V extends RecordTemplate>
    extends ResourceContextHolder implements AssociationResourcePromise<V>
{
  @Override
  public Promise<Map<CompoundKey, V>> batchGet(Set<CompoundKey> ids)
  {
    throw new RoutingException("'batch_get' not implemented", 400);
  }

  @Override
  public Promise<V> get(CompoundKey key)
  {
    throw new RoutingException("'get' not implemented", 400);
  }

  @Override
  public Promise<UpdateResponse> update(CompoundKey key, V entity)
  {
    throw new RoutingException("'update' not implemented", 400);
  }

  @Override
  public Promise<UpdateResponse> update(CompoundKey key, PatchRequest<V> patch)
  {
    throw new RoutingException("'partial_update' not implemented", 400);
  }

  @Override
  public Promise<UpdateResponse> delete(CompoundKey key)
  {
    throw new RoutingException("'delete' not implemented", 400);
  }

  @Override
  public Promise<BatchUpdateResult<CompoundKey, V>> batchUpdate(BatchUpdateRequest<CompoundKey, V> entities)
  {
    throw new RoutingException("'batch_update' not implemented", 400);
  }

  @Override
  public Promise<BatchUpdateResult<CompoundKey, V>> batchUpdate(BatchPatchRequest<CompoundKey, V> patches)
  {
    throw new RoutingException("'batch_partial_update' not implemented", 400);
  }

  @Override
  public Promise<BatchUpdateResult<CompoundKey, V>> batchDelete(BatchDeleteRequest<CompoundKey, V> ids)
  {
    throw new RoutingException("'batch_delete' not implemented", 400);
  }

  @Override
  public Promise<List<V>> getAll(PagingContext pagingContext)
  {
    throw new RoutingException("'get_all' not implemented", 400);
  }
}
