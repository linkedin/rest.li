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
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.examples.StringTestKeys;
import com.linkedin.restli.examples.greetings.api.Message;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.api.TwoPartKey;
import com.linkedin.restli.server.util.PatchApplier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class ComplexKeysDataProvider
{
  private Map<String, Message> _db = new HashMap<String, Message>();

  public ComplexKeysDataProvider()
  {
    addExample(StringTestKeys.URL,
               StringTestKeys.URL2,
               StringTestKeys.URL + " " + StringTestKeys.URL2);
    addExample(StringTestKeys.SIMPLEKEY,
               StringTestKeys.SIMPLEKEY2,
               StringTestKeys.SIMPLEKEY + " " + StringTestKeys.SIMPLEKEY2);
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
    return new ComplexResourceKey<TwoPartKey, TwoPartKey>(key, new TwoPartKey());
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
    ArrayList<Message> results = new ArrayList<Message>();

    for (Map.Entry<String, Message> entry : _db.entrySet())
    {
      if (entry.getKey().startsWith(prefix))
      {
        results.add(entry.getValue());
      }
    }

    return results;
  }

  public Map<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> batchGet(
      Set<ComplexResourceKey<TwoPartKey, TwoPartKey>> keys)
  {
    Map<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> results =
        new HashMap<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message>();

    for (ComplexResourceKey<TwoPartKey, TwoPartKey> key : keys)
    {
      String stringKey = keyToString(key.getKey());
      if (_db.containsKey(stringKey))
      {
        results.put(key, _db.get(stringKey));
      }
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
