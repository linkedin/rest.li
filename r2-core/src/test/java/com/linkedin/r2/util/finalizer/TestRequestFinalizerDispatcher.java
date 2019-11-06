/*
   Copyright (c) 2019 LinkedIn Corp.

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

package com.linkedin.r2.util.finalizer;

import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.Response;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.entitystream.EntityStream;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponse;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Tests for {@link RequestFinalizerDispatcher}.
 *
 * @author Chris Zhang
 */
public class TestRequestFinalizerDispatcher
{
  private AtomicInteger _index;
  private TestRequestFinalizer _requestFinalizer;
  private TestTransportDispatcher _innerDispatcher;
  private TestTransportDispatcher _outerDispatcher;

  @Mock
  private TransportResponse<RestResponse> _restTransportResponse;
  @Mock
  private TransportResponse<StreamResponse> _streamTransportResponse;
  @Mock
  private RestResponse _restResponse;
  @Mock
  private StreamResponse _streamResponse;
  @Mock
  private EntityStream _entityStream;

  @BeforeMethod
  public void setup()
  {
    MockitoAnnotations.initMocks(this);

    _index = new AtomicInteger(0);

    _requestFinalizer = new TestRequestFinalizer();
    _innerDispatcher = new TestTransportDispatcher(_requestFinalizer);
    _outerDispatcher = new TestTransportDispatcher(new RequestFinalizerDispatcher(_innerDispatcher));
  }

  @DataProvider
  public Object[][] throwTransportCallbackException()
  {
    return new Object[][] {{false}, {true}};
  }

  @Test(dataProvider = "throwTransportCallbackException")
  public void testHandleRestRequestOrdering(boolean throwTransportCallbackException)
  {
    when(_restTransportResponse.getResponse())
        .thenReturn(_restResponse);

    final TestTransportCallback<RestResponse> transportCallback = new TestTransportCallback<>(throwTransportCallbackException);
    _outerDispatcher.handleRestRequest(null, null, new RequestContext(), transportCallback);

    Assert.assertEquals(_outerDispatcher._executionOrder, 1);
    Assert.assertEquals(_innerDispatcher._executionOrder, 2);
    Assert.assertEquals(_innerDispatcher._transportCallback._executionOrder, 3);
    Assert.assertEquals(_outerDispatcher._transportCallback._executionOrder, 4);
    Assert.assertEquals(transportCallback._executionOrder, 5);
    Assert.assertEquals(_requestFinalizer._executionOrder, 6, "Expected request to be finalized after the callback.");
  }

  @Test(dataProvider = "throwTransportCallbackException")
  public void testHandleStreamRequestOrdering(boolean throwTransportCallbackException)
  {
    when(_streamTransportResponse.getResponse())
        .thenReturn(_streamResponse);
    when(_streamResponse.getEntityStream())
        .thenReturn(_entityStream);

    final TestTransportCallback<StreamResponse> transportCallback = new TestTransportCallback<>(throwTransportCallbackException);
    _outerDispatcher.handleStreamRequest(null, null, new RequestContext(), transportCallback);

    Assert.assertEquals(_outerDispatcher._executionOrder, 1);
    Assert.assertEquals(_innerDispatcher._executionOrder, 2);
    Assert.assertEquals(_innerDispatcher._transportCallback._executionOrder, 3);
    Assert.assertEquals(_outerDispatcher._transportCallback._executionOrder, 4);
    Assert.assertEquals(transportCallback._executionOrder, 5);
    verify(_entityStream).addObserver(any());
    if (throwTransportCallbackException)
    {
      Assert.assertEquals(_requestFinalizer._executionOrder, 6, "Expected request to be finalized after the callback threw an exception.");
    }
  }

