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
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.server.BatchDeleteRequest;
import com.linkedin.restli.server.BatchPatchRequest;
import com.linkedin.restli.server.BatchUpdateRequest;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.PathKeys;
import com.linkedin.restli.server.ResourceLevel;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.AlternativeKey;
import com.linkedin.restli.server.annotations.PathKeysParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;
import com.linkedin.restli.server.util.PatchApplier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * Resource for testing Alternative Key Feature for CollectionResource template.
 */
@RestLiCollection(name = "altKey", namespace = "com.linkedin.restli.examples.greetings.client")
@AlternativeKey(name = "alt", keyCoercer = StringLongCoercer.class, keyType = String.class)
public class CollectionAltKeyResource extends CollectionResourceTemplate<Long, Greeting>
{
  private static final String KEY_NAME = "altKeyId";
  private static AltKeyDataProvider _dataProvider = new AltKeyDataProvider();

  @Override
  public CreateResponse create(Greeting entity)
  {
    return new CreateResponse(entity.getId(), HttpStatus.S_201_CREATED);
  }

  @Override
  public Greeting get(Long id)
  {
    return _dataProvider.get(id);
  }

  @Override
  public Map<Long, Greeting> batchGet(Set<Long> ids)
  {
    return _dataProvider.batchGet(ids);
  }

  @Action(name = "getKeyValue", resourceLevel = ResourceLevel.ENTITY)
  public Long testAction(@PathKeysParam PathKeys keys)
  {
    return keys.getAsLong(KEY_NAME);
  }

  @Override
  public UpdateResponse update(Long key, Greeting entity)
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
  public UpdateResponse update(Long key, PatchRequest<Greeting> patch)
  {
    Greeting g = _dataProvider.get(key);
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

    return new UpdateResponse(HttpStatus.S_204_NO_CONTENT);
  }

  @Override
  public BatchUpdateResult<Long, Greeting> batchUpdate(BatchUpdateRequest<Long, Greeting> entities)
  {
    Map<Long, UpdateResponse> responseMap = new HashMap<>();
    for (Map.Entry<Long, Greeting> entry : entities.getData().entrySet())
    {
      responseMap.put(entry.getKey(), update(entry.getKey(), entry.getValue()));
    }
    return new BatchUpdateResult<>(responseMap);
  }

  @Override
  public BatchUpdateResult<Long, Greeting> batchUpdate(BatchPatchRequest<Long, Greeting> entityUpdates)
  {
    Map<Long, UpdateResponse> responseMap = new HashMap<>();
    for (Map.Entry<Long, PatchRequest<Greeting>> entry : entityUpdates.getData().entrySet())
    {
      responseMap.put(entry.getKey(), update(entry.getKey(), entry.getValue()));
    }
    return new BatchUpdateResult<>(responseMap);
  }

  @Override
  public UpdateResponse delete(Long key)
  {
    boolean removed = _dataProvider.get(key) != null;

    return new UpdateResponse(removed ? HttpStatus.S_204_NO_CONTENT : HttpStatus.S_404_NOT_FOUND);
  }

  @Override
  public BatchUpdateResult<Long, Greeting> batchDelete(BatchDeleteRequest<Long, Greeting> deleteRequest)
  {
    Map<Long, UpdateResponse> responseMap = new HashMap<>();
    for (Long id : deleteRequest.getKeys())
    {
      responseMap.put(id, delete(id));
    }
    return new BatchUpdateResult<>(responseMap);
  }
}
