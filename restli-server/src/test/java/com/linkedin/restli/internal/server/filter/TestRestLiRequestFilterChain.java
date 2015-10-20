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

package com.linkedin.restli.internal.server.filter;


import com.linkedin.restli.server.RestLiRequestData;
import com.linkedin.restli.server.filter.FilterRequestContext;
import com.linkedin.restli.server.filter.RequestFilter;
import com.linkedin.restli.server.filter.NextRequestFilter;

import java.util.ArrayList;
import java.util.Arrays;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


/**
 * @author nshankar
 */
public class TestRestLiRequestFilterChain
{
  @Mock
  RestLiRequestData _mockRestLiRequestData;
  @Mock
  private RestLiRequestFilterChainCallback _mockRestLiRequestFilterChainCallback;
  @Mock
  private FilterRequestContext _mockFilterRequestContext;
  @Mock
  private RequestFilter _mockFilter;
  private RestLiRequestFilterChain _restLiRequestFilterChain;

  @BeforeClass
  protected void setUp()
  {
    MockitoAnnotations.initMocks(this);
  }

  @BeforeMethod
  protected void init()
  {
    _restLiRequestFilterChain = new RestLiRequestFilterChain(Arrays.asList(_mockFilter),
                                                                 _mockRestLiRequestFilterChainCallback);
  }

  @AfterMethod
  protected void resetMocks()
  {
    reset(_mockRestLiRequestFilterChainCallback, _mockFilterRequestContext, _mockRestLiRequestData, _mockFilter);
  }

  @Test
  public void testFilterInvocationSuccess() throws Exception
  {
    doAnswer(new Answer<Object>()
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        FilterRequestContext requestContext = (FilterRequestContext) args[0];
        NextRequestFilter nextRequestFilter = (NextRequestFilter) args[1];
        nextRequestFilter.onRequest(requestContext);
        return null;
      }
    }).when(_mockFilter).onRequest(_mockFilterRequestContext, _restLiRequestFilterChain);
    when(_mockFilterRequestContext.getRequestData()).thenReturn(_mockRestLiRequestData);

    _restLiRequestFilterChain.onRequest(_mockFilterRequestContext);

    verify(_mockFilter).onRequest(_mockFilterRequestContext, _restLiRequestFilterChain);
    verify(_mockFilterRequestContext).getRequestData();
    verify(_mockRestLiRequestFilterChainCallback).onSuccess(_mockRestLiRequestData);
    verifyNoMoreInteractions(_mockRestLiRequestFilterChainCallback,
                             _mockFilterRequestContext,
                             _mockRestLiRequestData,
                             _mockFilter);
  }

  @Test
  public void testFilterInvocationFailure() throws Exception
  {
    RuntimeException e = new RuntimeException("Exception from filter!");
    doThrow(e).when(_mockFilter).onRequest(_mockFilterRequestContext, _restLiRequestFilterChain);

    _restLiRequestFilterChain.onRequest(_mockFilterRequestContext);

    verify(_mockFilter).onRequest(_mockFilterRequestContext, _restLiRequestFilterChain);
    verify(_mockRestLiRequestFilterChainCallback).onError(e);
    verifyNoMoreInteractions(_mockRestLiRequestFilterChainCallback,
                             _mockFilterRequestContext,
                             _mockRestLiRequestData,
                             _mockFilter);
  }

  @Test
  public void testNoFilters() throws Exception
  {
    NextRequestFilter emptyFilterChain = new RestLiRequestFilterChain(new ArrayList<RequestFilter>(),
                                                                                 _mockRestLiRequestFilterChainCallback);
    when(_mockFilterRequestContext.getRequestData()).thenReturn(_mockRestLiRequestData);

    emptyFilterChain.onRequest(_mockFilterRequestContext);

    verify(_mockFilterRequestContext).getRequestData();
    verify(_mockRestLiRequestFilterChainCallback).onSuccess(_mockRestLiRequestData);
    verifyNoMoreInteractions(_mockRestLiRequestFilterChainCallback,
                             _mockFilterRequestContext,
                             _mockRestLiRequestData,
                             _mockFilter);
  }
}
