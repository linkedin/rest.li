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


import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.server.CollectionResult;
import com.linkedin.restli.server.NoMetadata;

import java.util.List;


/**
 * @author Keren Jin
 */
public class GreetingFinderResult extends CollectionResult<Greeting, NoMetadata>
{
  public GreetingFinderResult(List<Greeting> elements)
  {
    super(elements);
  }

  public GreetingFinderResult(List<Greeting> elements, final Integer total)
  {
    super(elements, total, null);
  }
}
