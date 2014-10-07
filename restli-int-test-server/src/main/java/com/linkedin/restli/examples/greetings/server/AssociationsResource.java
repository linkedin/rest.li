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
import com.linkedin.restli.examples.greetings.api.Message;
import com.linkedin.restli.server.BatchPatchRequest;
import com.linkedin.restli.server.BatchUpdateRequest;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.AssocKeyParam;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.Key;
import com.linkedin.restli.server.annotations.Optional;
import com.linkedin.restli.server.annotations.RestLiAssociation;
import com.linkedin.restli.server.resources.AssociationResourceTemplate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.linkedin.restli.examples.AssociationResourceHelpers.DB;
import static com.linkedin.restli.examples.AssociationResourceHelpers.SIMPLE_COMPOUND_KEY;


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
    Map<CompoundKey, Message> result = new HashMap<CompoundKey, Message>();
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
    Map<CompoundKey, UpdateResponse> result = new HashMap<CompoundKey, UpdateResponse>();
    for (CompoundKey key: keys)
    {
      result.put(key, new UpdateResponse(HttpStatus.S_204_NO_CONTENT));
    }
    return new BatchUpdateResult<CompoundKey, Message>(result);
  }

  @Finder("assocKeyFinder")
  public List<Message> assocKeyFinder(@AssocKeyParam("src") String src)
  {
    return Collections.emptyList();
  }

  @Finder("assocKeyFinderOpt")
  public List<Message> assocKeyFinderOpt(@Optional @AssocKeyParam("src") String src)
  {
    return Collections.emptyList();
  }
}
