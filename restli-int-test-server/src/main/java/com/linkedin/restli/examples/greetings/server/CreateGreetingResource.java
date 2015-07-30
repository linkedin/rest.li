/*
   Copyright (c) 2015 LinkedIn Corp.

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
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.server.CreateKVResponse;
import com.linkedin.restli.server.BatchCreateKVResult;
import com.linkedin.restli.server.BatchCreateRequest;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.ReturnEntity;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.resources.KeyValueResource;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for testing the CREATE method that returns the entity.
 *
 * @author Boyang Chen
 */
@RestLiCollection(
    name = "createGreeting",
    namespace = "com.linkedin.restli.examples.greetings.client",
    keyName = "key"
)
public class CreateGreetingResource implements KeyValueResource<Long, Greeting>
{
  @ReturnEntity
  @RestMethod.Create
  public CreateKVResponse<Long, Greeting> create(Greeting entity)
  {
    Long id = 1L;
    entity.setId(id);
    return new CreateKVResponse<Long, Greeting>(entity.getId(), entity);
  }

  @ReturnEntity
  @RestMethod.BatchCreate
  public BatchCreateKVResult<Long, Greeting> batchCreate(BatchCreateRequest<Long, Greeting> entities)
  {
    List<CreateKVResponse<Long, Greeting>> responses = new ArrayList<CreateKVResponse<Long, Greeting>>(entities.getInput().size());
    // Maximum number of batch create entity is 3 in this resource. If more than 3 elements are detected, a 400 HTTP exception will be encoded
    int quota = 3;
    for (Greeting greeting : entities.getInput())
    {
      if (quota-- <= 0)
      {
        responses.add(new CreateKVResponse<Long, Greeting>(new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST, "exceed quota")));
      }
      else
      {
        responses.add(create(greeting));
      }
    }
    return new BatchCreateKVResult<Long, Greeting>(responses);
  }
}
