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

import com.linkedin.parseq.Task;
import com.linkedin.restli.server.ActionResult;
import com.linkedin.restli.server.BatchFinderResult;
import com.linkedin.restli.server.BatchResult;
import com.linkedin.restli.server.CollectionResult;
import com.linkedin.restli.server.GetResult;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.ResourceLevel;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.ActionParam;
import com.linkedin.restli.server.annotations.AlternativeKey;
import com.linkedin.restli.server.annotations.AlternativeKeys;
import com.linkedin.restli.server.annotations.BatchFinder;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.PagingContextParam;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.resources.CollectionResourceTaskTemplate;
import com.linkedin.restli.tools.twitter.TwitterTestDataModels.Status;
import com.linkedin.restli.tools.twitter.TwitterTestDataModels.User;
import java.util.Set;

/**
 * CollectionResource containing all statuses modeled using Task template with result wrappers.
 */
@RestLiCollection(name="statusTasksWrapped",
    keyName="statusID")
@AlternativeKeys(alternativeKeys = {@AlternativeKey(name="alt", keyCoercer=StringLongCoercer.class, keyType=String.class),
    @AlternativeKey(name="newAlt", keyCoercer=StringLongCoercer.class, keyType=String.class)})
@SuppressWarnings("deprecation")
public class StatusTaskResultWrappersResource extends CollectionResourceTaskTemplate<Long,Status>
{
  /**
   * Gets the status timeline for a given user
   */
  @Finder("user_timeline")
  public Task<CollectionResult<Status, User>> getUserTimeline(@PagingContextParam PagingContext pagingContext)
  {
    return null;
  }

  /**
   * Batch finder for statuses
   */
  @BatchFinder(value="batchFinderByAction",  batchParam="criteria")
  public Task<BatchFinderResult<Status, Status, User>> batchFindStatuses(@QueryParam("criteria") Status[] criteria)
  {
    return null;
  }

  /**
   * Gets a batch of statuses
   */
  @RestMethod.BatchGet
  public Task<BatchResult<Long, Status>> batchGetWrapped(Set<Long> ids)
  {
    return null;
  }

  /**
   * Gets a single status resource
   */
  @RestMethod.Get
  public Task<GetResult<Status>> getWrapped(Long key)
  {
    return null;
  }

  /**
   * Gets all the resources
   */
  @RestMethod.GetAll
  public Task<CollectionResult<Status, User>> getAllWrapped(@PagingContextParam PagingContext pagingContext)
  {
    return null;
  }

  /**
   * Ambiguous action binding test case
   */
  @Action(name="forward", resourceLevel= ResourceLevel.ENTITY)
  public Task<ActionResult<String>> forward(@ActionParam("to") long userID)
  {
    return null;
  }
}
