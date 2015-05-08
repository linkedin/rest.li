/*
   Copyright (c) 2015 LinkedIn Corp.

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


import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.examples.custom.types.CustomDouble;
import com.linkedin.restli.examples.greetings.api.Message;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.typeref.api.CustomDoubleRef;
import com.linkedin.restli.server.annotations.Key;
import com.linkedin.restli.server.annotations.RestLiAssociation;
import com.linkedin.restli.server.resources.AssociationResourceTemplate;


/**
 * @author nshankar
 */
@RestLiAssociation(name = "typerefCustomDoubleAssociationKeyResource",
                   namespace = "com.linkedin.restli.examples.greetings.client",
                   assocKeys = {@Key(name = "src", type = CustomDouble.class, typeref = CustomDoubleRef.class),
                       @Key(name = "dest", type = CustomDouble.class, typeref = CustomDoubleRef.class)})
public class TyperefCustomDoubleAssociationKeyResource extends AssociationResourceTemplate<Message>
{
  @Override
  public Message get(final CompoundKey key)
  {
    return new Message().setId(((CustomDouble) key.getPart("src")).toDouble() + "->" + ((CustomDouble) key.getPart(
        "dest")).toDouble()).setMessage("I need some $20").setTone(Tone.SINCERE);
  }
}
