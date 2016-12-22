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

package com.linkedin.restli.server.multiplexer;

import com.linkedin.common.callback.Callback;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.template.StringMap;
import com.linkedin.parseq.CountingEngine;
import com.linkedin.parseq.DelayedExecutorAdapter;
import com.linkedin.parseq.Engine;
import com.linkedin.parseq.EngineBuilder;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.restli.common.HttpMethod;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.multiplexer.IndividualRequest;
import com.linkedin.restli.common.multiplexer.IndividualRequestMap;
import com.linkedin.restli.common.multiplexer.MultiplexedRequestContent;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.server.RestLiConfig;
import com.linkedin.restli.server.RestLiServer;
import com.linkedin.restli.server.resources.ResourceFactory;

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

import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.collections.Maps;

import com.google.common.collect.ImmutableMap;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


public class TestMultiplexerRunMode
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

  @Test(dataProvider = "multiplexerConfigurations")
  public void testMultiplexedAsyncGetRequest(MultiplexerRunMode multiplexerRunMode) throws URISyntaxException, IOException, InterruptedException
  {
    RestLiConfig config = new RestLiConfig();
    config.addResourcePackageNames("com.linkedin.restli.server.multiplexer.resources");
    config.setMultiplexerRunMode(multiplexerRunMode);

    CountingEngine engine = engine();
    RestLiServer server = new RestLiServer(config, resourceFactory(), engine);

    IndividualRequest r0 = individualRequest("/users/0", null, Collections.<String, IndividualRequest>emptyMap());
    IndividualRequest r1 = individualRequest("/users/1", null, Collections.<String, IndividualRequest>emptyMap());
    IndividualRequest r2 = individualRequest("/users/2", null, ImmutableMap.of("0", r0, "1", r1));

    // request is seq(par(r0, r1), r2)
    RestRequest request = muxRestRequest(ImmutableMap.of("2", r2));

    CountDownLatch latch = new CountDownLatch(1);

    server.handleRequest(request, new RequestContext(), callback(latch));

    assertTrue(latch.await(5, TimeUnit.SECONDS));

    if (multiplexerRunMode == MultiplexerRunMode.SINGLE_PLAN)
    {
      assertEquals(engine.plansStarted(), 1);
    }
    else
    {
      // in MULTIPLE_PLANS mode: 1 task for multiplexed request itself + 3 individual tasks r0, r1, r2
      assertEquals(engine.plansStarted(), 1 + 3);
    }
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

  private CountingEngine engine()
  {
    ExecutorService taskScheduler = Executors.newFixedThreadPool(1);
    ScheduledExecutorService timerScheduler = Executors.newSingleThreadScheduledExecutor();
    CountingEngine countingEngine = new CountingEngine(taskScheduler, new DelayedExecutorAdapter(timerScheduler), LoggerFactory.getILoggerFactory(), Maps.newHashMap());
    return countingEngine;
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
