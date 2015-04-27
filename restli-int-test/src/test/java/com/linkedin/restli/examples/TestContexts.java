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

package com.linkedin.restli.examples;


import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.client.WithContextBuilders;
import com.linkedin.restli.examples.greetings.client.WithContextRequestBuilders;
import com.linkedin.restli.test.util.RootBuilderWrapper;

import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author Moira Tagle
 * @version $Revision: $
 */

public class TestContexts extends RestLiIntegrationTest
{
  private static final String PROJECTION_MESSAGE = "Projection!";
  private static final String NO_PROJECTION_MESSAGE = "No Projection!";
  private static final String HEADER_MESSAGE = "Header!";
  private static final String NO_HEADER_MESSAGE = "No Header!";
  private static final Tone HAS_KEYS = Tone.FRIENDLY;
  private static final Tone NO_KEYS = Tone.INSULTING;

  @BeforeClass
  public void initClass() throws Exception
  {
    super.init();
  }

  @AfterClass
  public void shutDown() throws Exception
  {
    super.shutdown();
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testGet(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Request<Greeting> requestWithProjection =
      builders.get().id(5L).fields(Greeting.fields().message(), Greeting.fields().tone()).build();
    Greeting projectionGreeting = getClient().sendRequest(requestWithProjection).getResponse().getEntity();
    Assert.assertEquals(projectionGreeting.getMessage(), PROJECTION_MESSAGE);
    Assert.assertEquals(projectionGreeting.getTone(), HAS_KEYS);

    Request<Greeting> requestNoProjection = builders.get().id(5L).build();
    Greeting greeting = getClient().sendRequest(requestNoProjection).getResponse().getEntity();
    Assert.assertEquals(greeting.getMessage(), NO_PROJECTION_MESSAGE);
    Assert.assertEquals(greeting.getTone(), HAS_KEYS);
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testFinder(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Request<CollectionResponse<Greeting>> requestWithProjection =
      builders.findBy("Finder")
        .fields(Greeting.fields().message(), Greeting.fields().tone())
        .setHeader("Expected-Header", HEADER_MESSAGE)
        .build();
    List<Greeting> projectionGreetings = getClient().sendRequest(requestWithProjection).getResponse().getEntity().getElements();

    // test projection and keys
    Greeting projectionGreeting = projectionGreetings.get(0);
    Assert.assertEquals(projectionGreeting.getMessage(), PROJECTION_MESSAGE);
    Assert.assertEquals(projectionGreeting.getTone(), NO_KEYS);

    // test header
    Greeting headerGreeting = projectionGreetings.get(1);
    Assert.assertEquals(headerGreeting.getMessage(), HEADER_MESSAGE);

    Request<CollectionResponse<Greeting>> requestNoProjection = builders.findBy("Finder").build();
    List<Greeting> noProjectionGreetings = getClient().sendRequest(requestNoProjection).getResponse().getEntity().getElements();

    // test projection and keys
    Greeting noProjectionGreeting = noProjectionGreetings.get(0);
    Assert.assertEquals(noProjectionGreeting.getMessage(), NO_PROJECTION_MESSAGE);
    Assert.assertEquals(noProjectionGreeting.getTone(), NO_KEYS);

    // test header
    Greeting noHeaderGreeting = noProjectionGreetings.get(1);
    Assert.assertEquals(noHeaderGreeting.getMessage(), NO_HEADER_MESSAGE);
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  private static Object[][] requestBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<Long, Greeting>(new WithContextBuilders()) },
      { new RootBuilderWrapper<Long, Greeting>(new WithContextBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
      { new RootBuilderWrapper<Long, Greeting>(new WithContextRequestBuilders()) },
      { new RootBuilderWrapper<Long, Greeting>(new WithContextRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) }
    };
  }
}
