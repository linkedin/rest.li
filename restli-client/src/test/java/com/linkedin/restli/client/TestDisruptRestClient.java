/*
   Copyright (c) 2017 LinkedIn Corp.

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

package com.linkedin.restli.client;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.Callbacks;
import com.linkedin.r2.disruptor.DisruptContext;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.restli.client.multiplexer.MultiplexedRequest;
import com.linkedin.restli.client.multiplexer.MultiplexedResponse;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.disruptor.DisruptRestController;
import org.junit.After;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;


/**
 * @author Sean Sheng
 */
@SuppressWarnings("unchecked")
public class TestDisruptRestClient
{
  public static final String DISRUPT_SOURCE_KEY = "R2_DISRUPT_SOURCE";
  public static final String DISRUPT_CONTEXT_KEY = "R2_DISRUPT_CONTEXT";

  private Client _underlying;
  private DisruptRestController _controller;
  private DisruptRestClient _client;
  private Request<Object> _request;
  private RequestContext _context;
  private RequestBuilder<Request<Object>> _builder;
  private Callback<Response<Object>> _callback;
  private Callback<MultiplexedResponse> _multiplexedCallback;
  private MultiplexedRequest _multiplexed;
  private DisruptContext _disrupt;
  private ErrorHandlingBehavior _behavior;

  @BeforeMethod
  public void doBeforeMethod()
  {
    _underlying = mock(Client.class);
    _controller = mock(DisruptRestController.class);
    _request = mock(Request.class);
    _context = mock(RequestContext.class);
    _builder = mock(RequestBuilder.class);
    _callback = mock(Callback.class);
    _multiplexedCallback = mock(Callback.class);
    _multiplexed = mock(MultiplexedRequest.class);
    _disrupt = mock(DisruptContext.class);

    _client = new DisruptRestClient(_underlying, _controller);
    _behavior = ErrorHandlingBehavior.FAIL_ON_ERROR;
  }

  @Test
  public void testShutdown()
  {
    _client.shutdown(Callbacks.empty());
    verify(_underlying, times(1)).shutdown(any(Callback.class));
  }

  @Test
  public void testSendRequest1()
  {
    when(_controller.getDisruptContext(any(String.class), any(ResourceMethod.class))).thenReturn(_disrupt);
    _client.sendRequest(_request);
    verify(_underlying, times(1)).sendRequest(eq(_request), any(RequestContext.class));
  }

  @Test
  public void testSendRequest2()
  {
    when(_controller.getDisruptContext(any(String.class), any(ResourceMethod.class))).thenReturn(_disrupt);
    _client.sendRequest(_request, _context);
    verify(_underlying, times(1)).sendRequest(eq(_request), eq(_context));
    verify(_context, times(1)).putLocalAttr(eq(DISRUPT_CONTEXT_KEY), eq(_disrupt));
    verify(_context, times(1)).putLocalAttr(eq(DISRUPT_SOURCE_KEY), any(String.class));
  }

  @Test
  public void testSendRequest3()
  {
    when(_controller.getDisruptContext(any(String.class), any(ResourceMethod.class))).thenReturn(_disrupt);
    _client.sendRequest(_request, _context, _behavior);
    verify(_underlying, times(1)).sendRequest(eq(_request), eq(_context), eq(_behavior));
    verify(_context, times(1)).putLocalAttr(eq(DISRUPT_CONTEXT_KEY), eq(_disrupt));
    verify(_context, times(1)).putLocalAttr(eq(DISRUPT_SOURCE_KEY), any(String.class));
  }

  @Test
  public void testSendRequest4()
  {
    when(_controller.getDisruptContext(any(String.class), any(ResourceMethod.class))).thenReturn(_disrupt);
    _client.sendRequest(_request, _behavior);
    verify(_underlying, times(1)).sendRequest(eq(_request), any(RequestContext.class), eq(_behavior));
  }

  @Test
  public void testSendRequest5()
  {
    when(_controller.getDisruptContext(any(String.class), any(ResourceMethod.class))).thenReturn(_disrupt);
    _client.sendRequest(_request, _callback);
    verify(_underlying, times(1)).sendRequest(eq(_request), any(RequestContext.class), eq(_callback));
  }

