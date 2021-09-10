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

package com.linkedin.restli.examples.method;

import com.linkedin.data.DataMap;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.restli.client.CreateIdRequest;
import com.linkedin.restli.client.GetRequest;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.common.IdResponse;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.examples.RestLiIntegrationTest;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.client.GreetingsRequestBuilders;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.methods.DefaultMethodAdapterProvider;
import com.linkedin.restli.internal.server.methods.MethodAdapterProvider;
import com.linkedin.restli.internal.server.methods.arguments.CreateArgumentBuilder;
import com.linkedin.restli.internal.server.methods.arguments.RestLiArgumentBuilder;
import com.linkedin.restli.internal.server.response.ErrorResponseBuilder;
import com.linkedin.restli.server.ErrorResponseFormat;
import com.linkedin.restli.server.RestLiConfig;
import com.linkedin.restli.server.RestLiRequestData;
import com.linkedin.restli.server.RestLiRequestDataImpl;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


/**
 * Integration tests for a custom {@link MethodAdapterProvider}.
 */
public class TestCustomMethodAdapterProvider extends RestLiIntegrationTest
{
  private static final Greeting FRIENDLY = new Greeting().setMessage("Friendly").setTone(Tone.FRIENDLY);

  @BeforeClass
  public void initClass() throws Exception
  {
    RestLiConfig config = new RestLiConfig();
    config.setMethodAdapterProvider(
        new DefaultMethodAdapterProvider(new ErrorResponseBuilder(ErrorResponseFormat.MESSAGE_AND_SERVICECODE))
        {
          @Override
          public RestLiArgumentBuilder getArgumentBuilder(ResourceMethod resourceMethod)
          {
            // Override the behavior of the CREATE argument builder
            if (resourceMethod == ResourceMethod.CREATE)
            {
              return new CreateArgumentBuilder()
              {
                @Override
                public RestLiRequestData extractRequestData(RoutingResult routingResult, DataMap dataMap)
                {
                    // Always use the FRIENDLY record regardless of the actual data
                    return new RestLiRequestDataImpl.Builder().entity(FRIENDLY).build();
                }
              };
            }
            else
            {
              return super.getArgumentBuilder(resourceMethod);
            }
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
  public void testCreateAndGetOverriddenRecord() throws RemoteInvocationException
  {
    Greeting insulting = new Greeting().setMessage("Insulting").setTone(Tone.INSULTING);
    CreateIdRequest<Long, Greeting> createRequest = new GreetingsRequestBuilders().create().input(insulting).build();
    Response<IdResponse<Long>> createResponse = getClient().sendRequest(createRequest).getResponse();

    Assert.assertFalse(createResponse.hasError());
    Long createId = createResponse.getEntity().getId();

    GetRequest<Greeting> getRequest = new GreetingsRequestBuilders().get().id(createId).build();
    Response<Greeting> getResponse = getClient().sendRequest(getRequest).getResponse();

    Assert.assertFalse(getResponse.hasError());
    Greeting actualEntity = getResponse.getEntity();
    Assert.assertEquals(actualEntity.getMessage(), FRIENDLY.getMessage());
    Assert.assertEquals(actualEntity.getTone(), FRIENDLY.getTone());
  }
}
