/*
   Copyright (c) 2016 LinkedIn Corp.

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

package com.linkedin.restli.server;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.linkedin.common.callback.Callback;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.template.StringMap;
import com.linkedin.parseq.Engine;
import com.linkedin.parseq.EngineBuilder;
import com.linkedin.parseq.promise.PromiseResolvedException;
import com.linkedin.parseq.promise.Promises;
import com.linkedin.parseq.promise.SettablePromise;
import com.linkedin.parseq.trace.Trace;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestMethod;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.restli.common.HttpMethod;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.multiplexer.IndividualRequest;
import com.linkedin.restli.common.multiplexer.IndividualRequestMap;
import com.linkedin.restli.common.multiplexer.MultiplexedRequestContent;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.server.RestLiConfig;
import com.linkedin.restli.server.RestLiServer;
import com.linkedin.restli.server.multiplexer.MultiplexerRunMode;
import com.linkedin.restli.server.resources.ResourceFactory;


public class TestAsyncMethodInvocationPlanClass
{

  private static final JacksonDataCodec CODEC = new JacksonDataCodec();

  @DataProvider(name = "multiplexerConfigurations")
  public Object[][] multiplexerConfigurations()
  {
   return new Object[][]
   {
     { MultiplexerRunMode.MULTIPLE_PLANS },
     { MultiplexerRunMode.SINGLE_PLAN }
   };
  }

  @DataProvider(name = "requestData")
  public Object[][] requestData()
  {
   return new Object[][]
   {
     { "/users/0", RestMethod.GET, "resource=users,method=get" },
     { "/users?action=register", RestMethod.POST, "resource=users,method=action,action=register" },
     { "/users?q=friends&userID=1", RestMethod.GET, "resource=users,method=finder,finder=friends" }
   };
  }


  @Test(dataProvider = "multiplexerConfigurations")
  public void testMultiplexedAsyncGet(MultiplexerRunMode multiplexerRunMode) throws URISyntaxException, IOException, InterruptedException
  {
    RestLiConfig config = new RestLiConfig();
    config.addResourcePackageNames("com.linkedin.restli.server.multiplexer.resources");
    config.setMultiplexerRunMode(multiplexerRunMode);
    SettablePromise<Trace> traceHolder = Promises.settable();

    Engine engine = engine(traceHolder);
    RestLiServer server = new RestLiServer(config, resourceFactory(), engine);

    IndividualRequest r0 = individualRequest("/users/0", null, Collections.<String, IndividualRequest>emptyMap());
    IndividualRequest r1 = individualRequest("/users/1", null, Collections.<String, IndividualRequest>emptyMap());
    IndividualRequest r2 = individualRequest("/users/2", null, ImmutableMap.of("0", r0, "1", r1));

    // request is seq(par(r0, r1), r2)
    RestRequest request = muxRestRequest(ImmutableMap.of("2", r2));

    CountDownLatch latch = new CountDownLatch(1);

    server.handleRequest(request, new RequestContext(), callback(latch));

    assertTrue(latch.await(5, TimeUnit.SECONDS));
    assertTrue(traceHolder.await(5, TimeUnit.SECONDS));

    if (multiplexerRunMode == MultiplexerRunMode.SINGLE_PLAN)
    {
      //For multiplexed requests in SINGLE_PLAN mode there is only one plan with class "mux"
      assertEquals(traceHolder.get().getPlanClass(), "mux");
    }
    else
    {
      //For multiplexed requests in MULTIPLE_PLANS mode there are multiple plans, first one is with class "mux",
      //following 3 are with class "resource=users,method=get", last one will have class "resource=users,method=get"
      assertEquals(traceHolder.get().getPlanClass(), "resource=users,method=get");
    }
  }

  @Test(dataProvider = "requestData")
  public void testAsyncGet(String uri, String method, String expectedPlanClass) throws URISyntaxException, IOException, InterruptedException
  {
    RestLiConfig config = new RestLiConfig();
    config.addResourcePackageNames("com.linkedin.restli.server.multiplexer.resources");
    SettablePromise<Trace> traceHolder = Promises.settable();

    Engine engine = engine(traceHolder);
    RestLiServer server = new RestLiServer(config, resourceFactory(), engine);

    RestRequest request = new RestRequestBuilder(new URI(uri))
        .setMethod(method)
        .setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, AllProtocolVersions.BASELINE_PROTOCOL_VERSION.toString()).build();

    CountDownLatch latch = new CountDownLatch(1);

    server.handleRequest(request, new RequestContext(), callback(latch));

    assertTrue(latch.await(5, TimeUnit.SECONDS));
    assertTrue(traceHolder.await(5, TimeUnit.SECONDS));

    assertTrue(latch.await(5, TimeUnit.SECONDS));
    assertTrue(traceHolder.await(5, TimeUnit.SECONDS));

    assertEquals(traceHolder.get().getPlanClass(), expectedPlanClass);
  }

  private Callback<RestResponse> callback(CountDownLatch latch)
  {
    return new Callback<RestResponse>()
    {
      @Override
      public void onSuccess(RestResponse result)
      {
        latch.countDown();
      }

      @Override
      public void onError(Throwable e)
      {
        e.printStackTrace();
        latch.countDown();
      }
    };
  }

  private static IndividualRequest individualRequest(String url, Map<String, String> headers, Map<String, IndividualRequest> dependentRequests)
  {
    IndividualRequest individualRequest = new IndividualRequest();
    individualRequest.setMethod(HttpMethod.GET.name());
    individualRequest.setRelativeUrl(url);
    if (headers != null && headers.size() > 0)
    {
      individualRequest.setHeaders(new StringMap(headers));
    }
    individualRequest.setDependentRequests(new IndividualRequestMap(dependentRequests));
    return individualRequest;
  }

  private RestRequest muxRestRequest(Map<String, IndividualRequest> requests) throws URISyntaxException, IOException
  {
    MultiplexedRequestContent content = new MultiplexedRequestContent();
    content.setRequests(new IndividualRequestMap(requests));
    return muxRequestBuilder()
        .setMethod(HttpMethod.POST.name())
        .setEntity(CODEC.mapToBytes(content.data()))
        .setHeader(RestConstants.HEADER_CONTENT_TYPE, RestConstants.HEADER_VALUE_APPLICATION_JSON)
        .build();
  }

  private RestRequestBuilder muxRequestBuilder() throws URISyntaxException
  {
    return new RestRequestBuilder(new URI("/mux"));
  }

  private Engine engine(SettablePromise<Trace> traceHolder)
  {
    ExecutorService taskScheduler = Executors.newFixedThreadPool(1);
    ScheduledExecutorService timerScheduler = Executors.newSingleThreadScheduledExecutor();
    return new EngineBuilder()
      .setTaskExecutor(taskScheduler)
      .setTimerScheduler(timerScheduler)
      .setPlanCompletionListener(planCtx -> {
        try {
          traceHolder.done(planCtx.getRootTask().getTrace());
        } catch (PromiseResolvedException e) {
          //this is expected in MULTIPLE_PLANS mux mode
        }
      })
      .build();
  }

  private ResourceFactory resourceFactory()
  {
    return new ResourceFactory()
    {
      @Override
      public void setRootResources(Map<String, ResourceModel> rootResources)
      {
      }
      @Override
      public <R> R create(Class<R> resourceClass)
      {
        try
        {
          return resourceClass.newInstance();
        }
        catch (InstantiationException e)
        {
          throw new RuntimeException(e);
        }
        catch (IllegalAccessException e)
        {
          throw new RuntimeException(e);
        }
      }
    };
  }
}
