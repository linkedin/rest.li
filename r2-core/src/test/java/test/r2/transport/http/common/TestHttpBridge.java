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

package test.r2.transport.http.common;

import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import com.linkedin.r2.message.Messages;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.message.stream.StreamException;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.StreamResponseBuilder;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.transport.common.bridge.client.TransportCallbackAdapter;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;
import com.linkedin.r2.transport.http.common.HttpBridge;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public class TestHttpBridge
{
  @Test
  public void testRestToHttpErrorMessage() throws TimeoutException, InterruptedException
  {
    URI uri = URI.create("http://some.host/thisShouldAppearInTheErrorMessage");

    RestRequest r = new RestRequestBuilder(uri).build();

    FutureCallback<RestResponse> futureCallback = new FutureCallback<RestResponse>();
    TransportCallback<RestResponse> callback = new TransportCallbackAdapter<RestResponse>(futureCallback);
    TransportCallback<RestResponse> bridgeCallback = HttpBridge.restToHttpCallback(callback, r);

    bridgeCallback.onResponse(TransportResponseImpl.<RestResponse>error(new Exception()));

    try
    {
      futureCallback.get(30, TimeUnit.SECONDS);
      Assert.fail("get should have thrown exception");
    }
    catch (ExecutionException e)
    {
      Assert.assertTrue(e.getCause().getMessage().contains(uri.toString()));
    }

  }

  @Test
  public void testHttpToRestErrorMessage() throws TimeoutException, InterruptedException, ExecutionException
  {
    FutureCallback<RestResponse> futureCallback = new FutureCallback<RestResponse>();
    TransportCallback<RestResponse> callback =
        new TransportCallbackAdapter<RestResponse>(futureCallback);
    TransportCallback<RestResponse> bridgeCallback = HttpBridge.httpToRestCallback(callback);

    RestResponse restResponse = new RestResponseBuilder().build();

    // Note: FutureCallback will fail if called twice. An exception would be raised on the current
    // thread because we begin the callback sequence here in onResponse.
    // (test originally added due to bug with double callback invocation)
    bridgeCallback.onResponse(TransportResponseImpl.<RestResponse> error(new RestException(restResponse)));

    RestResponse resp = futureCallback.get(30, TimeUnit.SECONDS);
    // should have unpacked restResponse from the RestException that we passed in without
    // propagating the actual exception
    Assert.assertSame(resp, restResponse);
  }

  @Test
  public void testStreamToHttpErrorMessage() throws TimeoutException, InterruptedException
  {
    URI uri = URI.create("http://some.host/thisShouldAppearInTheErrorMessage");

    RestRequest r = new RestRequestBuilder(uri).build();

    FutureCallback<StreamResponse> futureCallback = new FutureCallback<StreamResponse>();
    TransportCallback<StreamResponse> callback = new TransportCallbackAdapter<StreamResponse>(futureCallback);
    TransportCallback<StreamResponse> bridgeCallback = HttpBridge.streamToHttpCallback(callback,
        Messages.toStreamRequest(r));

    bridgeCallback.onResponse(TransportResponseImpl.<StreamResponse>error(new Exception()));

    try
    {
      futureCallback.get(30, TimeUnit.SECONDS);
      Assert.fail("get should have thrown exception");
    }
    catch (ExecutionException e)
    {
      Assert.assertTrue(e.getCause().getMessage().contains(uri.toString()));
    }

  }

  @Test
  public void testHttpToStreamErrorMessage() throws TimeoutException, InterruptedException, ExecutionException
  {
    FutureCallback<StreamResponse> futureCallback = new FutureCallback<StreamResponse>();
    TransportCallback<StreamResponse> callback =
        new TransportCallbackAdapter<StreamResponse>(futureCallback);
    TransportCallback<StreamResponse> bridgeCallback = HttpBridge.httpToStreamCallback(callback);

    StreamResponse streamResponse = new StreamResponseBuilder().build(EntityStreams.emptyStream());

    // Note: FutureCallback will fail if called twice. An exception would be raised on the current
    // thread because we begin the callback sequence here in onResponse.
    // (test originally added due to bug with double callback invocation)
    bridgeCallback.onResponse(TransportResponseImpl.<StreamResponse> error(new StreamException(streamResponse)));

    StreamResponse resp = futureCallback.get(30, TimeUnit.SECONDS);
    // should have unpacked restResponse from the RestException that we passed in without
    // propagating the actual exception
    Assert.assertSame(resp, streamResponse);
  }
}
