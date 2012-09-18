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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.linkedin.restli.server.CustomLongRef;
import com.linkedin.restli.server.CustomStringRef;
import com.linkedin.restli.server.NoCoercerCustomStringRef;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.ActionParam;
import com.linkedin.restli.server.annotations.Context;

import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.custom.types.CustomLong;
import com.linkedin.restli.server.custom.types.CustomString;
import com.linkedin.restli.server.custom.types.NoCoercerCustomString;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.Status;

/**
 * Nested CollectionResource of all replies to a given status
 *
 * @author dellamag
 */
@RestLiCollection(parent=StatusCollectionResource.class,
                  name="replies",
                  keyName="statusID")
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
  @Finder("paging")
  public List<Status> getAll(@Context PagingContext pagingContext)
  {
    return null;
  }

  @Finder("customLong")
  public List<Status> customLong(@QueryParam(value="l", typeref=CustomLongRef.class) CustomLong l)
  {
    return null;
  }

  @Finder("customLongArray")
  public List<Status> customLongArray(@QueryParam(value="longs", typeref=CustomLongRef.class) CustomLong[] longs)
  {
    return null;
  }

  @Finder("customString")
  public List<Status> customString(@QueryParam(value="s", typeref=CustomStringRef.class) CustomString s)
  {
    return null;
  }

  @Finder("noCoercerCustomString")
  public List<Status> noCoercerCustomString(@QueryParam(value="s", typeref=NoCoercerCustomStringRef.class) NoCoercerCustomString s)
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
