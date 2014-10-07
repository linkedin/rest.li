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


import com.linkedin.common.callback.Callback;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.server.BatchDeleteRequest;
import com.linkedin.restli.server.BatchPatchRequest;
import com.linkedin.restli.server.BatchUpdateRequest;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.AssocKeyParam;
import com.linkedin.restli.server.annotations.CallbackParam;
import com.linkedin.restli.server.annotations.PagingContextParam;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.Key;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiAssociation;
import com.linkedin.restli.server.resources.AssociationResourceAsyncTemplate;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.Followed;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Association resource for the 'following' relationship.
 *
 * @author femekci
 */
@RestLiAssociation(name = "asyncfollows", assocKeys = {
    @Key(name = "followerID", type = long.class),
    @Key(name = "followeeID", type = long.class) })
public class AsyncFollowsAssociativeResource extends
    AssociationResourceAsyncTemplate<Followed>
{
  /**
   * Gets a batch of Followed resources
   */
  @Override
  public void batchGet(final Set<CompoundKey> ids,
                       @CallbackParam final Callback<Map<CompoundKey, Followed>> callback)
  {
    callback.onSuccess(null);
  }

  /**
   * Gets a single Followed resource
   */
  @Override
  public void get(final CompoundKey key, @CallbackParam final Callback<Followed> callback)
  {
    callback.onSuccess(null);
  }

  /**
   * Gets the friends of the given user
   *
   * @param userID the user who's friends we want to fetch
   */
  @Finder("friends")
  public void getFriends(@QueryParam("userID") final long userID,
                         @CallbackParam final Callback<List<Followed>> callback)
  {
    callback.onSuccess(null);
  }

  /**
   * Gets the followers of the given user
   *
   * @param userID the user who's followers we want to fetch
   */
  @Finder("followers")
  public void getFollowers(@QueryParam("userID") final long userID,
                           @CallbackParam final Callback<List<Followed>> callback)
  {
    callback.onSuccess(null);
  }

  /**
   * Test finder
   *
   * @param someParam some parameter
   */
  @Finder("other")
  public void getOther(@AssocKeyParam("followerID") final long followerID,
                       @QueryParam("someParam") final String someParam,
                       @CallbackParam final Callback<List<Followed>> callback)
  {
    callback.onSuccess(null);
  }

  /**
   * Partially updates the given Followed relationship
   */
  @Override
  public void update(final CompoundKey key,
                     final PatchRequest<Followed> request,
                     @CallbackParam final Callback<UpdateResponse> callback)
  {
    callback.onSuccess(null);
  }

  /**
   * Updates (overwrites) the given Followed relationship
   */
  @Override
  public void update(final CompoundKey key,
                     final Followed entity,
                     @CallbackParam final Callback<UpdateResponse> callback)
  {
    callback.onSuccess(null);
  }

  @Override
  public void batchDelete(BatchDeleteRequest<CompoundKey, Followed> ids,
                          @CallbackParam Callback<BatchUpdateResult<CompoundKey, Followed>> callback)
  {
    callback.onSuccess(null);
  }

  @Override
  public void batchUpdate(BatchUpdateRequest<CompoundKey, Followed> entities,
                          @CallbackParam Callback<BatchUpdateResult<CompoundKey, Followed>> callback)
  {
    callback.onSuccess(null);
  }

  @Override
  public void batchUpdate(BatchPatchRequest<CompoundKey, Followed> patches,
                          @CallbackParam Callback<BatchUpdateResult<CompoundKey, Followed>> callback)
  {
    callback.onSuccess(null);
  }

  @Override
  public void getAll(@PagingContextParam PagingContext ctx, @CallbackParam Callback<List<Followed>> callback)
  {
    callback.onSuccess(null);
  }
}
