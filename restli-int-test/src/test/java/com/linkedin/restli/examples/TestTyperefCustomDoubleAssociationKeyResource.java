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
import com.linkedin.restli.client.RestliRequestOptions;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpecImpl;
import com.linkedin.restli.examples.greetings.api.Message;
import com.linkedin.restli.internal.common.TestConstants;

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
  private static final Client CLIENT = new TransportClientAdapter(new HttpClientFactory().getClient(Collections.<String, String>emptyMap()));
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
    super.init();
  }

  @AfterClass
  public void shutDown() throws Exception
  {
    super.shutdown();
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestOptionsDataProvider")
  public void testGet(RestliRequestOptions requestOptions) throws RemoteInvocationException
  {
    HashMap<String, CompoundKey.TypeInfo> keyParts = new HashMap<String, CompoundKey.TypeInfo>();
    keyParts.put("src", new CompoundKey.TypeInfo(Double.class, Double.class));
    keyParts.put("dest", new CompoundKey.TypeInfo(Double.class, Double.class));
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
    GetRequest<Message> req = getBuilder.id(new CompoundKey().append("src", 100.0).append("dest", 200.0))
                                        .setReqParam("array", stringArray)
                                        .build();
    Response<Message> resp = REST_CLIENT.sendRequest(req).getResponse();
    Message result = resp.getEntity();
    Assert.assertEquals(result.getId(), "100.0->200.0");
    Assert.assertEquals(result.getMessage(),
                        String.format("I need some $20. Array contents %s.", Arrays.asList(stringArray)));
  }
}
