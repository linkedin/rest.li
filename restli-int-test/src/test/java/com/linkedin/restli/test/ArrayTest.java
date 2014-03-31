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

package com.linkedin.restli.test;


import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.Message;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.api.ToneFacet;
import com.linkedin.restli.test.util.RootBuilderWrapper;

import java.util.Arrays;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * Test if the generated request builder will still have Iterable parameters.
 * TODO: When Iterable parameters are deprecated, this test should be removed
 *
 * @author Keren Jin
 */
public class ArrayTest
{
  @Test(dataProvider = "requestBuilderDataProvider")
  public void testIterable(RootBuilderWrapper<Integer, Greeting> builders)
  {
    builders.findBy("Test")
      .setQueryParam("primitive", Arrays.asList(1, 2, 3))
      .setQueryParam("enum", Arrays.asList(Tone.FRIENDLY, Tone.SINCERE))
      .setQueryParam("record" ,Arrays.asList(new Message(), new Message()))
      .setQueryParam("existing", Arrays.asList(new ToneFacet(), new ToneFacet()));
  }

  @DataProvider
  private static Object[][] requestBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<Integer, Greeting>(new ArrayTestBuilders()) },
      { new RootBuilderWrapper<Integer, Greeting>(new ArrayTestRequestBuilders()) }
    };
  }
}
