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


import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.server.PathKeys;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.HeaderParam;
import com.linkedin.restli.server.annotations.PathKeysParam;
import com.linkedin.restli.server.annotations.ProjectionParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.resources.KeyValueResource;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Moira Tagle
 * @version $Revision: $
 */

@RestLiCollection(name = "withContext", namespace = "com.linkedin.restli.examples.greetings.client")
public class WithContextResource implements KeyValueResource<Long,Greeting>
{
  private static final String ID = "withContextId";
  private static final String PROJECTION_MESSAGE = "Projection!";
  private static final String NO_PROJECTION_MESSAGE = "No Projection!";
  private static final String NO_HEADER_MESSAGE = "No Header!";

  @RestMethod.Get
  public Greeting get(Long key, @ProjectionParam MaskTree projection, @PathKeysParam PathKeys keys)
  {
    Greeting greeting = createGreeting(projection, keys);
    greeting.setId(key);

    return greeting;
  }

  @Finder("finder")
  public List<Greeting> finder(@HeaderParam("Expected-Header") String header, @ProjectionParam MaskTree projection, @PathKeysParam PathKeys keys)
  {
    List<Greeting> list = new ArrayList<>();

    Greeting greeting1 = createGreeting(projection, keys);
    greeting1.setId(1L);

    Greeting greeting2 = createGreeting(header);
    greeting2.setId(2L);

    list.add(greeting1);
    list.add(greeting2);
    return list;
  }

  private static Greeting createGreeting(String header)
  {
    Greeting greeting = new Greeting();
    if (header != null)
    {
      greeting.setMessage(header);
    }
    else
    {
      greeting.setMessage(NO_HEADER_MESSAGE);
    }
    greeting.setTone(Tone.SINCERE);
    return greeting;

  }

  private static Greeting createGreeting(MaskTree projection, PathKeys keys)
  {
    Greeting greeting = new Greeting();
    if (projection != null)
    {
      greeting.setMessage(PROJECTION_MESSAGE);
    }
    else
    {
      greeting.setMessage(NO_PROJECTION_MESSAGE);
    }
    if (keys.get(ID) == null)
    {
      greeting.setTone(Tone.INSULTING);
    }
    else
    {
      greeting.setTone(Tone.FRIENDLY);
    }
    return greeting;
  }

}
