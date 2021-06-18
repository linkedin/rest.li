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


import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.typeref.api.LongRef;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * @author xma
 */
@RestLiCollection(name = "typerefKeys",
                  namespace = "com.linkedin.restli.examples.greetings.client",
                  keyTyperefClass = LongRef.class)
public class TyperefKeysResource extends CollectionResourceTemplate<Long, Greeting>
{
  @Override
  public CreateResponse create(Greeting entity)
  {
    return new CreateResponse(entity.getId());
  }

  @Override
  public Map<Long, Greeting> batchGet(Set<Long> keys)
  {
    Map<Long, Greeting> result = new HashMap<>();
    for (Long key : keys)
    {
      result.put(key, new Greeting().setId(key));
    }
    return result;
  }
}
