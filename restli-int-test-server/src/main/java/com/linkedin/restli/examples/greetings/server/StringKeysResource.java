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

package com.linkedin.restli.examples.greetings.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.linkedin.data.transform.DataProcessingException;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.examples.StringTestKeys;
import com.linkedin.restli.examples.greetings.api.Message;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.server.BatchCreateRequest;
import com.linkedin.restli.server.BatchCreateResult;
import com.linkedin.restli.server.BatchDeleteRequest;
import com.linkedin.restli.server.BatchPatchRequest;
import com.linkedin.restli.server.BatchResult;
import com.linkedin.restli.server.BatchUpdateRequest;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.Context;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.Optional;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;
import com.linkedin.restli.server.util.PatchApplier;

/**
 * Demonstrates a resource keyed by a string.
 *
 * @author jbetz
 */
@RestLiCollection(
  name = "stringKeys",
  namespace = "com.linkedin.restli.examples.greetings.client",
  keyName = "parentKey"
  )
public class StringKeysResource extends CollectionResourceTemplate<String, Message>
{
  private static final String[] MESSAGES =
      { "I need some %20.", "Your tests run too slow.", };
  private static final Tone[] TONES = { Tone.FRIENDLY, Tone.SINCERE, Tone.INSULTING };

  private static final int INITIAL_SIZE = 20;
  private static final String[] INITIAL_MESSAGES = new String[INITIAL_SIZE];
  private static final Tone[] INITIAL_TONES = new Tone[INITIAL_SIZE];
  private static final String[] TEST_KEYS = new String[] {
    StringTestKeys.SIMPLEKEY, StringTestKeys.SIMPLEKEY2, StringTestKeys.SIMPLEKEY3,
    StringTestKeys.URL, StringTestKeys.URL2, StringTestKeys.URL3,
    StringTestKeys.SINGLE_ENCODED_URL, StringTestKeys.DOUBLE_ENCODED_URL,
    StringTestKeys.COMPLICATED_KEY
  };

  static {
    // generate some "random" initial data
    for (int i = 0; i < INITIAL_SIZE; i++)
      INITIAL_MESSAGES[i] = MESSAGES[i % MESSAGES.length];
    for (int i = 0; i < INITIAL_SIZE; i++)
      INITIAL_TONES[i] = TONES[i % TONES.length];
  }

  private final AtomicLong _idSeq = new AtomicLong();
  private String generateId()
  {
    return "message" + _idSeq.getAndIncrement();
  }
  private final Map<String, Message> _db = Collections.synchronizedMap(new LinkedHashMap<String, Message>());

  public StringKeysResource()
  {
    for (int i = 0; i < INITIAL_SIZE; i++)
    {
      Message g =
          new Message().setId(generateId())
                        .setMessage(INITIAL_MESSAGES[i])
                        .setTone(INITIAL_TONES[i]);
      _db.put(g.getId().toString(), g);
    }
    for(String key : TEST_KEYS)
    {
      _db.put(key, new Message().setId(key)
            .setMessage(key) // echo back the key in the message
            .setTone(Tone.SINCERE));
    }
  }

  @RestMethod.Create
  public CreateResponse create(Message entity)
  {
    _db.put(entity.getId().toString(), entity);
    return new CreateResponse(entity.getId());
  }

  @RestMethod.BatchGet
  public Map<String, Message> batchGet(Set<String> ids)
  {
    Map<String, Message> batch = new HashMap<String, Message>();
    Map<String, RestLiServiceException> errors = new HashMap<String, RestLiServiceException>();
    for (String id : ids)
    {
      Message g = _db.get(id);
      if (g != null)
      {
        batch.put(id, g);
      }
      else
      {
        errors.put(id, new RestLiServiceException(HttpStatus.S_404_NOT_FOUND));
      }
    }

    return new BatchResult<String, Message>(batch, errors);
  }

  @RestMethod.BatchUpdate
  public BatchUpdateResult<String, Message> batchUpdate(BatchUpdateRequest<String, Message> entities)
  {
    Map<String, UpdateResponse> responseMap = new HashMap<String, UpdateResponse>();
    for (Map.Entry<String, Message> entry : entities.getData().entrySet())
    {
      responseMap.put(entry.getKey(), update(entry.getKey(), entry.getValue()));
    }
    return new BatchUpdateResult<String, Message>(responseMap);
  }

  @RestMethod.BatchPartialUpdate
  public BatchUpdateResult<String, Message> batchUpdate(BatchPatchRequest<String, Message> entityUpdates)
  {
    Map<String, UpdateResponse> responseMap = new HashMap<String, UpdateResponse>();
    for (Map.Entry<String, PatchRequest<Message>> entry : entityUpdates.getData().entrySet())
    {
      responseMap.put(entry.getKey(), update(entry.getKey(), entry.getValue()));
    }
    return new BatchUpdateResult<String, Message>(responseMap);
  }

  @RestMethod.BatchCreate
  public BatchCreateResult<String, Message> batchCreate(BatchCreateRequest<String, Message> entities)
  {
    List<CreateResponse> responses = new ArrayList<CreateResponse>(entities.getInput().size());

    for (Message g : entities.getInput())
    {
      responses.add(create(g));
    }
    return new BatchCreateResult<String, Message>(responses);
  }

  @RestMethod.BatchDelete
  public BatchUpdateResult<String, Message> batchDelete(BatchDeleteRequest<String, Message> deleteRequest)
  {
    Map<String, UpdateResponse> responseMap = new HashMap<String, UpdateResponse>();
    for (String id : deleteRequest.getKeys())
    {
      responseMap.put(id, delete(id));
    }
    return new BatchUpdateResult<String, Message>(responseMap);
  }

  @RestMethod.Get
  public Message get(String key)
  {
    return _db.get(key);
  }

  @RestMethod.Delete
  public UpdateResponse delete(String key)
  {
    boolean removed = _db.remove(key) != null;

    return new UpdateResponse(removed ? HttpStatus.S_204_NO_CONTENT : HttpStatus.S_404_NOT_FOUND);
  }

  @RestMethod.PartialUpdate
  public UpdateResponse update(String key, PatchRequest<Message> patch)
  {
    Message g = _db.get(key);
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

    _db.put(key, g);

    return new UpdateResponse(HttpStatus.S_204_NO_CONTENT);
  }

  @RestMethod.Update
  public UpdateResponse update(String key, Message entity)
  {
    Message g = _db.get(key);
    if (g == null)
    {
      return new UpdateResponse(HttpStatus.S_404_NOT_FOUND);
    }

    _db.put(key, entity);

    return new UpdateResponse(HttpStatus.S_204_NO_CONTENT);
  }

  @Finder("search")
  public List<Message> search(@Context PagingContext ctx, @QueryParam("keyword") @Optional String keyword)
  {
    keyword = keyword.toLowerCase();
    List<Message> messages = new ArrayList<Message>();
    int idx = 0;
    int start = ctx.getStart();
    int stop = start + ctx.getCount();
    for (Message g : _db.values())
    {
      if (keyword == null || g.getMessage().toLowerCase().contains(keyword))
      {
        if (idx++ >= ctx.getStart())
        {
          messages.add(g);
        }

        if (idx == stop)
        {
          break;
        }
      }
    }
    return messages;
  }
}
