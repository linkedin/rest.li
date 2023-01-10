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

import com.linkedin.parseq.promise.Promise;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.server.BatchFinderResult;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.ResourceLevel;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.ActionParam;
import com.linkedin.restli.server.annotations.AlternativeKey;
import com.linkedin.restli.server.annotations.AlternativeKeys;
import com.linkedin.restli.server.annotations.BatchFinder;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.PagingContextParam;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.tools.twitter.TwitterTestDataModels.Status;
import com.linkedin.restli.tools.twitter.TwitterTestDataModels.User;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CollectionResource containing all statuses modeled using promise template.
 */
@RestLiCollection(name="statusPromises",
    keyName="statusID")
@AlternativeKeys(alternativeKeys = {@AlternativeKey(name="alt", keyCoercer=StringLongCoercer.class, keyType=String.class),
    @AlternativeKey(name="newAlt", keyCoercer=StringLongCoercer.class, keyType=String.class)})
@SuppressWarnings("deprecation")
public class StatusPromiseResource extends com.linkedin.restli.server.resources.CollectionResourcePromiseTemplate<Long,Status>
{
  /**
   * Gets the status timeline for a given user
   */
  @Finder("user_timeline")
  public Promise<List<Status>> getUserTimeline(@PagingContextParam PagingContext pagingContext)
  {
    return null;
  }

  /**
   * Batch finder for statuses
   */
  @BatchFinder(value="batchFinderByAction",  batchParam="criteria")
  public Promise<BatchFinderResult<Status, Status, User>> batchFindStatuses(@QueryParam("criteria") Status[] criteria)
  {
    return null;
  }

  /**
   * Creates a new Status
   */
  @Override
  public Promise<CreateResponse> create(Status entity) {
    return null;
  }

  /**
   * Gets a batch of statuses
   */
  @Override
  public Promise<Map<Long, Status>> batchGet(Set<Long> ids)
  {
    return null;
  }

  /**
   * Gets a single status resource
   */
  @Override
  public Promise<Status> get(Long key)
  {
    return null;
  }

  /**
   * Gets all the resources
   */
  @Override
  public Promise<List<Status>> getAll(@PagingContextParam PagingContext pagingContext)
  {
    return null;
  }

  /**
   * Deletes a status resource
   */
  @Override
  public Promise<UpdateResponse> delete(Long key)
  {
    return null;
  }

  /**
   * Updates a single status resource
   */
  @Override
  public Promise<UpdateResponse> update(Long key, PatchRequest<Status> request)
  {
    return null;
  }

  /**
   * Ambiguous action binding test case
   */
  @Action(name="forward",
      resourceLevel= ResourceLevel.ENTITY)
  public Promise<String> forward(@ActionParam("to") long userID)
  {
    return null;
  }
}
