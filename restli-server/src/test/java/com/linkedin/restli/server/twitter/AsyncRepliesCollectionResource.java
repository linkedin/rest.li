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

import com.linkedin.common.callback.Callback;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.annotations.CallbackParam;
import com.linkedin.restli.server.annotations.Context;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.CollectionResourceAsyncTemplate;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.Status;

/**
 * Nested CollectionResource of all replies to a given status.
 *
 * @author  femekci
 */
@RestLiCollection(parent=AsyncStatusCollectionResource.class,
                  name="asyncreplies",
                  keyName="statusID")
public class AsyncRepliesCollectionResource extends
    CollectionResourceAsyncTemplate<Long, Status>
{
  /**
   * Replies to the parent status.
   */
  @Override
  public void create(final Status entity,
                     @CallbackParam final Callback<CreateResponse> callback)
  {
    callback.onSuccess(null);
  }

  /**
   * Iterates through all replies to the parent status.
   */
  @Finder("paging")
  public void getAll(@Context final PagingContext pagingContext,
                     @CallbackParam final Callback<List<Status>> callback)
  {
    callback.onSuccess(null);
  }

  /**
   * Gets a batch of replies by statusID.
   */
  @Override
  public void batchGet(final Set<Long> ids,
                       @CallbackParam final Callback<Map<Long, Status>> callback)
  {
    callback.onSuccess(null);
  }
}
