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

import com.linkedin.data.DataMap;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.common.OptionsResponse;
import com.linkedin.restli.docgen.DefaultDocumentationRequestHandler;
import com.linkedin.restli.docgen.RestLiDocumentationRenderer;
import com.linkedin.restli.docgen.RestLiJSONDocumentationRenderer;
import com.linkedin.restli.docgen.RestLiResourceRelationship;
import com.linkedin.restli.examples.RestLiIntegrationTest;
import com.linkedin.restli.examples.greetings.client.GreetingsRequestBuilders;
import com.linkedin.restli.server.RestLiConfig;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


/**
 * Integration tests for configuring server with a custom JSON documentation handler..
 *
 * @author Karthik Balasubramanian
 */
public class TestCustomDocumentationHandler extends RestLiIntegrationTest
{

  public static final String CUSTOM_SUFFIX = ".Custom";

  @BeforeClass
  public void initClass() throws Exception
  {
    RestLiConfig config = new RestLiConfig();
    config.setDocumentationRequestHandler(new DefaultDocumentationRequestHandler()
    {
      @Override
      protected RestLiDocumentationRenderer getJsonDocumentationRenderer(DataSchemaResolver schemaResolver,
          RestLiResourceRelationship relationships)
      {
        return new RestLiJSONDocumentationRenderer(relationships)
        {
          @Override
          public void renderDataModel(NamedDataSchema schema, DataMap output, Map<String, String> requestHeaders) throws IOException
          {
            super.renderDataModel(schema, output, requestHeaders);
            DataMap schemaData =  _codec.stringToMap(schema.toString());
            String customName = schema.getFullName() + CUSTOM_SUFFIX;
            schemaData.put("name", customName);
            output.put(customName, schemaData);
          }
        };
      }
    });
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
        .addParam("format",  RestLiDocumentationRenderer.DocumentationFormat.JSON.toString().toLowerCase())
        .build();

    OptionsResponse optionsResponse = getClient().sendRequest(optionsRequest).getResponse().getEntity();
    Assert.assertEquals(1, optionsResponse.getResourceSchemas().size());
    Assert.assertNotNull(optionsResponse.getResourceSchemas().get("com.linkedin.restli.examples.greetings.client.greetings"));

    Assert.assertEquals(optionsResponse.getDataSchemas().size(), 10);
    List<String> expectedModels = new ArrayList<>(Arrays.asList("com.linkedin.restli.examples.greetings.api.Greeting",
        "com.linkedin.restli.examples.greetings.api.SearchMetadata",
        "com.linkedin.restli.examples.groups.api.TransferOwnershipRequest",
        "com.linkedin.restli.examples.greetings.api.Empty",
        "com.linkedin.restli.examples.greetings.api.Tone"));
    List<String> expectedCustomModels = expectedModels
        .stream().map(name -> name + CUSTOM_SUFFIX).collect(Collectors.toList());
    expectedModels.addAll(expectedCustomModels);
    Assert.assertTrue(optionsResponse.getDataSchemas().keySet().containsAll(expectedModels));
    for(String schema : expectedModels)
    {
      NamedDataSchema dataSchema = (NamedDataSchema) optionsResponse.getDataSchemas().get(schema);
      Assert.assertEquals(dataSchema.getFullName(), schema);
    }
  }
}
