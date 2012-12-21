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

import com.linkedin.restli.examples.custom.types.CustomLong;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.typeref.api.CustomLongRef;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Moira Tagle
 * @version $Revision: $
 */

@RestLiCollection(name="customTypes2",
                  namespace = "com.linkedin.restli.examples.greetings.client",
                  keyTyperefClass = CustomLongRef.class)
public class CustomTypesResource2 extends CollectionResourceTemplate<CustomLong, Greeting>
{

  @Override
  public Greeting get(CustomLong lo)
  {
    return new Greeting().setId(lo.toLong());
  }

  @Override
  public Map<CustomLong, Greeting> batchGet(Set<CustomLong> ids)
  {
    Map<CustomLong, Greeting> result = new HashMap<CustomLong, Greeting>(ids.size());

    for (CustomLong id: ids)
    {
      result.put(id, new Greeting().setId(id.toLong()));
    }

    return result;
  }

}
