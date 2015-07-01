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

import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.server.CreateKVResponse;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.ReturnEntity;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.resources.KeyValueResource;

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
    if(entity.getTone() == Tone.INSULTING)
    {
      Greeting missingEntity = new Greeting();
      missingEntity.setId(10L);
      return new CreateKVResponse<Long, Greeting>(id, missingEntity);
    }
    return new CreateKVResponse<Long, Greeting>(id, entity);
  }
}
