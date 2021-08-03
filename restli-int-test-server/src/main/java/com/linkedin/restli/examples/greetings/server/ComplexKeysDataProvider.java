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


import com.linkedin.data.transform.DataProcessingException;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.examples.StringTestKeys;
import com.linkedin.restli.examples.greetings.api.Message;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.api.TwoPartKey;
import com.linkedin.restli.server.BatchDeleteRequest;
import com.linkedin.restli.server.BatchPatchRequest;
import com.linkedin.restli.server.BatchResult;
import com.linkedin.restli.server.BatchUpdateRequest;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.util.PatchApplier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class ComplexKeysDataProvider
{
  private Map<String, Message> _db = new HashMap<>();

  public ComplexKeysDataProvider()
  {
    addExample(StringTestKeys.URL,
               StringTestKeys.URL2,
               StringTestKeys.URL + " " + StringTestKeys.URL2);
    addExample(StringTestKeys.SIMPLEKEY,
               StringTestKeys.SIMPLEKEY2,
               StringTestKeys.SIMPLEKEY + " " + StringTestKeys.SIMPLEKEY2);
    addExample(StringTestKeys.SIMPLEKEY3,
        StringTestKeys.SIMPLEKEY4,
        StringTestKeys.SIMPLEKEY3 + " " + StringTestKeys.SIMPLEKEY4);
  }

  public Message get(ComplexResourceKey<TwoPartKey, TwoPartKey> key)
  {
    return _db.get(keyToString(key.getKey()));
  }

  public ComplexResourceKey<TwoPartKey, TwoPartKey> create(Message message)
  {
    TwoPartKey key = new TwoPartKey();
    key.setMajor(message.getMessage());
    key.setMinor(message.getMessage());

    _db.put(keyToString(key), message);
    return new ComplexResourceKey<>(key, new TwoPartKey());
  }

  public void update(ComplexResourceKey<TwoPartKey, TwoPartKey> key, Message message) {
    _db.put(keyToString(key.getKey()), message);
  }

  public void partialUpdate(
      ComplexResourceKey<TwoPartKey, TwoPartKey> key,
      PatchRequest<Message> patch) throws DataProcessingException
  {
    Message message = _db.get(keyToString(key.getKey()));
    PatchApplier.applyPatch(message, patch);
  }

  public List<Message> findByPrefix(String prefix)
  {
    ArrayList<Message> results = new ArrayList<>();

    for (Map.Entry<String, Message> entry : _db.entrySet())
    {
      if (entry.getKey().startsWith(prefix))
      {
        results.add(entry.getValue());
      }
    }

    return results;
  }

  public BatchResult<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> batchGet(
      Set<ComplexResourceKey<TwoPartKey, TwoPartKey>> keys)
  {
    Map<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> data =
        new HashMap<>();
    Map<ComplexResourceKey<TwoPartKey, TwoPartKey>, RestLiServiceException> errors =
        new HashMap<>();

    for (ComplexResourceKey<TwoPartKey, TwoPartKey> key : keys)
    {
      String stringKey = keyToString(key.getKey());
      if (_db.containsKey(stringKey))
      {
        data.put(key, _db.get(stringKey));
      }
      else
      {
        errors.put(key, new RestLiServiceException(HttpStatus.S_404_NOT_FOUND));
      }
    }

    return new BatchResult<>(data, errors);
  }

  public BatchUpdateResult<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> batchUpdate(
      BatchUpdateRequest<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> entities)
  {
    final Map<ComplexResourceKey<TwoPartKey, TwoPartKey>, UpdateResponse> results =
        new HashMap<>();
    final Map<ComplexResourceKey<TwoPartKey, TwoPartKey>, RestLiServiceException> errors =
        new HashMap<>();

    for (Map.Entry<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> entry : entities.getData().entrySet())
    {
      if (_db.containsKey(keyToString(entry.getKey().getKey())))
      {
        _db.put(keyToString(entry.getKey().getKey()), entry.getValue());
        results.put(entry.getKey(), new UpdateResponse(HttpStatus.S_200_OK));
      }
      else
      {
        errors.put(entry.getKey(), new RestLiServiceException(HttpStatus.S_404_NOT_FOUND));
      }
    }

    return new BatchUpdateResult<>(results, errors);
  }

  public BatchUpdateResult<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> batchUpdate(
      BatchPatchRequest<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> patches)
  {
    final Map<ComplexResourceKey<TwoPartKey, TwoPartKey>, UpdateResponse> results =
        new HashMap<>();

    for (Map.Entry<ComplexResourceKey<TwoPartKey, TwoPartKey>, PatchRequest<Message>> patch : patches.getData().entrySet())
    {
      try
      {
        this.partialUpdate(patch.getKey(), patch.getValue());
        results.put(patch.getKey(), new UpdateResponse(HttpStatus.S_204_NO_CONTENT));
      }
      catch (DataProcessingException e)
      {
        results.put(patch.getKey(), new UpdateResponse(HttpStatus.S_400_BAD_REQUEST));
      }
    }

    return new BatchUpdateResult<>(results);
  }

  public BatchUpdateResult<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> batchDelete(
      BatchDeleteRequest<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> ids)
  {
    final Map<ComplexResourceKey<TwoPartKey, TwoPartKey>, UpdateResponse> results =
        new HashMap<>();

    for (ComplexResourceKey<TwoPartKey, TwoPartKey> id : ids.getKeys())
    {
      _db.remove(keyToString(id.getKey()));
      results.put(id, new UpdateResponse(HttpStatus.S_204_NO_CONTENT));
    }

    return new BatchUpdateResult<>(results);
  }

  public List<Message> getAll()
  {
    ArrayList<Message> results = new ArrayList<>();

    for (Map.Entry<String, Message> entry : _db.entrySet())
    {
      results.add(entry.getValue());
    }

    return results;
  }

  private String keyToString(TwoPartKey key)
  {
    return key.getMajor() + " " + key.getMinor();
  }

  private void addExample(String majorKey, String minorKey, String messageText)
  {
    TwoPartKey key = new TwoPartKey();
    key.setMajor(majorKey);
    key.setMinor(minorKey);
    Message message = new Message();
    message.setId(keyToString(key));
    message.setMessage(messageText);
    message.setTone(Tone.SINCERE);
    _db.put(keyToString(key), message);
  }
}
