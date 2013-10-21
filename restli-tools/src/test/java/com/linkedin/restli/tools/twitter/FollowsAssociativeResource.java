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

package com.linkedin.restli.tools.twitter;

import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.server.ResourceLevel;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.AssocKey;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.Key;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiAssociation;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.resources.AssociationResourceTemplate;
import com.linkedin.restli.tools.twitter.TwitterTestDataModels.Followed;
import com.linkedin.restli.tools.twitter.TwitterTestDataModels.Status;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Association resource for the 'following' relationship
 *
 * @author dellamag
 */
@RestLiAssociation(name="follows",
                   assocKeys={@Key(name="followerID", type=long.class),
                           @Key(name="followeeID", type=long.class)})
public class FollowsAssociativeResource extends AssociationResourceTemplate<Followed>
{
  /**
   * Gets a batch of Followed resources
   */
  @Override
  public Map<CompoundKey, Followed> batchGet(Set<CompoundKey> ids)
  {
    return null;
  }

  /**
   * Gets a single Followed resource
   */
  @Override
  public Followed get(CompoundKey key)
  {
    return null;
  }

  /**
   * Gets the friends of the given user
   *
   * @param userID the user who's friends we want to fetch
   */
  @Finder("friends")
  public List<Followed> getFriends(@QueryParam("userID") long userID)
  {
    return null;
  }

  /**
   * Gets the followers of the given user
   *
   * @param userID the user who's followers we want to fetch
   */
  @Finder("followers")
  public List<Followed> getFollowers(@QueryParam("userID") long userID)
  {
    return null;
  }

  /**
   * Test finder
   *
   * @param someParam some parameter
   */
  @Finder("other")
  public List<Followed> getOther(@AssocKey("followerID") long followerID,
                                 @QueryParam("someParam") String someParam)
  {
    return null;
  }

  @Action(name="entityAction", resourceLevel = ResourceLevel.ENTITY)
  public Status entityAction()
  {
    return null;
  }

  /**
   * Updates the given Followed relationship
   */
  @Override
  public UpdateResponse update(CompoundKey key, PatchRequest<Followed> request)
  {
    return null;
  }

}
