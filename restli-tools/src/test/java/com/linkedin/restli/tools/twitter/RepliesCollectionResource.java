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

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.ActionParam;
import com.linkedin.restli.server.annotations.PagingContextParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;
import com.linkedin.restli.tools.twitter.TwitterTestDataModels.Status;

/**
 * Nested CollectionResource of all replies to a given status
 *
 * @author dellamag
 */
@RestLiCollection(name="replies",
                    keyName="statusID",
                    parent=StatusCollectionResource.class)
public class RepliesCollectionResource extends CollectionResourceTemplate<Long, Status>
{
  /**
   * Replies to the parent status
   */
  @Override
  public CreateResponse create(Status entity)
  {
    return null;
  }

  /**
   * Iterates through all replies to the parent status
   */
  @Override
  public List<Status> getAll(@PagingContextParam PagingContext pagingContext)
  {
    return null;
  }

  /**
   * Gets a batch of replies by statusID
   */
  @Override
  public Map<Long, Status> batchGet(Set<Long> ids)
  {
    return null;
  }


  /**
   * Simple test action to demonstrate actions on a nested collection resource
   */
  @Action(name="replyToAll")
  public void replyToAll(@ActionParam("status") String status)
  {
  }
}
