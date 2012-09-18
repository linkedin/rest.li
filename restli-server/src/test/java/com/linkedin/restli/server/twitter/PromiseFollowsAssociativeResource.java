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

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.linkedin.parseq.promise.Promise;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.AssocKey;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.Key;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiAssociation;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.resources.KeyValueResource;
import com.linkedin.restli.server.resources.ResourceContextHolder;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.Followed;

/**
 * Association resource for the 'following' relationship
 *
 * @author dellamag
 */
@RestLiAssociation(
             name="promisefollows",
             assocKeys={@Key(name="followerID", type=long.class),
                        @Key(name="followeeID", type=long.class)})
public class PromiseFollowsAssociativeResource extends ResourceContextHolder implements KeyValueResource<CompoundKey, Followed>
{
  /**
   * Gets a batch of Followed resources
   */
  @RestMethod.BatchGet
  public Promise<Map<CompoundKey, Followed>> batchGet(Set<CompoundKey> ids)
  {
    throw new AssertionError("should be mocked");
  }

  /**
   * Gets a single Followed resource
   */
  @RestMethod.Get
  public Promise<Followed> get(CompoundKey key)
  {
    throw new AssertionError("should be mocked");
  }

  /**
   * Gets the friends of the given user
   *
   * @param userID the user who's friends we want to fetch
   */
  @Finder("friends")
  public Promise<List<Followed>> getFriends(@QueryParam("userID") long userID)
  {
    throw new AssertionError("should be mocked");
  }

  /**
   * Gets the followers of the given user
   *
   * @param userID the user who's followers we want to fetch
   */
  @Finder("followers")
  public Promise<List<Followed>> getFollowers(@QueryParam("userID") long userID)
  {
    throw new AssertionError("should be mocked");
  }

  /**
   * Test finder
   *
   * @param someParam some parameter
   */
  @Finder("other")
  public Promise<List<Followed>> getOther(@AssocKey("followerID") long followerID,
                                 @QueryParam("someParam") String someParam)
  {
    throw new AssertionError("should be mocked");
  }


  /**
   * Partially updates the given Followed relationship
   */
  @RestMethod.PartialUpdate
  public Promise<UpdateResponse> update(CompoundKey key, PatchRequest<Followed> request)
  {
    throw new AssertionError("should be mocked");
  }

  /**
   * Updates (overwrites) the given Followed relationship
   */
  @RestMethod.Update
  public Promise<UpdateResponse> update(CompoundKey key, Followed entity)
  {
    throw new AssertionError("should be mocked");
  }
}
