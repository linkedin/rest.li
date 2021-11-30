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

import java.util.HashMap;
import java.util.Map;

import com.linkedin.restli.examples.StringTestKeys;
import com.linkedin.restli.examples.greetings.api.Message;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.server.ResourceLevel;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestMethod.Get;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;

/**
 * Demonstrates a sub resource keyed by string.
 * @author jbetz
 */
@RestLiCollection(
  parent = StringKeysResource.class,
  name = "stringKeysSub",
  namespace = "com.linkedin.restli.examples.greetings.client",
  keyName = "subKey"
  )
public class StringKeysSubResource extends CollectionResourceTemplate<String, Message>
{
  private Map<String, Message> _db = new HashMap<>();

  public StringKeysSubResource()
  {
    addExample(StringTestKeys.URL, StringTestKeys.URL2, StringTestKeys.URL + " " + StringTestKeys.URL2);
    addExample(StringTestKeys.SIMPLEKEY, StringTestKeys.SIMPLEKEY2, StringTestKeys.SIMPLEKEY + " " + StringTestKeys.SIMPLEKEY);
  }

  private void addExample(String parentKey, String subKey, String text)
  {
    Message message = new Message();
    message.setId(keyToString(parentKey, subKey));
    message.setMessage(text);
    message.setTone(Tone.SINCERE);
    _db.put(keyToString(parentKey, subKey), message);
  }

  private String keyToString(String parentKey, String subKey)
  {
    return parentKey + " " + subKey;
  }

  @Get
  public Message get(String key)
  {
    return _db.get(getParentKey() + " " + key);
  }

  public String getParentKey()
  {
    return getContext().getPathKeys().get("parentKey");
  }
}
