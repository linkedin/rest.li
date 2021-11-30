/*
   Copyright (c) 2021 LinkedIn Corp.

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


import com.linkedin.data.transform.DataProcessingException;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.server.BatchCreateRequest;
import com.linkedin.restli.server.BatchCreateResult;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.PathKeys;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.ActionParam;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.Optional;
import com.linkedin.restli.server.annotations.PagingContextParam;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestLiSimpleResource;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;
import com.linkedin.restli.server.resources.SimpleResourceTemplate;
import com.linkedin.restli.server.util.PatchApplier;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * This resource represents a collection resource under a collection resource,
 * which is under another collection resource, and that is under another simple resource
 *
 * Used to test sub-resource with depth more than 1
 */
@RestLiCollection(name = "greetingsOfgreetingsOfgreetingsOfgreeting", namespace = "com.linkedin.restli.examples.greetings.client", parent = CollectionOfCollectionOfSimpleResource.class)
public class CollectionOfCollectionOfCollectionOfSimpleResource extends CollectionResourceTemplate<Long, Greeting>
{
  private static final GreetingsResourceImpl _impl = new GreetingsResourceImpl("greetingsOfgreetingsOfgreetingsOfgreeting");

  @RestMethod.Get
  public Greeting get(Long key)
  {
    PathKeys pathKeys = getContext().getPathKeys();
    Long parentParentId = pathKeys.getAsLong("subgreetingsId");
    Long parentId= pathKeys.getAsLong("greetingsOfgreetingsOfgreetingId");

    return new Greeting().setId(key + parentId + parentParentId)
        .setMessage("SubSubSubGreeting")
        .setTone(Tone.FRIENDLY);
  }
}
