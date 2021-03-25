/*
   Copyright (c) 2021 LinkedIn Corp.

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
 * Collection resource under an association resource which is also under an association resource
 */
@RestLiCollection(
    parent = AssociationsAssociationsResource.class,
    name = "AssociationsAssociationsSub",
    namespace = "com.linkedin.restli.examples.greetings.client",
    keyName = "subKey"
)
public class AssociationsAssociationsSubResource extends CollectionResourceTemplate<String, Message>
{
  public Message get(String key)
  {
    PathKeys pathKeys = getContext().getPathKeys();
    String srcKey = pathKeys.getAsString("src");
    String destKey = pathKeys.getAsString("dest");

    String anotherSrcKey = pathKeys.getAsString("anotherSrc");
    String anotherDestKey = pathKeys.getAsString("anotherDest");
    Message message = new Message();
    message.setId(srcKey + anotherSrcKey + key);
    message.setTone(Tone.FRIENDLY);
    message.setMessage(destKey+anotherDestKey);
    return message;
  }
}
