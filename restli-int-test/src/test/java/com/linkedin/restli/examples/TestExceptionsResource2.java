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


import com.linkedin.data.DataMap;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.restli.client.ActionRequest;
import com.linkedin.restli.client.GetRequest;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.client.Exceptions2Builders;
import com.linkedin.restli.internal.server.util.DataMapUtils;
import java.util.Collections;
import org.testng.Assert;
import org.testng.annotations.Test;


public class TestExceptionsResource2 extends RestLiIntegrationTest
{
  private static final Client CLIENT = new TransportClientAdapter(new HttpClientFactory().getClient(Collections.<String, String>emptyMap()));
  private static final String URI_PREFIX = "http://localhost:1338/";
  private static final RestClient REST_CLIENT = new RestClient(CLIENT, URI_PREFIX);
  private static final Exceptions2Builders EXCEPTIONS_2_BUILDERS = new Exceptions2Builders();

  @Test
  public void testGet() throws RemoteInvocationException
  {
    try
    {
      final GetRequest<Greeting> req = new Exceptions2Builders().get().id(1L).build();
      REST_CLIENT.sendRequest(req).getResponse();
      Assert.fail("Expected exception");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode());
      final DataMap respEntityMap = DataMapUtils.readMap(e.getResponse().getEntity().asInputStream());
      Assert.assertEquals(respEntityMap, new Greeting().setMessage("Hello, sorry for the mess").data());
    }
  }

  @Test
  public void testExceptionWithValue() throws RemoteInvocationException
  {
    final ActionRequest<Integer> req = EXCEPTIONS_2_BUILDERS.actionExceptionWithValue().build();
    try
    {
      REST_CLIENT.sendRequest(req).getResponse();
      Assert.fail("Expect exception");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode());
      final DataMap respEntityMap = DataMapUtils.readMap(e.getResponse().getEntity().asInputStream());
      Assert.assertSame(respEntityMap.getInteger("value"), 42);
    }
  }

  @Test
  public void testExceptionWithoutValue() throws RemoteInvocationException
  {
    final ActionRequest<Void> req = EXCEPTIONS_2_BUILDERS.actionExceptionWithoutValue().build();
    try
    {
      REST_CLIENT.sendRequest(req).getResponse();
      Assert.fail("Expect exception");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode());
    }
  }
}
