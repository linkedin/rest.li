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


import com.linkedin.data.transform.DataProcessingException;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.examples.greetings.api.Message;
import com.linkedin.restli.examples.greetings.api.TwoPartKey;
import com.linkedin.restli.server.BatchUpdateRequest;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.ComplexKeyResourceTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Demonstrates a resource with a complex key.
 * @author jbetz
 *
 */
@RestLiCollection(
  name = "complexKeys",
  namespace = "com.linkedin.restli.examples.greetings.client",
  keyName="keys"
  )
public class ComplexKeysResource extends ComplexKeyResourceTemplate<TwoPartKey, TwoPartKey, Message>
{
  private static ComplexKeysDataProvider _dataProvider = new ComplexKeysDataProvider();

  @Override
  public Message get(final ComplexResourceKey<TwoPartKey, TwoPartKey> complexKey)
  {
    TwoPartKey key = complexKey.getKey();
    return _dataProvider.get(complexKey);
  }

  @Override
  public CreateResponse create(final Message message)
  {
    ComplexResourceKey<TwoPartKey, TwoPartKey> key = _dataProvider.create(message);
    return new CreateResponse(key);
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

  @Finder("prefix")
  public List<Message> prefix(@QueryParam("prefix") String prefix)
  {
    return _dataProvider.findByPrefix(prefix);
  }

  @Override
  public Map<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> batchGet(
      final Set<ComplexResourceKey<TwoPartKey, TwoPartKey>> ids)
  {
    return _dataProvider.batchGet(ids);
  }

  @Override
  public BatchUpdateResult<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> batchUpdate(
      final BatchUpdateRequest<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> entities)
  {
    return _dataProvider.batchUpdate(entities);
  }
}
