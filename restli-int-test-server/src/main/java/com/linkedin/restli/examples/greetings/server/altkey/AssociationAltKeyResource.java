/*
   Copyright (c) 2019 LinkedIn Corp.

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
package com.linkedin.restli.examples.greetings.server.altkey;

import com.linkedin.data.transform.DataProcessingException;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.server.BatchDeleteRequest;
import com.linkedin.restli.server.BatchPatchRequest;
import com.linkedin.restli.server.BatchUpdateRequest;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.ResourceLevel;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.ActionParam;
import com.linkedin.restli.server.annotations.AlternativeKey;
import com.linkedin.restli.server.annotations.Key;
import com.linkedin.restli.server.annotations.RestLiAssociation;
import com.linkedin.restli.server.resources.AssociationResourceTemplate;
import com.linkedin.restli.server.util.PatchApplier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 *  Resource for testing Alternative Key Feature for AssociationResource template.
 */
@RestLiAssociation(name="associationAltKey",
    namespace = "com.linkedin.restli.examples.greetings.client",
    assocKeys={@Key(name="message", type=String.class),
        @Key(name="greetingId", type=Long.class)})
@AlternativeKey(name = "alt", keyCoercer = StringCompoundKeyCoercer.class, keyType = String.class)
public class AssociationAltKeyResource extends AssociationResourceTemplate<Greeting>
{
  private static AltKeyDataProvider _dataProvider = new AltKeyDataProvider();

  public CreateResponse create(Greeting entity)
  {
    CompoundKey key = new CompoundKey();
    key.append("message", "h");
    key.append("greetingId", 3L);
    return new CreateResponse(key, HttpStatus.S_201_CREATED);
  }
  @Override
  public Greeting get(CompoundKey id)
  {
    return _dataProvider.get(id);
  }

  @Override
  public Map<CompoundKey, Greeting> batchGet(Set<CompoundKey> ids)
  {
    return _dataProvider.compoundKeyBatchGet(ids);
  }

  @Action(name = "testAction", resourceLevel = ResourceLevel.ENTITY)
  public String testAction()
  {
    return "Hello!";
  }

  @Override
  public UpdateResponse update(CompoundKey key, Greeting entity)
  {
    if (_dataProvider.get(key) != null)
    {
      _dataProvider.update(key, entity);
      return new UpdateResponse(HttpStatus.S_204_NO_CONTENT);
    }
    else
    {
      new UpdateResponse(HttpStatus.S_404_NOT_FOUND);
    }
    return new UpdateResponse(HttpStatus.S_204_NO_CONTENT);
  }

  @Override
  public UpdateResponse update(CompoundKey key, PatchRequest<Greeting> patch)
  {
    Greeting g = get(key);
    if (g == null)
    {
      return new UpdateResponse(HttpStatus.S_404_NOT_FOUND);
    }

    try
    {
      PatchApplier.applyPatch(g, patch);
    }
    catch (DataProcessingException e)
    {
      return new UpdateResponse(HttpStatus.S_400_BAD_REQUEST);
    }

    update(key, g);
    return new UpdateResponse(HttpStatus.S_204_NO_CONTENT);
  }

  @Override
  public BatchUpdateResult<CompoundKey, Greeting> batchUpdate(BatchUpdateRequest<CompoundKey, Greeting> entities)
  {
    Map<CompoundKey, UpdateResponse> responseMap = new HashMap<>();
    for (Map.Entry<CompoundKey, Greeting> entry : entities.getData().entrySet())
    {
      responseMap.put(entry.getKey(), update(entry.getKey(), entry.getValue()));
    }
    return new BatchUpdateResult<>(responseMap);
  }

  @Override
  public BatchUpdateResult<CompoundKey, Greeting> batchUpdate(BatchPatchRequest<CompoundKey, Greeting> entityUpdates)
  {
    Map<CompoundKey, UpdateResponse> responseMap = new HashMap<>();
    for (Map.Entry<CompoundKey, PatchRequest<Greeting>> entry : entityUpdates.getData().entrySet())
    {
      responseMap.put(entry.getKey(), update(entry.getKey(), entry.getValue()));
    }
    return new BatchUpdateResult<>(responseMap);
  }

  @Override
  public UpdateResponse delete(CompoundKey key)
  {
    boolean removed = get(key) != null;

    return new UpdateResponse(removed ? HttpStatus.S_204_NO_CONTENT : HttpStatus.S_404_NOT_FOUND);
  }

  @Override
  public BatchUpdateResult<CompoundKey, Greeting> batchDelete(BatchDeleteRequest<CompoundKey, Greeting> deleteRequest)
  {
    Map<CompoundKey, UpdateResponse> responseMap = new HashMap<>();
    for (CompoundKey id : deleteRequest.getKeys())
    {
      responseMap.put(id, delete(id));
    }
    return new BatchUpdateResult<>(responseMap);
  }
}
