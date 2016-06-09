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


import com.linkedin.restli.examples.greetings.api.Message;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.server.PathKeys;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.PathKeyParam;
import com.linkedin.restli.server.annotations.PathKeysParam;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Moira Tagle
 * @version $Revision: $
 */

@RestLiCollection(
  parent = AssociationsResource.class,
  name = "associationsSub",
  namespace = "com.linkedin.restli.examples.greetings.client",
  keyName = "subKey"
)
public class AssociationsSubResource extends CollectionResourceTemplate<String, Message>
{
  public Message get(String key)
  {
    PathKeys pathKeys = getContext().getPathKeys();
    String srcKey = pathKeys.getAsString("src");
    String destKey = pathKeys.getAsString("dest");
    Message message = new Message();
    message.setId(srcKey);
    message.setTone(Tone.FRIENDLY);
    message.setMessage(destKey);
    return message;
  }

  @Finder("tone")
  public List<Message> findByTone(@QueryParam("tone") Tone tone)
  {
    List<Message> messages = new ArrayList<Message>(2);

    Message message1 = new Message();
    message1.setId("one");
    message1.setMessage("one");
    message1.setTone(tone);

    Message message2 = new Message();
    message2.setId("two");
    message2.setMessage("two");
    message2.setTone(tone);

    messages.add(message1);
    messages.add(message2);

    return messages;
  }

  @Action(name="action")
  public int action()
  {
    return 1;
  }

  @Action(name="getSource")
  public String srcAction(@PathKeysParam PathKeys pks)
  {
    if (pks != null)
    {
      return pks.getAsString("src");
    }
    else
    {
      return null;
    }
  }

  @Action(name="concatenateStrings")
  public String thingAction(@PathKeyParam("src") String src, @PathKeyParam("dest") String dest)
  {
    return src + dest;
  }
}
