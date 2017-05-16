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

package com.linkedin.restli.tools.sample;

import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.ResourceLevel;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.*;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sample Collection Resource containing all simple greetings
 *
 * @author Min Chen
 */
@RestLiCollection(name = "greetings", namespace = "com.linkedin.restli.tools.sample")
public class SimpleGreetingResource extends CollectionResourceTemplate<Long,SimpleGreeting>
{
  /**
   * Creates a new Greeting
   */
  @Override
  public CreateResponse create(SimpleGreeting entity)
  {
    return null;
  }

  /**
   * Gets a batch of Greetings
   */
  @Override
  public Map<Long, SimpleGreeting> batchGet(Set<Long> ids)
  {
    return null;
  }

  /**
   * Gets a single greeting resource
   */
  @Override
  public SimpleGreeting get(Long key)
  {
    return null;
  }

  /**
   * Deletes a greeting resource
   */
  @Override
  public UpdateResponse delete(Long key)
  {
    return null;
  }

  /**
   * Updates a single greeting resource
   */
  @Override
  public UpdateResponse update(Long key, PatchRequest<SimpleGreeting> request)
  {
    return null;
  }

  /**
   * Action data template array return type and input type test case
   */
  @Action(name="greetingArrayAction",
          resourceLevel= ResourceLevel.COLLECTION)
  public SimpleGreeting[] statusArrayAction(@ActionParam("greetings") SimpleGreeting[] greetings)
  {
    return greetings;
  }

  /**
   * Action array return type test case
   */
  @Action(name="intArrayAction",
          resourceLevel= ResourceLevel.COLLECTION)
  public int[] intArrayAction(@ActionParam("ints") int[] ints)
  {
    return ints;
  }

  @Action(name="markGreetingAsRead",
      resourceLevel= ResourceLevel.COLLECTION)
  public String markGreetingAsRead(
      @Deprecated @Optional() @ActionParam("key") Long key,
      @Optional @ActionParam("urnKey") String urnKey)
  {
    return null;
  }

  // find greetings by message
  @Finder("message")
  public List<SimpleGreeting> find(@PagingContextParam PagingContext pagingContext,
                          @QueryParam("message") @Optional String title)
  {
    return null;
  }

  @Finder("recipients")
  public List<SimpleGreeting> findGreetingsByGuest(
      @Deprecated @Optional @QueryParam("recipientIds") long[] recipientIds,
      @Optional @QueryParam("recipients") String[] recipients)
  {
    return null;
  }
}
