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
package com.linkedin.restli.server.twitter;


import com.linkedin.parseq.promise.Promise;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.server.BatchCreateRequest;
import com.linkedin.restli.server.BatchCreateResult;
import com.linkedin.restli.server.BatchDeleteRequest;
import com.linkedin.restli.server.BatchPatchRequest;
import com.linkedin.restli.server.BatchUpdateRequest;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.ActionParam;
import com.linkedin.restli.server.annotations.Context;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.resources.KeyValueResource;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.DiscoveredItem;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.DiscoveredItemKey;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.DiscoveredItemKeyParams;

import java.util.List;
import java.util.Map;
import java.util.Set;


@RestLiCollection(name="promisediscovereditems", keyName="promiseDiscoveredItemId")
public class PromiseDiscoveredItemsResource
    implements KeyValueResource<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem>
{
  @Finder("user")
  public Promise<List<DiscoveredItem>> getDiscoveredItemsForUser(@QueryParam("userId") long userId,
                                                        @Context PagingContext pagingContext)
  {
    return null;
  }

  @RestMethod.Create
  public Promise<CreateResponse> create(DiscoveredItem entity)
  {
    return null;
  }

  @RestMethod.BatchGet
  public Promise<Map<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem>> batchGet(
      Set<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>> ids)
  {
    return null;
  }

  @RestMethod.Get
  public Promise<DiscoveredItem> get(ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> key)
  {
    return null;
  }

  @RestMethod.Delete
  public Promise<UpdateResponse> delete(ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> key)
  {
    return null;
  }

  @RestMethod.PartialUpdate
  public Promise<UpdateResponse> update(
      ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> key, PatchRequest<DiscoveredItem> request)
  {
    return null;
  }

  @RestMethod.Update
  public Promise<UpdateResponse> update(ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> key, DiscoveredItem entity)
  {
    return null;
  }

  @RestMethod.BatchUpdate
  public Promise<BatchUpdateResult<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem>> batchUpdate(
      BatchUpdateRequest<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem> entities)
  {
    return null;
  }

  @RestMethod.BatchPartialUpdate
  public Promise<BatchUpdateResult<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem>> batchUpdate(
      BatchPatchRequest<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem> entityUpdates)
  {
    return null;
  }

  @RestMethod.BatchCreate
  public Promise<BatchCreateResult<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem>> batchCreate(
      BatchCreateRequest<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem> entities)
  {
    return null;
  }

  @RestMethod.BatchDelete
  public Promise<BatchUpdateResult<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem>> batchDelete(
      BatchDeleteRequest<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem> ids)
  {
    return null;
  }

  @Action(name="purge")
  public void purge(@ActionParam("user") long userId)
  {
  }
}
