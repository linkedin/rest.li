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

import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.examples.custom.types.CustomLong;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.typeref.api.CustomLongRef;
import com.linkedin.restli.examples.typeref.api.UnionRefInline;
import com.linkedin.restli.server.BatchCreateRequest;
import com.linkedin.restli.server.BatchCreateResult;
import com.linkedin.restli.server.BatchDeleteRequest;
import com.linkedin.restli.server.BatchPatchRequest;
import com.linkedin.restli.server.BatchUpdateRequest;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.Optional;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * @author Moira Tagle
 * @version $Revision: $
 */

@RestLiCollection(name="customTypes2",
                  namespace = "com.linkedin.restli.examples.greetings.client",
                  keyTyperefClass = CustomLongRef.class)
public class CustomTypesResource2 extends CollectionResourceTemplate<CustomLong, Greeting>
{

  @Override
  public Greeting get(CustomLong lo)
  {
    return new Greeting().setId(lo.toLong());
  }

  @Override
  public Map<CustomLong, Greeting> batchGet(Set<CustomLong> ids)
  {
    Map<CustomLong, Greeting> result = new HashMap<>(ids.size());

    for (CustomLong id: ids)
    {
      result.put(id, new Greeting().setId(id.toLong()));
    }

    return result;
  }

  @Override
  public BatchUpdateResult<CustomLong, Greeting> batchDelete(BatchDeleteRequest<CustomLong, Greeting> ids)
  {
    Map<CustomLong, UpdateResponse> results = new HashMap<>();
    for (CustomLong id: ids.getKeys())
    {
      results.put(id, new UpdateResponse(HttpStatus.S_204_NO_CONTENT));
    }
    return new BatchUpdateResult<>(results);
  }

  @Override
  public BatchUpdateResult<CustomLong, Greeting> batchUpdate(BatchPatchRequest<CustomLong, Greeting> entityUpdates)
  {
    Map<CustomLong, UpdateResponse> results = new HashMap<>();
    for (CustomLong id: entityUpdates.getData().keySet())
    {
      results.put(id, new UpdateResponse(HttpStatus.S_204_NO_CONTENT));
    }
    return new BatchUpdateResult<>(results);
  }

  @Override
  public BatchUpdateResult<CustomLong, Greeting> batchUpdate(BatchUpdateRequest<CustomLong, Greeting> entities)
  {
    Map<CustomLong, UpdateResponse> results = new HashMap<>();
    for (CustomLong id: entities.getData().keySet())
    {
      results.put(id, new UpdateResponse(HttpStatus.S_204_NO_CONTENT));
    }
    return new BatchUpdateResult<>(results);
  }

  @RestMethod.Create
  public CreateResponse create(final Greeting greeting, @QueryParam(value="unionRefParam") @Optional UnionRefInline unionRef)
  {
    // just echo back the provided id (for testing only, this would not a be correct implementation of POST)
    return new CreateResponse(new CustomLong(greeting.getId()));
  }

  @Override
  public BatchCreateResult<CustomLong, Greeting> batchCreate(BatchCreateRequest<CustomLong, Greeting> entities)
  {
    List<CreateResponse> results = new ArrayList<>();
    for (Greeting greeting: entities.getInput())
    {
      // just echo back the provided ids (for testing only, this would not a be correct implementation of POST)
      results.add(new CreateResponse(new CustomLong(greeting.getId()), HttpStatus.S_204_NO_CONTENT));
    }
    return new BatchCreateResult<>(results);
  }
}
