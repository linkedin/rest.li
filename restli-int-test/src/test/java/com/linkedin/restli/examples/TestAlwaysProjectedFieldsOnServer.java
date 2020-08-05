/*
   Copyright (c) 2020 LinkedIn Corp.

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
import com.linkedin.restli.common.CollectionMetadata;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.SearchMetadata;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.api.ToneFacet;
import com.linkedin.restli.examples.greetings.client.GreetingsBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsCallbackBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsCallbackRequestBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsPromiseBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsPromiseCtxBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsPromiseCtxRequestBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsPromiseRequestBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsRequestBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsTaskBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsTaskRequestBuilders;
import com.linkedin.restli.server.RestLiConfig;
import com.linkedin.restli.server.config.RestLiMethodConfigBuilder;
import com.linkedin.restli.test.util.RootBuilderWrapper;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * Integration tests for always projected fields server config.
 *
 * @author Karthik Balasubramanian
 */
public class TestAlwaysProjectedFieldsOnServer extends RestLiIntegrationTest
{
  @BeforeClass
  public void initClass() throws Exception
  {
    RestLiConfig config = new RestLiConfig();
    config.setMethodConfig(new RestLiMethodConfigBuilder()
            .addAlwaysProjectedFields("*.*", "id,tone")
            .addAlwaysProjectedFields("*.FINDER-*", "id,tone,total")
            .build());
    super.init(false, config);
  }

  @AfterClass
  public void shutDown() throws Exception
  {
    super.shutdown();
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testGetRequestWithProjection(RootBuilderWrapper<Long, Greeting> builders) throws Exception
  {
    Greeting.Fields fields = Greeting.fields();

    // Project 'message'. 'id' and 'tone' are included by server, leaving out 'senders'
    Request<Greeting> request = builders.get()
        .id(1L)
        .fields(fields.message())
        .build();

    Greeting greetingResponse = getClient().sendRequest(request).getResponse().getEntity();
    Assert.assertNotNull(greetingResponse.getId());
    Assert.assertNotNull(greetingResponse.getMessage());
    Assert.assertNotNull(greetingResponse.getTone());
    Assert.assertNull(greetingResponse.getSenders());

    // Project all fields including the 'senders' array
    request = builders.get()
        .id(1L)
        .fields(fields.id(), fields.message(), fields.tone(), fields.senders())
        .build();
    greetingResponse = getClient().sendRequest(request).getResponse().getEntity();
    Assert.assertNotNull(greetingResponse.getId());
    Assert.assertNotNull(greetingResponse.getMessage());
    Assert.assertNotNull(greetingResponse.getTone());
    List<String> fullSenders = greetingResponse.getSenders();
    Assert.assertNotNull(fullSenders);
    // We always send back 8 senders for all messages
    Assert.assertEquals(fullSenders.size(), 8);
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testMetadataWithProjection(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Greeting.Fields fields = Greeting.fields();

    Request<CollectionResponse<Greeting>> findRequest = builders.findBy("searchWithFacets").setQueryParam("tone", Tone.FRIENDLY)
            .fields(fields.message()) // 'id' and 'tone' are included by server
            .metadataFields(SearchMetadata.fields().facets().items().count())
            .pagingFields(CollectionMetadata.fields().count())  // 'total' is included by server
            .build(); // Project count, 'tone' is included by server

    CollectionResponse<Greeting> greetingsResponse = getClient().sendRequest(findRequest).getResponse().getEntity();
    List<Greeting> greetings = greetingsResponse.getElements();
    for (Greeting g : greetings)
    {
      Assert.assertEquals(g.getTone(), Tone.FRIENDLY);
      Assert.assertNotNull(g.getId());
      Assert.assertNotNull(g.getMessage());
    }
    SearchMetadata metadata = new SearchMetadata(greetingsResponse.getMetadataRaw());
    Assert.assertEquals(1, metadata.getFacets().size());
    for (ToneFacet facet : metadata.getFacets())
    {
      Assert.assertNotNull(facet.getCount());
      Assert.assertNotNull(facet.getTone());
    }
    CollectionMetadata pagingData = greetingsResponse.getPaging();
    Assert.assertNotNull(pagingData.getCount());
    Assert.assertNotNull(pagingData.getTotal());
    Assert.assertFalse(pagingData.hasLinks());
    Assert.assertFalse(pagingData.hasStart());
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  private static Object[][] requestBuilderDataProvider()
  {
    return new Object[][]
        {
          { new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders()) },
          { new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
          { new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders()) },
          { new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
          { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseBuilders()) },
          { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
          { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseRequestBuilders()) },
          { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
          { new RootBuilderWrapper<Long, Greeting>(new GreetingsCallbackBuilders()) },
          { new RootBuilderWrapper<Long, Greeting>(new GreetingsCallbackBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
          { new RootBuilderWrapper<Long, Greeting>(new GreetingsCallbackRequestBuilders()) },
          { new RootBuilderWrapper<Long, Greeting>(new GreetingsCallbackRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
          { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseCtxBuilders()) },
          { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseCtxBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
          { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseCtxRequestBuilders()) },
          { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseCtxRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
          { new RootBuilderWrapper<Long, Greeting>(new GreetingsTaskBuilders()) },
          { new RootBuilderWrapper<Long, Greeting>(new GreetingsTaskBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
          { new RootBuilderWrapper<Long, Greeting>(new GreetingsTaskRequestBuilders()) },
          { new RootBuilderWrapper<Long, Greeting>(new GreetingsTaskRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) }
       };
  }
}
