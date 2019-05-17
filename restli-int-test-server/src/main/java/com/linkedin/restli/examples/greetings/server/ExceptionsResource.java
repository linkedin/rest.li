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

/**
 * $Id: $
 */

package com.linkedin.restli.examples.greetings.server;


import com.linkedin.data.DataMap;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.server.BatchCreateRequest;
import com.linkedin.restli.server.BatchCreateResult;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Josh Walker
 * @version $Revision: $
 */

@RestLiCollection(name = "exceptions",
                  namespace = "com.linkedin.restli.examples.greetings.client")
public class ExceptionsResource extends CollectionResourceTemplate<Long, Greeting>
{
  @Override
  @SuppressWarnings("deprecation")
  public Greeting get(Long key)
  {
    try
    {
      String s = (new String[0])[42];
    }
    catch (ArrayIndexOutOfBoundsException e)
    {
      Greeting details = new Greeting().setMessage("Hello, Sorry for the mess");

      throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR, "error processing request", e)
              .setServiceErrorCode(42).setErrorDetails(details.data());
    }
    return null;
  }

  /**
   * Responds with an error for requests to create insulting greetings, responds
   * with 201 created for all other requests.
   */
  @RestMethod.Create
  @SuppressWarnings("deprecation")
  public CreateResponse create(Greeting g)
  {
    if(g.hasTone() && g.getTone() == Tone.INSULTING)
    {
      RestLiServiceException notAcceptableException = new RestLiServiceException(HttpStatus.S_406_NOT_ACCEPTABLE,
                                                                          "I will not tolerate your insolence!");
      DataMap details = new DataMap();
      details.put("reason", "insultingGreeting");
      notAcceptableException.setErrorDetails(details);
      notAcceptableException.setServiceErrorCode(999);
      throw notAcceptableException;
    }
    else
    {
      return new CreateResponse(g.getId(), HttpStatus.S_201_CREATED);
    }
  }

  /**
   * For a batch create request, responds with an error for requests to create insulting greetings, responds
   * with 201 created for all other requests.
   */
  @Override
  public BatchCreateResult<Long, Greeting> batchCreate(BatchCreateRequest<Long, Greeting> entities)
  {
    List<CreateResponse> responses = new ArrayList<CreateResponse>(entities.getInput().size());

    for (Greeting g : entities.getInput())
    {
      try
      {
        responses.add(create(g));
      }
      catch (RestLiServiceException restliException)
      {
        responses.add(new CreateResponse(restliException));
      }
    }
    return new BatchCreateResult<Long, Greeting>(responses);
  }
}