  @Test
  public void testSendRequest6()
  {
    when(_builder.build()).thenReturn(_request);
    when(_controller.getDisruptContext(any(String.class), any(ResourceMethod.class))).thenReturn(_disrupt);
    _client.sendRequest(_builder, _context);
    verify(_underlying, times(1)).sendRequest(eq(_request), eq(_context));
    verify(_context, times(1)).putLocalAttr(eq(DISRUPT_CONTEXT_KEY), eq(_disrupt));
    verify(_context, times(1)).putLocalAttr(eq(DISRUPT_SOURCE_KEY), any(String.class));
  }

  @Test
  public void testSendRequest7()
  {
    when(_builder.build()).thenReturn(_request);
    when(_controller.getDisruptContext(any(String.class), any(ResourceMethod.class))).thenReturn(_disrupt);
    _client.sendRequest(_builder, _context, _behavior);
    verify(_underlying, times(1)).sendRequest(eq(_request), eq(_context), eq(_behavior));
    verify(_context, times(1)).putLocalAttr(eq(DISRUPT_CONTEXT_KEY), eq(_disrupt));
    verify(_context, times(1)).putLocalAttr(eq(DISRUPT_SOURCE_KEY), any(String.class));
  }

  @Test
  public void testSendRequest8()
  {
    when(_builder.build()).thenReturn(_request);
    when(_controller.getDisruptContext(any(String.class), any(ResourceMethod.class))).thenReturn(_disrupt);
    _client.sendRequest(_builder, _callback);
    verify(_underlying, times(1)).sendRequest(eq(_request), any(RequestContext.class), eq(_callback));
  }

  @Test
  public void testSendRequest9()
  {
    when(_builder.build()).thenReturn(_request);
    when(_controller.getDisruptContext(any(String.class), any(ResourceMethod.class))).thenReturn(_disrupt);
    _client.sendRequest(_builder, _context, _callback);
    verify(_underlying, times(1)).sendRequest(eq(_request), eq(_context), eq(_callback));
    verify(_context, times(1)).putLocalAttr(eq(DISRUPT_CONTEXT_KEY), eq(_disrupt));
    verify(_context, times(1)).putLocalAttr(eq(DISRUPT_SOURCE_KEY), any(String.class));
  }

  @Test
  public void testSendRequest10()
  {
    when(_builder.build()).thenReturn(_request);
    when(_controller.getDisruptContext(any(String.class), any(ResourceMethod.class))).thenReturn(_disrupt);
    _client.sendRequest(_builder, _behavior);
    verify(_underlying, times(1)).sendRequest(eq(_request), any(RequestContext.class), eq(_behavior));
  }

  @Test
  public void testSendRequest11()
  {
    when(_controller.getDisruptContext(any(String.class))).thenReturn(_disrupt);
    _client.sendRequest(_multiplexed);
    verify(_underlying, times(1)).sendRequest(eq(_multiplexed), any(RequestContext.class), any(Callback.class));;
  }

  @Test
  public void testSendRequest12()
  {
    when(_controller.getDisruptContext(any(String.class))).thenReturn(_disrupt);
    _client.sendRequest(_multiplexed, _multiplexedCallback);
    verify(_underlying, times(1)).sendRequest(eq(_multiplexed), any(RequestContext.class), eq(_multiplexedCallback));
  }

  @Test
  public void testSendRequest13()
  {
    when(_controller.getDisruptContext(any(String.class))).thenReturn(_disrupt);
    _client.sendRequest(_multiplexed, _context, _multiplexedCallback);
    verify(_underlying, times(1)).sendRequest(eq(_multiplexed), eq(_context), eq(_multiplexedCallback));
    verify(_context, times(1)).putLocalAttr(eq(DISRUPT_CONTEXT_KEY), eq(_disrupt));
    verify(_context, times(1)).putLocalAttr(eq(DISRUPT_SOURCE_KEY), any(String.class));
  }

  @Test
  public void testDisruptSourceAlreadySet()
  {
    when(_context.getLocalAttr(eq(DISRUPT_SOURCE_KEY))).thenReturn(any(String.class));
    _client.sendRequest(_request, _context);
    verify(_context, never()).putLocalAttr(eq(DISRUPT_CONTEXT_KEY), any(String.class));
  }
}
