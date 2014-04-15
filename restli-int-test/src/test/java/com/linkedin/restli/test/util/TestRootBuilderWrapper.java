/*
   Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.restli.test.util;


import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.client.GreetingsBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsRequestBuilders;
import org.testng.Assert;
import org.testng.annotations.Test;


public class TestRootBuilderWrapper
{
  @Test
  public void testBuilderVersion()
  {
    RootBuilderWrapper<Long, Greeting> rootBuilderWrapper1 =
        new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders());
    RootBuilderWrapper<Long, Greeting> rootBuilderWrapper2 =
        new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders());

    Assert.assertFalse(rootBuilderWrapper1.areRestLi2Builders());
    Assert.assertTrue(rootBuilderWrapper2.areRestLi2Builders());

    Assert.assertFalse(rootBuilderWrapper1.get().isRestLi2Builder());
    Assert.assertTrue(rootBuilderWrapper2.get().isRestLi2Builder());

    RootBuilderWrapper<Long, Greeting> dummyBuilder =
        new RootBuilderWrapper<Long, Greeting>(new MyRequestBuilders());
    Assert.assertFalse(dummyBuilder.areRestLi2Builders());
  }

  @Test
  public void testWrapperMethodNameGeneration()
  {
    RootBuilderWrapper<Long, Greeting> rootBuilderWrapper1 =
        new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders());
    RootBuilderWrapper<Long, Greeting> rootBuilderWrapper2 =
        new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders());

    rootBuilderWrapper1.findBy("searchWithTones").addQueryParam("tones", Tone.FRIENDLY);
    rootBuilderWrapper1.findBy("searchWithTones").setParam("Tones", new Tone[3]);
    rootBuilderWrapper1.findBy("SearchWithTones").addQueryParam("Tones", Tone.FRIENDLY);
    rootBuilderWrapper1.findBy("SearchWithTones").setParam("tones", new Tone[3]);

    rootBuilderWrapper1.action("someAction").setParam("a", 5);
    rootBuilderWrapper2.action("someAction").setParam("a", 5);
    rootBuilderWrapper1.action("SomeAction").setParam("A", 5);
    rootBuilderWrapper2.action("SomeAction").setParam("A", 5);
  }

  private static class MyRequestBuilders
  {
    public static String getPrimaryResource() {
      return "MyRequest";
    }
  }
}
