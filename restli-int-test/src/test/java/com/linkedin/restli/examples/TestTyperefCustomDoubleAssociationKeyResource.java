/*
   Copyright (c) 2015 LinkedIn Corp.

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

import com.linkedin.data.template.DynamicRecordMetadata;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.restli.client.GetRequest;
import com.linkedin.restli.client.GetRequestBuilder;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.client.RestliRequestOptions;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpecImpl;
import com.linkedin.restli.examples.custom.types.CustomDouble;
import com.linkedin.restli.examples.greetings.api.Message;
import com.linkedin.restli.examples.typeref.api.CustomDoubleRef;
import com.linkedin.restli.examples.typeref.api.UriRef;
import com.linkedin.restli.internal.common.TestConstants;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * @author nshakar
 */
public class TestTyperefCustomDoubleAssociationKeyResource extends RestLiIntegrationTest
{
  private static final Client CLIENT = new TransportClientAdapter(new HttpClientFactory.Builder().build().getClient(Collections.<String, String>emptyMap()));
  private static final String URI_PREFIX = "http://localhost:1338/";
  private static final RestClient REST_CLIENT = new RestClient(CLIENT, URI_PREFIX);

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestOptionsDataProvider")
  private static Object[][] requestOptionsDataProvider()
  {
    return new Object[][]{{RestliRequestOptions.DEFAULT_OPTIONS},
        {com.linkedin.restli.examples.TestConstants.FORCE_USE_NEXT_OPTIONS}};
  }

  @BeforeClass
  public void initClass() throws Exception
  {
    System.setProperty(RestLiIntTestServer.VALIDATE_KEYS_PROPERTY_NAME, Boolean.TRUE.toString());
    super.init();
  }

  @AfterClass
  public void shutDown() throws Exception
  {
    super.shutdown();
    System.setProperty(RestLiIntTestServer.VALIDATE_KEYS_PROPERTY_NAME, Boolean.FALSE.toString());
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestOptionsDataProvider")
  public void testGet(RestliRequestOptions requestOptions) throws RemoteInvocationException, URISyntaxException
  {
    HashMap<String, CompoundKey.TypeInfo> keyParts = new HashMap<String, CompoundKey.TypeInfo>();
    keyParts.put("src", new CompoundKey.TypeInfo(CustomDouble.class, CustomDoubleRef.class));
    keyParts.put("dest", new CompoundKey.TypeInfo(URI.class, UriRef.class));
    GetRequestBuilder<CompoundKey, Message> getBuilder = new GetRequestBuilder<CompoundKey, Message>(
        "typerefCustomDoubleAssociationKeyResource",
        Message.class,
        new ResourceSpecImpl(EnumSet.of(ResourceMethod.GET),
                             new HashMap<String, DynamicRecordMetadata>(),
                             new HashMap<String, DynamicRecordMetadata>(),
                             CompoundKey.class,
                             null,
                             null,
                             Message.class,
                             keyParts),
        requestOptions);

    final String[] stringArray = {"foo"};
    GetRequest<Message> req = getBuilder
        .id(new CompoundKey()
            .append("src", new CustomDouble(100.0))
            .append("dest", new URI("http://www.linkedin.com/")))
        .setReqParam("array", stringArray)
        .build();
    Response<Message> resp = REST_CLIENT.sendRequest(req).getResponse();
    Message result = resp.getEntity();
    Assert.assertEquals(result.getId(), "100.0->www.linkedin.com");
    Assert.assertEquals(result.getMessage(),
                        String.format("I need some $20. Array contents %s.", Arrays.asList(stringArray)));

    // key validation failure scenario
    try
    {
      req = getBuilder.id(
          new CompoundKey().append("src", new CustomDouble(100.02)).append("dest", new URI("http://www.linkedin.com/")))
          .setReqParam("array", stringArray).build();
      REST_CLIENT.sendRequest(req).getResponse();
    }
    catch (RestLiResponseException ex)
    {
      Assert.assertEquals(ex.getServiceErrorMessage(), "Input field validation failure, reason: ERROR ::  :: \"100.02\" does not match [0-9]*\\.[0-9]\n");
      Assert.assertEquals(ex.getStatus(), HttpStatus.S_400_BAD_REQUEST.getCode());
    }
  }
}
