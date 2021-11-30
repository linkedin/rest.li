/*
   Copyright (c) 2021 LinkedIn Corp.

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

package com.linkedin.restli.examples.documentation;

import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.message.rest.RestMethod;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.common.OptionsResponse;
import com.linkedin.restli.docgen.DefaultDocumentationRequestHandler;
import com.linkedin.restli.docgen.RestLiDocumentationRenderer;
import com.linkedin.restli.examples.RestLiIntTestServer;
import com.linkedin.restli.examples.RestLiIntegrationTest;
import com.linkedin.restli.examples.greetings.client.GreetingsRequestBuilders;
import com.linkedin.restli.server.RestLiConfig;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


/**
 * Integration tests for JSON documentation handler.
 *
 * @author Karthik Balasubramanian
 */
public class TestDocumentationHandler extends RestLiIntegrationTest
{
  @BeforeClass
  public void initClass() throws Exception
  {
    RestLiConfig config = new RestLiConfig();
    config.setDocumentationRequestHandler(new DefaultDocumentationRequestHandler());
    super.init(false, config);
  }

  @AfterClass
  public void shutDown() throws Exception
  {
    super.shutdown();
  }

  @Test
  public void testOptionsJson() throws RemoteInvocationException
  {
    Request<OptionsResponse> optionsRequest = new GreetingsRequestBuilders().options()
        .addParam("format", RestLiDocumentationRenderer.DocumentationFormat.JSON.toString().toLowerCase())
        .build();

    OptionsResponse optionsResponse = getClient().sendRequest(optionsRequest).getResponse().getEntity();
    Assert.assertEquals(1, optionsResponse.getResourceSchemas().size());
    Assert.assertNotNull(optionsResponse.getResourceSchemas().get("com.linkedin.restli.examples.greetings.client.greetings"));

    Assert.assertEquals(5, optionsResponse.getDataSchemas().size());
    List<String> expectedModels = Arrays.asList(
        "com.linkedin.restli.examples.greetings.api.Greeting",
        "com.linkedin.restli.examples.greetings.api.SearchMetadata",
        "com.linkedin.restli.examples.groups.api.TransferOwnershipRequest",
        "com.linkedin.restli.examples.greetings.api.Empty",
        "com.linkedin.restli.examples.greetings.api.Tone");
    Assert.assertTrue(optionsResponse.getDataSchemas().keySet().containsAll(expectedModels));
    for(String schema : expectedModels)
    {
      NamedDataSchema dataSchema = (NamedDataSchema) optionsResponse.getDataSchemas().get(schema);
      Assert.assertEquals(dataSchema.getFullName(), schema);
    }
  }

  @Test
  public void testHTML() throws ExecutionException, InterruptedException
  {
    Map<String, String> transportProperties = Collections.singletonMap(HttpClientFactory.HTTP_REQUEST_TIMEOUT, "10000");

    Client client = newTransportClient(transportProperties);
    URI uri = URI.create("http://localhost:" + RestLiIntTestServer.DEFAULT_PORT + "/restli/docs");
    RestRequest r = new RestRequestBuilder(uri).setMethod(RestMethod.GET).build();
    RestResponse response = client.restRequest(r).get();

    Assert.assertFalse(response.getEntity().isEmpty());
    String html = response.getEntity().asString(StandardCharsets.UTF_8);
    Assert.assertTrue(html.contains("<a href=\"http://localhost:1338/restli/docs?format=json\">View in JSON format</a>"));
  }
}
