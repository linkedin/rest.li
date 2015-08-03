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
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;

import java.net.HttpCookie;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * @author Boyang Chen
 */
@RestLiCollection(name = "cookie",
                  namespace = "com.linkedin.restli.examples.greetings.client")
public class CookieResource extends CollectionResourceTemplate<Long, Greeting>
{
  @Override
  public Greeting get(Long Key)
  {
    Greeting result = new Greeting().setId(1L).setMessage("NO CONTENT").setTone(Tone.FRIENDLY);
    setResponseCookie();
    return result;
  }

  @Override
  public Map<Long, Greeting> batchGet(Set<Long> keys)
  {
    Map<Long, Greeting> result = new HashMap<Long, Greeting>();
    for (Long key : keys)
    {
      result.put(key, new Greeting().setId(key).setMessage("NO CONTENT").setTone(Tone.FRIENDLY));
    }
    setResponseCookie();
    return result;
  }

  /**
   * Concatonates request cookies and add it as a single response cookie.
   */
  private void setResponseCookie()
  {
    ResourceContext context = getContext();
    List<HttpCookie> requestCookies = context.getRequestCookies();
    if (requestCookies.size() > 0)
    {
      for (HttpCookie elem : requestCookies)
      {
        context.addResponseCookie(new HttpCookie(elem.getValue(), elem.getName()));
      }
    }
    else
    {
      context.addResponseCookie(new HttpCookie("empty_name", "empty_cookie"));
    }
  }

}
