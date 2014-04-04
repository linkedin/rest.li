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

package com.linkedin.restli.examples.greetings.server;


import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.groups.api.TransferOwnershipRequest;
import com.linkedin.restli.server.BatchCreateRequest;
import com.linkedin.restli.server.BatchCreateResult;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.Context;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.Optional;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * This resource represents a collection resource under a simple resource.
 */
@RestLiCollection(name = "subgreetings", namespace = "com.linkedin.restli.examples.greetings.client", parent = RootSimpleResource.class)
public class CollectionUnderSimpleResource extends CollectionResourceTemplate<Long, Greeting>
{
  private static final GreetingsResourceImpl _impl = new GreetingsResourceImpl("subgreetings");

  @RestMethod.Create
  public CreateResponse create(Greeting entity)
  {
    return _impl.create(entity);
  }

  @RestMethod.BatchGet
  public Map<Long, Greeting> batchGet(Set<Long> ids)
  {
    return _impl.batchGet(ids);
  }

  @RestMethod.BatchCreate
  public BatchCreateResult<Long, Greeting> batchCreate(BatchCreateRequest<Long, Greeting> entities)
  {
    return _impl.batchCreate(entities);
  }

  @RestMethod.Get
  public Greeting get(Long key)
  {
    return _impl.get(key);
  }

  @RestMethod.Delete
  public UpdateResponse delete(Long key)
  {
    return _impl.delete(key);
  }

  @RestMethod.PartialUpdate
  public UpdateResponse update(Long key, PatchRequest<Greeting> patch)
  {
    return _impl.update(key, patch);
  }

  @RestMethod.Update
  public UpdateResponse update(Long key, Greeting entity)
  {
    return _impl.update(key, entity);
  }

  @Finder("search")
  public List<Greeting> search(@Context PagingContext ctx, @QueryParam("tone") @Optional Tone tone, @QueryParam("complexQueryParam") @Optional Greeting complexQueryParam)
  {
    return _impl.search(ctx, tone);
  }

  @Action(name = "purge")
  public int purge()
  {
    return _impl.purge();
  }

  @Action(name = "exceptionTest")
  public void exceptionTest()
  {
    _impl.exceptionTest();
  }
}
