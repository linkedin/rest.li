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
import com.linkedin.restli.examples.greetings.api.Empty;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.GreetingCriteria;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.server.BatchCreateRequest;
import com.linkedin.restli.server.BatchCreateResult;
import com.linkedin.restli.server.BatchDeleteRequest;
import com.linkedin.restli.server.BatchFinderResult;
import com.linkedin.restli.server.BatchPatchRequest;
import com.linkedin.restli.server.BatchResult;
import com.linkedin.restli.server.BatchUpdateRequest;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.CollectionResult;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.BatchFinder;
import com.linkedin.restli.server.annotations.MaxBatchSize;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;
import com.linkedin.restli.server.util.PatchApplier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class for testing @MaxBatchSize annotation on batch methods
 *
 * @author Yingjie Bi
 */
@RestLiCollection(name = "batchGreeting", namespace = "com.linkedin.restli.examples.greetings.client", keyName = "key")
public class BatchGreetingResource extends CollectionResourceTemplate<Long, Greeting>
{

  private static final Greeting GREETING_ONE;
  private static final Greeting GREETING_TWO;
  private static final Greeting GREETING_THREE;

  private static final Map<Long, Greeting> DB;

  static
  {
    GREETING_ONE = new Greeting();
    GREETING_ONE.setTone(Tone.INSULTING);
    GREETING_ONE.setId(1l);
    GREETING_ONE.setMessage("Hi");

    GREETING_TWO = new Greeting();
    GREETING_TWO.setTone(Tone.FRIENDLY);
    GREETING_TWO.setId(2l);
    GREETING_TWO.setMessage("Hello");

    GREETING_THREE = new Greeting();
    GREETING_THREE.setTone(Tone.SINCERE);
    GREETING_THREE.setId(3l);
    GREETING_THREE.setMessage("How are you?");

    DB = new HashMap<>();
    DB.put(1l, GREETING_ONE);
    DB.put(2l,GREETING_TWO);
    DB.put(3l,GREETING_THREE);
  }

  @RestMethod.BatchGet
  @MaxBatchSize(value = 2, validate = true)
  public Map<Long, Greeting> batchGet(Set<Long> ids)
  {
    Map<Long, Greeting> batch = new HashMap<Long, Greeting>();
    Map<Long, RestLiServiceException> errors = new HashMap<Long, RestLiServiceException>();
    for (Long id : ids)
    {
      Greeting greeting = DB.get(id);
      if (greeting != null)
      {
        batch.put(id, greeting);
      }
      else
      {
        errors.put(id, new RestLiServiceException(HttpStatus.S_404_NOT_FOUND));
      }
    }

    return new BatchResult<>(batch, errors);
  }

  @RestMethod.BatchUpdate
  @MaxBatchSize(value = 2)
  public BatchUpdateResult<Long, Greeting> batchUpdate(BatchUpdateRequest<Long, Greeting> entities)
  {
    Map<Long, UpdateResponse> responseMap = new HashMap<Long, UpdateResponse>();
    for (Map.Entry<Long, Greeting> entry : entities.getData().entrySet())
    {
      responseMap.put(entry.getKey(), update(entry.getKey(), entry.getValue()));
    }
    return new BatchUpdateResult<>(responseMap);
  }

  @RestMethod.BatchPartialUpdate
  @MaxBatchSize(value = 2, validate = true)
  public BatchUpdateResult<Long, Greeting> batchUpdate(BatchPatchRequest<Long, Greeting> entityUpdates)
  {
    Map<Long, UpdateResponse> responseMap = new HashMap<Long, UpdateResponse>();
    for (Map.Entry<Long, PatchRequest<Greeting>> entry : entityUpdates.getData().entrySet())
    {
      responseMap.put(entry.getKey(), update(entry.getKey(), entry.getValue()));
    }
    return new BatchUpdateResult<>(responseMap);
  }

  @RestMethod.BatchCreate
  @MaxBatchSize(value = 2, validate = true)
  public BatchCreateResult<Long, Greeting> batchCreate(BatchCreateRequest<Long, Greeting> entities)
  {
    List<CreateResponse> responses = new ArrayList<>(entities.getInput().size());

    for (Greeting greeting : entities.getInput())
    {
      responses.add(new CreateResponse(greeting.getId()));
    }
    return new BatchCreateResult<>(responses);
  }

  @RestMethod.BatchDelete
  @MaxBatchSize(value = 2, validate = true)
  public BatchUpdateResult<Long, Greeting> batchDelete(BatchDeleteRequest<Long, Greeting> deleteRequest)
  {
    Map<Long, UpdateResponse> responseMap = new HashMap<Long, UpdateResponse>();
    for (Long id : deleteRequest.getKeys())
    {
      responseMap.put(id, new UpdateResponse(HttpStatus.S_204_NO_CONTENT));
    }
    return new BatchUpdateResult<>(responseMap);
  }

  @BatchFinder(value = "searchGreetings", batchParam = "criteria")
  @MaxBatchSize(value = 2, validate = true)
  public BatchFinderResult<GreetingCriteria, Greeting, Empty> searchGreetings(@QueryParam("criteria")
      GreetingCriteria[] criteria)
  {
    BatchFinderResult<GreetingCriteria, Greeting, Empty> batchFinderResult = new BatchFinderResult<>();

    for (GreetingCriteria currentCriteria: criteria)
    {
      if (currentCriteria.getId() == 1l)
      {
        CollectionResult<Greeting, Empty> c1 = new CollectionResult<>(Arrays.asList(GREETING_ONE), 1);
        batchFinderResult.putResult(currentCriteria, c1);
      }
      else if (currentCriteria.getId() == 2l)
      {
        CollectionResult<Greeting, Empty> c2 = new CollectionResult<>(Arrays.asList(GREETING_TWO), 1);
        batchFinderResult.putResult(currentCriteria, c2);
      }
    }

    return batchFinderResult;
  }

  @RestMethod.Get
  public Greeting get(Long id)
  {
    return DB.get(id);
  }


  @RestMethod.PartialUpdate
  public UpdateResponse update(Long id, PatchRequest<Greeting> patch)
  {
    Greeting greeting = DB.get(id);

    try
    {
      PatchApplier.applyPatch(greeting, patch);
    }
    catch (DataProcessingException e)
    {
      return new UpdateResponse(HttpStatus.S_400_BAD_REQUEST);
    }

    DB.put(id, greeting);

    return new UpdateResponse(HttpStatus.S_204_NO_CONTENT);
  }

  @RestMethod.Update
  public UpdateResponse update(Long id, Greeting entity)
  {
    DB.put(id, entity);

    return new UpdateResponse(HttpStatus.S_204_NO_CONTENT);
  }
}