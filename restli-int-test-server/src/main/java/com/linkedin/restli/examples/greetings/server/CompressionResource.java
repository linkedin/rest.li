/*
   Copyright (c) 2013 LinkedIn Corp.

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

import java.util.ArrayList;
import java.util.List;

import com.linkedin.restli.examples.custom.types.CustomLong;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.typeref.api.CustomLongRef;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;

/**
 * Simple "hello world" resource that takes a repeat parameter to specify how many times it should appear.
 * Tuning the level of redundancy allows testing of compression correctness.
 * @author erli
 */
@RestLiCollection(name="compression",
namespace = "com.linkedin.restli.examples.greetings.client")
public class CompressionResource extends CollectionResourceTemplate<Long, Greeting>
{
  private static String _path = "/compression";

  public static String getPath()
  {
    return _path;
  }

  public static String getRedundantQueryExample()
  {
    return "?q=repeatedGreetings&repeat=100";
  }

  public static String getNoRedundantQueryExample()
  {
    return "?q=repeatedGreetings&repeat=0";
  }

  @Finder("repeatedGreetings")
  public List<Greeting> serveRepeatedGreeting(@QueryParam(value="repeat", typeref=CustomLongRef.class) CustomLong l)
  {
    List<Greeting> result = new ArrayList<Greeting>();
    Greeting g = new Greeting();
    g.setId(1);

    StringBuilder msg = new StringBuilder();

    for(long i=0;i < l.toLong(); i++)
    {
      msg.append("hello world ");
    }

    g.setMessage(msg.toString());
    result.add(g);

    return result;
  }

}