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


import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.examples.greetings.api.Empty;
import com.linkedin.restli.examples.greetings.api.Message;
import com.linkedin.restli.examples.greetings.api.MessageCriteria;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.server.BatchFinderResult;
import com.linkedin.restli.server.BatchPatchRequest;
import com.linkedin.restli.server.BatchUpdateRequest;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.CollectionResult;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.ResourceLevel;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.AssocKeyParam;
import com.linkedin.restli.server.annotations.BatchFinder;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.Key;
import com.linkedin.restli.server.annotations.Optional;
import com.linkedin.restli.server.annotations.PagingContextParam;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiAssociation;
import com.linkedin.restli.server.resources.AssociationResourceTemplate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.linkedin.restli.examples.AssociationResourceHelpers.DB;
import static com.linkedin.restli.examples.AssociationResourceHelpers.SIMPLE_COMPOUND_KEY;
import static com.linkedin.restli.examples.AssociationResourceHelpers.URL_COMPOUND_KEY;

/**
 * Demonstrates an assocation resource keyed by string.
 * @author jbetz
 *
 */
@RestLiAssociation(name="associations",
namespace = "com.linkedin.restli.examples.greetings.client",
assocKeys={@Key(name="src", type=String.class),
           @Key(name="dest", type=String.class)})
public class AssociationsResource extends AssociationResourceTemplate<Message>
{
  public CreateResponse create(Message message)
  {
    // Associations should never support creates or batch_creates. This is a bug in Rest.li that needs to be fixed.
    // For now we are implementing this method to make sure that calling getId() on the client throws an exception.
    return new CreateResponse(SIMPLE_COMPOUND_KEY, HttpStatus.S_201_CREATED);
  }

  @Override
  public Message get(CompoundKey id)
  {
    return DB.get(id);
  }

  @Override
  public Map<CompoundKey, Message> batchGet(Set<CompoundKey> ids)
  {
    Map<CompoundKey, Message> result = new HashMap<>();
    for (CompoundKey key: ids)
    {
      result.put(key, DB.get(key));
    }
    return result;
  }

  @Override
  public BatchUpdateResult<CompoundKey, Message> batchUpdate(BatchUpdateRequest<CompoundKey, Message> entities)
  {
    if (!entities.getData().equals(DB))
    {
      throw new RestLiServiceException(HttpStatus.S_417_EXPECTATION_FAILED);
    }

    return buildUpdateResult(entities.getData().keySet());
  }

  @Override
  public BatchUpdateResult<CompoundKey, Message> batchUpdate(BatchPatchRequest<CompoundKey, Message> patches)
  {
    if (!patches.getData().keySet().equals(DB.keySet()))
    {
      throw new RestLiServiceException(HttpStatus.S_417_EXPECTATION_FAILED);
    }

    return buildUpdateResult(patches.getData().keySet());
  }

  private BatchUpdateResult<CompoundKey, Message> buildUpdateResult(Set<CompoundKey> keys)
  {
    Map<CompoundKey, UpdateResponse> result = new HashMap<>();
    for (CompoundKey key: keys)
    {
      result.put(key, new UpdateResponse(HttpStatus.S_204_NO_CONTENT));
    }
    return new BatchUpdateResult<>(result);
  }

  @Finder("assocKeyFinder")
  public List<Message> assocKeyFinder(@AssocKeyParam("src") String src)
  {
    if (src.equals(SIMPLE_COMPOUND_KEY.getPartAsString("src")))
    {
      return Collections.singletonList(DB.get(SIMPLE_COMPOUND_KEY));
    }
    else if (src.equals(URL_COMPOUND_KEY.getPartAsString("src")))
    {
      return Collections.singletonList(DB.get(URL_COMPOUND_KEY));
    }
    return Collections.emptyList();
  }

  @Finder("assocKeyFinderOpt")
  public List<Message> assocKeyFinderOpt(@Optional @AssocKeyParam("src") String src)
  {
    return Collections.emptyList();
  }


  private static final Message m1 = new Message().setMessage("hello").setTone(Tone.FRIENDLY);
  private static final Message m2 = new Message().setMessage("world").setTone(Tone.FRIENDLY);

  @BatchFinder(value = "searchMessages", batchParam = "criteria")
  public BatchFinderResult<MessageCriteria, Message, Empty> searchMessages(@AssocKeyParam("src") String src, @PagingContextParam PagingContext context,
      @QueryParam("criteria") MessageCriteria[] criteria)
  {
    BatchFinderResult<MessageCriteria, Message, Empty> batchFinderResult = new BatchFinderResult<>();

    for (MessageCriteria currentCriteria: criteria) {
      if (currentCriteria.getTone() == Tone.FRIENDLY) {
        // on success
        CollectionResult<Message, Empty> cr = new CollectionResult<>(Arrays.asList(m1, m2), 2);
        batchFinderResult.putResult(currentCriteria, cr);
      } else {
        // on error: to construct error response for test
        batchFinderResult.putError(currentCriteria, new RestLiServiceException(HttpStatus.S_404_NOT_FOUND, "Failed to find message!"));
      }
    }

    return batchFinderResult;
  }

  @Action(name = "testAction", resourceLevel = ResourceLevel.ENTITY)
  public String testAction()
  {
    return "Hello!";
  }

}
