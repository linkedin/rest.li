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

/**
 * $Id: $
 */

package com.linkedin.d2.contrib;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.D2Client;
import com.linkedin.d2.balancer.D2ClientBuilder;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author David Hoa
 * @version $Revision: $
 */

public class TestRouteLookupClient
{

  @Test
  public void testSimpleRouteLookup() throws ExecutionException, InterruptedException
  {
    RouteLookup routeLookup = new SimpleTestRouteLookup();

    FutureCallback<String> futureCallback = new FutureCallback<String>();
    String serviceName = "BarBar";
    routeLookup.run(serviceName, null, "1",futureCallback);

    String resultString = futureCallback.get();
    Assert.assertEquals(resultString, serviceName + "1" + "Foo");

    futureCallback = new FutureCallback<String>();
    routeLookup.run(serviceName, "blah", "2",futureCallback);
    resultString = futureCallback.get();
    Assert.assertEquals(resultString, serviceName + "blah" + "2" + "Foo");
  }

  @Test
  public void testRouteLookupClientFuture() throws ExecutionException, InterruptedException
  {
    RouteLookup routeLookup = new SimpleTestRouteLookup();

    final D2Client d2Client = new D2ClientBuilder().build();
    d2Client.start(new FutureCallback<None>());
    RouteLookupClient routeLookupClient = new RouteLookupClient(d2Client, routeLookup, "WestCoast");
    RestRequest dummyRestRequest = new RestRequestBuilder(URI.create("d2://simple_uri")).build();
    Future future = routeLookupClient.restRequest(dummyRestRequest, "5436");
    try
    {
      future.get();
      // the request shouldn't succeed because we haven't set up a server or any service -> cluster -> uri
      // mapping; we want it to fail because we can get the service name we tried to get at from the
      // ServiceUnavailableException that is thrown.
      Assert.fail("Unexpected success, request should have thrown a ServiceUnavailableException");
    }
    catch (Exception e)
    {
      String message = e.getMessage();
      if (!message.contains("_serviceName=simple_uriWestCoast5436Foo"))
      {
        Assert.fail("request was not rewritten to point at the d2 service simple_uriWestCoast5436Foo");
      }
    }
  }

  @Test
  public void testRouteLookupClientCallback()
    throws InterruptedException, ExecutionException, TimeoutException
  {
    RouteLookup routeLookup = new SimpleTestRouteLookup();

    final D2Client d2Client = new D2ClientBuilder().build();
    d2Client.start(new FutureCallback<None>());
    RouteLookupClient routeLookupClient = new RouteLookupClient(d2Client, routeLookup, "WestCoast");
    RestRequest dummyRestRequest = new RestRequestBuilder(URI.create("d2://simple_uri")).build();
    FutureCallback<RestResponse> futureCallback = new FutureCallback<RestResponse>();
    routeLookupClient.restRequest(dummyRestRequest,futureCallback, "5555");

    try
    {
      RestResponse response = futureCallback.get(10, TimeUnit.SECONDS);
      Assert.fail("Unexpected success, request should have thrown a ServiceUnavailableException");
    }
    catch (Exception e)
    {
      String message = e.getMessage();
      if (!message.contains("_serviceName=simple_uriWestCoast5555Foo"))
      {
        Assert.fail("request was not rewritten to point at the d2 service simple_uriWestCoast5555Foo");
      }
    }
  }

  @Test
  public void testBadRequest()
  {
    RouteLookup routeLookup = new SimpleTestRouteLookup();

    final D2Client d2Client = new D2ClientBuilder().build();
    d2Client.start(new FutureCallback<None>());
    RouteLookupClient routeLookupClient = new RouteLookupClient(d2Client, routeLookup, "WestCoast");
    RestRequest dummyRestRequest = new RestRequestBuilder(URI.create("http://simple_uri")).build();
    try
    {
      Future future = routeLookupClient.restRequest(dummyRestRequest, "5436");
      future.get();
      Assert.fail("Unexpected success, request should have thrown an Exception");
    }
    catch (Exception e)
    {
      Assert.assertTrue(e instanceof IllegalArgumentException);
      String message = e.getMessage();
      if (!message.contains("Unsupported scheme in URI: http://simple_uri"))
      {
        Assert.fail("request was sent using http instead of d2, but we didn't get Unsupported scheme");
      }
    }
  }

}