  @Test
  public void testExistingRequestFinalizerManager()
  {
    when(_restTransportResponse.getResponse())
        .thenReturn(_restResponse);

    final RequestContext requestContext = new RequestContext();
    final RequestFinalizerManagerImpl manager = new RequestFinalizerManagerImpl(null, requestContext);

    final AtomicBoolean atomicBoolean = new AtomicBoolean(false);
    manager.registerRequestFinalizer((request, response ,requestContext1, throwable) -> atomicBoolean.set(true));

    requestContext.putLocalAttr(R2Constants.SERVER_REQUEST_FINALIZER_MANAGER_REQUEST_CONTEXT_KEY, manager);

    final TestTransportCallback<RestResponse> transportCallback = new TestTransportCallback<>(false);
    _outerDispatcher.handleRestRequest(null, null, requestContext, transportCallback);

    Assert.assertEquals(_outerDispatcher._executionOrder, 1);
    Assert.assertEquals(_innerDispatcher._executionOrder, 2);
    Assert.assertEquals(_innerDispatcher._transportCallback._executionOrder, 3);
    Assert.assertEquals(_outerDispatcher._transportCallback._executionOrder, 4);
    Assert.assertEquals(transportCallback._executionOrder, 5);
    Assert.assertEquals(_requestFinalizer._executionOrder, 6, "Expected request to be finalized after the callback.");
    Assert.assertTrue(atomicBoolean.get(), "Expected the request finalizer registered before reaching the"
        + "RequestFinalizerDispatcher to still be invoked.");
  }

  private class TestRequestFinalizer implements RequestFinalizer
  {
    private int _executionOrder;

    @Override
    public void finalizeRequest(Request request, Response response, RequestContext requestContext, Throwable error)
    {
      _executionOrder = _index.incrementAndGet();
    }
  }

  private class TestTransportCallback<T> implements TransportCallback<T>
  {
    private final TransportCallback<T> _transportCallback;

    private int _executionOrder;
    private boolean _throwException = false;

    private TestTransportCallback(boolean throwException)
    {
      _transportCallback = null;
      _throwException = throwException;
    }

    private TestTransportCallback(TransportCallback<T> transportCallback)
    {
      _transportCallback = transportCallback;
    }

    @Override
    public void onResponse(TransportResponse<T> response)
    {
      _executionOrder = _index.incrementAndGet();
      if (_transportCallback != null)
      {
        _transportCallback.onResponse(response);
      }
      else if (_throwException)
      {
        throw new RuntimeException("Expected exception.");
      }
    }
  }

  @SuppressWarnings({"unchecked", "ConstantConditions"})
  private class TestTransportDispatcher implements TransportDispatcher
  {
    private final TransportDispatcher _transportDispatcher;
    private final TestRequestFinalizer _requestFinalizer;

    private int _executionOrder;
    private TestTransportCallback<?> _transportCallback;

    /**
     * Constructor for innermost TransportDispatcher. Will register a {@link TestRequestFinalizer} and invoke callback
     * when handling request.
     */
    private TestTransportDispatcher(TestRequestFinalizer requestFinalizer)
    {
      _transportDispatcher = null;
      _requestFinalizer = requestFinalizer;
    }

    /**
     * Constructor for an outer TransportDispatcher. Will invoke decorated TransportDispatcher when handling request.
     *
     * @param transportDispatcher TransportDispatcher to decorate.
     */
    private TestTransportDispatcher(TransportDispatcher transportDispatcher)
    {
      _transportDispatcher = transportDispatcher;
      _requestFinalizer = null;
    }

    @Override
    public void handleRestRequest(RestRequest req, Map<String, String> wireAttrs, RequestContext requestContext,
        TransportCallback<RestResponse> callback)
    {
      _transportCallback = new TestTransportCallback<>(callback);

      handleRequest(requestContext, (TransportCallback<RestResponse>) _transportCallback, _restTransportResponse,
          () -> _transportDispatcher.handleRestRequest(req, wireAttrs, requestContext, (TransportCallback<RestResponse>) _transportCallback));
    }

    @Override
    public void handleStreamRequest(StreamRequest req, Map<String, String> wireAttrs,
        RequestContext requestContext, TransportCallback<StreamResponse> callback)
    {
      _transportCallback = new TestTransportCallback<>(callback);

      handleRequest(requestContext, (TransportCallback<StreamResponse>) _transportCallback, _streamTransportResponse,
          () -> _transportDispatcher.handleStreamRequest(req, wireAttrs, requestContext, (TransportCallback<StreamResponse>) _transportCallback));
    }

    private <T> void handleRequest(RequestContext requestContext, TransportCallback<T> callback, TransportResponse<T> transportResponse, Runnable requestHandler)
    {
      _executionOrder = _index.incrementAndGet();

      if (_transportDispatcher == null)
      {
        RequestFinalizerManager manager =
            (RequestFinalizerManager) requestContext.getLocalAttr(R2Constants.SERVER_REQUEST_FINALIZER_MANAGER_REQUEST_CONTEXT_KEY);
        manager.registerRequestFinalizer(_requestFinalizer);

        callback.onResponse(transportResponse);
      }
      else
      {
        requestHandler.run();
      }
    }
  }
}
