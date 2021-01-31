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

package com.linkedin.restli.examples.greetings;


import com.linkedin.restli.client.Request;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.client.GreetingsRequestBuilders;
import com.linkedin.restli.test.util.RootBuilderWrapper;

import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author Eran Leshem
 */
public class TestGreetingsRequestBuilders
{
  @Test
  public void testBatchable()
  {
    RootBuilderWrapper<Long, Greeting> builders = new RootBuilderWrapper<>(new GreetingsRequestBuilders());
    Request<Greeting> request = builders.get().id(1L).build();
    Assert.assertTrue(request.getResourceProperties().getSupportedMethods().contains(ResourceMethod.BATCH_GET));
  }
}
