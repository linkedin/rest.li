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

import com.linkedin.restli.server.BatchCreateRequest;
import com.linkedin.restli.server.BatchCreateResult;
import com.linkedin.restli.server.BatchDeleteRequest;
import com.linkedin.restli.server.BatchPatchRequest;
import com.linkedin.restli.server.BatchUpdateRequest;
import com.linkedin.restli.server.BatchUpdateResult;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.linkedin.common.callback.Callback;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.CallbackParam;
import com.linkedin.restli.server.annotations.PagingContextParam;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.ComplexKeyResourceAsyncTemplate;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.DiscoveredItem;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.DiscoveredItemKey;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.DiscoveredItemKeyParams;


@RestLiCollection(name="asyncdiscovereditems",
                  keyName="asyncDiscoveredItemId")
public class AsyncDiscoveredItemsResource
    extends ComplexKeyResourceAsyncTemplate<DiscoveredItemKey, DiscoveredItemKeyParams, DiscoveredItem>
{
  @Finder("user")
  public void getDiscoveredItemsForUser(@PagingContextParam PagingContext pagingContext,
                                         @QueryParam("userId") long userId,
                                         @CallbackParam final Callback<List<DiscoveredItem>> callback)
  {
    callback.onSuccess(null);
  }

  @Override
  public void create(final DiscoveredItem entity,
                     @CallbackParam final Callback<CreateResponse> callback)
  {
    callback.onSuccess(null);
  }

  @Override
  public void batchGet(Set<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>> ids,
                       @CallbackParam Callback<Map<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem>> callback)
  {
    callback.onSuccess(null);
  }

  @Override
  public void get(final ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> key,
                  @CallbackParam final Callback<DiscoveredItem> callback)
  {
    callback.onSuccess(null);
  }

  @Override
  public void delete(final ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> key,
                     @CallbackParam final Callback<UpdateResponse> callback)
  {
    callback.onSuccess(null);
  }

  @Override
  public void update(final ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> key,
                     final PatchRequest<DiscoveredItem> request,
                     @CallbackParam final Callback<UpdateResponse> callback)
  {
    callback.onSuccess(null);
  }

  @Override
  public void update(final ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> key,
                     final DiscoveredItem entity,
                     @CallbackParam final Callback<UpdateResponse> callback)
  {
    callback.onSuccess(null);
  }

  @Override
  public void batchCreate(BatchCreateRequest<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem> entities,
                          @CallbackParam Callback<BatchCreateResult<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem>> callback)
  {
    callback.onSuccess(null);
  }

  @Override
  public void batchUpdate(BatchUpdateRequest<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem> entities,
                          @CallbackParam Callback<BatchUpdateResult<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem>> callback)
  {
    callback.onSuccess(null);
  }

  @Override
  public void batchUpdate(BatchPatchRequest<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem> entities,
                          @CallbackParam Callback<BatchUpdateResult<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem>> callback)
  {
    callback.onSuccess(null);
  }

  @Override
  public void batchDelete(BatchDeleteRequest<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem> ids,
                          @CallbackParam Callback<BatchUpdateResult<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem>> callback)
  {
    callback.onSuccess(null);
  }

  @Override
  public void getAll(@PagingContextParam PagingContext ctx, @CallbackParam Callback<List<DiscoveredItem>> callback)
  {
    callback.onSuccess(null);
  }
}
