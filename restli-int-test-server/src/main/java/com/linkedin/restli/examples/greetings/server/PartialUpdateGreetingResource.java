/*
   Copyright (c) 2018 LinkedIn Corp.

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
import com.linkedin.restli.server.BatchPatchRequest;
import com.linkedin.restli.server.BatchUpdateEntityResult;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UpdateEntityResponse;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.ReturnEntity;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.resources.KeyValueResource;
import com.linkedin.restli.server.util.PatchApplier;
import java.util.HashMap;
import java.util.Map;


/**
 * Resource for testing PARTIAL_UPDATE and BATCH_PARTIAL_UPDATE methods that return
 * the patched entity and entities, respectively.
 *
 * @author Evan Williams
 */
@RestLiCollection(
    name = "partialUpdateGreeting",
    namespace = "com.linkedin.restli.examples.greetings.client",
    keyName = "key"
)
public class PartialUpdateGreetingResource implements KeyValueResource<Long, Greeting>
{
  private static final int SIZE = 30;

  private static final Map<Long, Greeting> _db = new HashMap<>();
  static
  {
    for (long i = 0; i < SIZE; i++)
    {
      _db.put(i, new Greeting().setId(i).setMessage("Message " + i).setTone(Tone.FRIENDLY));
    }
  }

  @ReturnEntity
  @RestMethod.PartialUpdate
  public UpdateEntityResponse<Greeting> update(Long key, PatchRequest<Greeting> patch)
  {
    // used to test individual errors per id
    if (!_db.containsKey(key))
    {
      throw new RestLiServiceException(HttpStatus.S_404_NOT_FOUND);
    }

    Greeting greeting = _db.get(key);

    // used to test whole request failures
    if (patch.toString().contains(";DROP TABLE"))
    {
      throw new RuntimeException("Oops! You broke Rest.li");
    }

    try
    {
      PatchApplier.applyPatch(greeting, patch);
    }
    catch (DataProcessingException e)
    {
      throw new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST);
    }

    return new UpdateEntityResponse<>(HttpStatus.S_200_OK, greeting);
  }

  @ReturnEntity
  @RestMethod.BatchPartialUpdate
  public BatchUpdateEntityResult<Long, Greeting> batchUpdate(BatchPatchRequest<Long, Greeting> entityUpdates)
  {
    Map<Long, UpdateEntityResponse<Greeting>> responseMap = new HashMap<>();
    Map<Long, RestLiServiceException> errorMap = new HashMap<>();
    for (Map.Entry<Long, PatchRequest<Greeting>> entry : entityUpdates.getData().entrySet())
    {
      try
      {
        UpdateEntityResponse<Greeting> updateEntityResponse = update(entry.getKey(), entry.getValue());
        responseMap.put(entry.getKey(), updateEntityResponse);
      }
      catch (RestLiServiceException e)
      {
        errorMap.put(entry.getKey(), e);
      }
    }
    return new BatchUpdateEntityResult<>(responseMap, errorMap);
  }
}
