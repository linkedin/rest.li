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


import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.filter.FilterRequestContext;
import com.linkedin.restli.server.filter.FilterResponseContext;
import com.linkedin.restli.server.filter.NextResponseFilter;
import com.linkedin.restli.server.filter.ResponseFilter;

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
public class TestRestLiResponseFilterChain
{
  @Mock
  private RestLiResponseFilterContextFactory<Object> _mockResponseFilterContextFactory;
  @Mock
  private RestLiResponseFilterChainCallback _mockRestLiResponseFilterChainCallback;
  @Mock
  private FilterRequestContext _mockFilterRequestContext;
  @Mock
  private FilterResponseContext _mockFilterResponseContext;
  @Mock
  private RestLiResponseData _restLiResponseData;
  @Mock
  private ResponseFilter _mockFilter;
  private RestLiResponseFilterChain _restLiRestLiResponseFilterChain;

  @BeforeClass
  protected void setUp()
  {
    MockitoAnnotations.initMocks(this);
  }

  @BeforeMethod
  protected void init()
  {
    _restLiRestLiResponseFilterChain = new RestLiResponseFilterChain(Arrays.asList(_mockFilter),
                                                                   _mockResponseFilterContextFactory,
                                                                   _mockRestLiResponseFilterChainCallback);
  }

  @AfterMethod
  protected void resetMocks()
  {
    reset(_mockFilter,
          _mockFilterRequestContext,
          _mockFilterResponseContext,
          _mockResponseFilterContextFactory,
          _mockRestLiResponseFilterChainCallback,
          _restLiResponseData);
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
        FilterResponseContext responseContext = (FilterResponseContext) args[1];
        NextResponseFilter nextResponseFilter = (NextResponseFilter) args[2];
        nextResponseFilter.onResponse(requestContext, responseContext);
        return null;
      }
    }).when(_mockFilter).onResponse(_mockFilterRequestContext, _mockFilterResponseContext,
                                    _restLiRestLiResponseFilterChain);

    when(_mockFilterResponseContext.getResponseData()).thenReturn(_restLiResponseData);

    _restLiRestLiResponseFilterChain.onResponse(_mockFilterRequestContext, _mockFilterResponseContext);

    verify(_mockFilter).onResponse(_mockFilterRequestContext, _mockFilterResponseContext,
                                   _restLiRestLiResponseFilterChain);
    verify(_mockFilterResponseContext).getResponseData();
    verify(_mockRestLiResponseFilterChainCallback).onCompletion(_restLiResponseData);
    verifyNoMoreInteractions(_mockFilter,
                             _mockFilterRequestContext,
                             _mockFilterResponseContext,
                             _mockResponseFilterContextFactory,
                             _mockRestLiResponseFilterChainCallback,
                             _restLiResponseData);
  }

  @Test
  public void testFilterInvocationFailure() throws Exception
  {
    RuntimeException e = new RuntimeException("Exception from filter!");
    doThrow(e).when(_mockFilter)
        .onResponse(_mockFilterRequestContext, _mockFilterResponseContext, _restLiRestLiResponseFilterChain);
    when(_mockFilterResponseContext.getResponseData()).thenReturn(_restLiResponseData);
    when(_mockResponseFilterContextFactory.fromThrowable(e)).thenReturn(_mockFilterResponseContext);

    _restLiRestLiResponseFilterChain.onResponse(_mockFilterRequestContext, _mockFilterResponseContext);

    verify(_mockFilter).onResponse(_mockFilterRequestContext, _mockFilterResponseContext,
                                   _restLiRestLiResponseFilterChain);
    verify(_mockFilterResponseContext).getResponseData();
    verify(_mockRestLiResponseFilterChainCallback).onCompletion(_restLiResponseData);
    verify(_mockResponseFilterContextFactory).fromThrowable(e);
    verifyNoMoreInteractions(_mockFilter,
                             _mockFilterRequestContext,
                             _mockFilterResponseContext,
                             _mockResponseFilterContextFactory,
                             _mockRestLiResponseFilterChainCallback,
                             _restLiResponseData);
  }

  @Test
  public void testNoFilters() throws Exception
  {
    _restLiRestLiResponseFilterChain = new RestLiResponseFilterChain(new ArrayList<ResponseFilter>(),
                                                                   _mockResponseFilterContextFactory,
                                                                   _mockRestLiResponseFilterChainCallback);
    when(_mockFilterResponseContext.getResponseData()).thenReturn(_restLiResponseData);

    _restLiRestLiResponseFilterChain.onResponse(_mockFilterRequestContext, _mockFilterResponseContext);

    verify(_mockFilterResponseContext).getResponseData();
    verify(_mockRestLiResponseFilterChainCallback).onCompletion(_restLiResponseData);
    verifyNoMoreInteractions(_mockFilter,
                             _mockFilterRequestContext,
                             _mockFilterResponseContext,
                             _mockResponseFilterContextFactory,
                             _mockRestLiResponseFilterChainCallback,
                             _restLiResponseData);
  }
}
