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
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.examples.greetings.api.Message;
import com.linkedin.restli.examples.greetings.api.TwoPartKey;
import com.linkedin.restli.examples.greetings.server.ComplexKeysDataProvider;
import com.linkedin.restli.server.BatchDeleteRequest;
import com.linkedin.restli.server.BatchPatchRequest;
import com.linkedin.restli.server.BatchResult;
import com.linkedin.restli.server.BatchUpdateRequest;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.ResourceLevel;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.AlternativeKey;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.ComplexKeyResourceTemplate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Resource for testing Alternative Key Feature for ComplexKeyResource template.
 */
@RestLiCollection(
    name = "complexKeyAltKey",
    namespace = "com.linkedin.restli.examples.greetings.client"
)
@AlternativeKey(name = "alt", keyCoercer = StringComplexKeyCoercer.class, keyType = String.class)
public class ComplexKeyAltKeyResource extends ComplexKeyResourceTemplate<TwoPartKey, TwoPartKey, Message>
{
  private static ComplexKeysDataProvider _dataProvider = new ComplexKeysDataProvider();

  @Override
  public CreateResponse create(Message entity)
  {
    TwoPartKey key = new TwoPartKey();
    key.setMajor("testKey");
    key.setMinor("testKey");
    ComplexResourceKey<TwoPartKey, TwoPartKey> complexKey = new ComplexResourceKey<>(key, new TwoPartKey());
    return new CreateResponse(complexKey, HttpStatus.S_201_CREATED);
  }

  @Override
  public Message get(final ComplexResourceKey<TwoPartKey, TwoPartKey> complexKey)
  {
    return _dataProvider.get(complexKey);
  }

  @Override
  public BatchResult<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> batchGet(
      final Set<ComplexResourceKey<TwoPartKey, TwoPartKey>> ids)
  {
    return _dataProvider.batchGet(ids);
  }

  @Action(name = "testAction", resourceLevel = ResourceLevel.ENTITY)
  public int testAction()
  {
    return 1;
  }

  @Override
  public UpdateResponse update(final ComplexResourceKey<TwoPartKey, TwoPartKey> key,
      final Message message)
  {
    _dataProvider.update(key, message);
    return new UpdateResponse(HttpStatus.S_204_NO_CONTENT);
  }

  @Override
  public UpdateResponse update(final ComplexResourceKey<TwoPartKey, TwoPartKey> key,
      final PatchRequest<Message> patch)
  {
    try
    {
      _dataProvider.partialUpdate(key, patch);
    }
    catch(DataProcessingException e)
    {
      return new UpdateResponse(HttpStatus.S_400_BAD_REQUEST);
    }

    return new UpdateResponse(HttpStatus.S_204_NO_CONTENT);
  }

  @Override
  public BatchUpdateResult<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> batchUpdate(
      final BatchUpdateRequest<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> entities)
  {
    return _dataProvider.batchUpdate(entities);
  }

  @Override
  public BatchUpdateResult<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> batchUpdate(
      final BatchPatchRequest<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> patches)
  {
    return _dataProvider.batchUpdate(patches);
  }

  @Override
  public UpdateResponse delete(final ComplexResourceKey<TwoPartKey, TwoPartKey> key)
  {
    boolean removed = get(key) != null;

    return new UpdateResponse(removed ? HttpStatus.S_204_NO_CONTENT : HttpStatus.S_404_NOT_FOUND);
  }

  @Override
  public BatchUpdateResult<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> batchDelete(
      final BatchDeleteRequest<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> ids)
  {
    Map<ComplexResourceKey<TwoPartKey, TwoPartKey>, UpdateResponse> results =
        new HashMap<>();

    for (ComplexResourceKey<TwoPartKey, TwoPartKey> id : ids.getKeys())
    {
      results.put(id, delete(id));
    }

    return new BatchUpdateResult<>(results);
  }
}
