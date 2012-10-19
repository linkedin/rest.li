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

import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.examples.StringTestKeys;
import com.linkedin.restli.examples.greetings.api.Message;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.server.annotations.Key;
import com.linkedin.restli.server.annotations.RestLiAssociation;
import com.linkedin.restli.server.resources.AssociationResourceTemplate;

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
  private Map<CompoundKey, Message> _db;

  public AssociationsResource()
  {
    _db = new HashMap<CompoundKey, Message>();
    createExample(StringTestKeys.URL, StringTestKeys.URL2, "I need some %20.");
    createExample(StringTestKeys.SIMPLEKEY, StringTestKeys.SIMPLEKEY2, "src1-dest1");
  }

  private void createExample(String srcKey, String destKey, String message)
  {
    CompoundKey key = new CompoundKey();
    key.append("src", srcKey);
    key.append("dest", destKey);
    Message greeting = new Message();
    greeting.setId(key.getPartAsString("src") + " " + key.getPartAsString("dest"));
    greeting.setMessage(message);
    greeting.setTone(Tone.SINCERE);
    _db.put(key, greeting);
  }

  @Override
  public Message get(CompoundKey id)
  {
    return _db.get(id);
  }
}
