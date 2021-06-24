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


import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.examples.greetings.api.ComplexArray;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.ActionParam;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.ComplexKeyResourceTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * @author Moira Tagle
 * @version $Revision: $
 */
@RestLiCollection(name = "complexArray", namespace = "com.linkedin.restli.examples.greetings.client")
public class ComplexArrayResource extends ComplexKeyResourceTemplate<ComplexArray, ComplexArray, Greeting>
{
  private static Greeting DEFAULT_GREETING = new Greeting();

  @Override
  public Greeting get(ComplexResourceKey<ComplexArray, ComplexArray> key)
  {
    key.getKey().getArray();
    key.getKey().getNext().getArray();
    key.getParams().getArray();
    key.getParams().getNext().getArray();
    return DEFAULT_GREETING;
  }

  @Override
  public Map<ComplexResourceKey<ComplexArray, ComplexArray>, Greeting> batchGet(Set<ComplexResourceKey<ComplexArray, ComplexArray>> keys)
  {
    Map<ComplexResourceKey<ComplexArray, ComplexArray>, Greeting> map = new HashMap<>();
    for(ComplexResourceKey<ComplexArray, ComplexArray> key: keys)
    {
      map.put(key, get(key));
    }
    return map;
  }

  @Finder("finder")
  public List<Greeting> finder(@QueryParam("array") ComplexArray array)
  {
    array.getArray();
    array.getNext().getArray();

    List<Greeting> list = new ArrayList<>();
    list.add(DEFAULT_GREETING);
    return list;
  }

  @Action(name = "action")
  public int action(@ActionParam("array") ComplexArray array)
  {
    array.getArray();
    array.getNext().getArray();

    return 1;
  }
}
