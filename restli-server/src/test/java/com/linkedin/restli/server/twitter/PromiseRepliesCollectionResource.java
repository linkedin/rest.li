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
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.ActionParam;
import com.linkedin.restli.server.annotations.Context;
import com.linkedin.restli.server.annotations.RestMethod;

import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.KeyValueResource;
import com.linkedin.restli.server.resources.ResourceContextHolder;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.Status;

/**
 * Nested CollectionResource of all replies to a given status
 *
 * @author dellamag
 */
@RestLiCollection(parent=PromiseStatusCollectionResource.class,
                  name="promisereplies",
                  keyName="statusID")
public class PromiseRepliesCollectionResource extends ResourceContextHolder implements KeyValueResource<Long, Status>
{
  /**
   * Replies to the parent status
   */
  @RestMethod.Create
  public Promise<CreateResponse> create(Status entity)
  {
    throw new AssertionError("should be mocked");
  }

  /**
   * Iterates through all replies to the parent status
   */
  @Finder("paging")
  public Promise<List<Status>> getAll(@Context PagingContext pagingContext)
  {
    throw new AssertionError("should be mocked");
  }

  /**
   * Gets a batch of replies by statusID
   */
  @RestMethod.BatchGet
  public Promise<Map<Long, Status>> batchGet(Set<Long> ids)
  {
    throw new AssertionError("should be mocked");
  }


  /**
   * Simple test action to demonstrate actions on a nested collection resource
   */
  @Action(name="replyToAll")
  public void replyToAll(@ActionParam("status") String status)
  {
  }
}
