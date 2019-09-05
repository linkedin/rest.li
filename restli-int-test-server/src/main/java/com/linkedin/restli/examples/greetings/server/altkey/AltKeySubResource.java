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

import com.linkedin.restli.examples.greetings.api.Message;
import com.linkedin.restli.server.annotations.AlternativeKey;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Resource for testing Alternative Key Feature for CollectionSubResource
 */
@RestLiCollection(parent = CollectionAltKeyResource.class,name = "altKeySub", namespace = "com.linkedin.restli.examples.greetings.client", keyName = "subKey")
@AlternativeKey(name = "alt", keyCoercer = StringKeyCoercer.class, keyType = String.class)
public class AltKeySubResource extends CollectionResourceTemplate<String, Message>
{
  private static Map<String, Message> _db = new HashMap<>();
  static
  {
    Message message1 = new Message();
    message1.setId("1");
    message1.setMessage("a");
    Message message2 = new Message();
    message2.setId("2");
    message2.setMessage("b");

    _db.put("1", message1);
    _db.put("2", message2);
  }

  @Override
  public Message get(String key)
  {
    return _db.get(key);
  }

  @Override
  public Map<String, Message> batchGet(Set<String> ids)
  {
    Map<String, Message> result = new HashMap<>();
    for(String id : ids)
    {
      result.put(id, _db.get(id));
    }
    return result;
  }
}
