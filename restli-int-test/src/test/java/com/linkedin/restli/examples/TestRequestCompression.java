/*
   Copyright (c) 2014 LinkedIn Corp.

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


import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.filter.CompressionConfig;
import com.linkedin.r2.filter.CompressionOption;
import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.filter.compression.ServerCompressionFilter;
import com.linkedin.r2.filter.logging.SimpleLoggingFilter;
import com.linkedin.r2.filter.message.rest.RestFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.AbstractJmxManager;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.r2.transport.http.common.HttpConstants;
import com.linkedin.r2.util.NamedThreadFactory;
import com.linkedin.restli.client.ProtocolVersionOption;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.ResponseFuture;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.client.RestliRequestOptions;
import com.linkedin.restli.client.RestliRequestOptionsBuilder;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.client.GreetingsRequestBuilders;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.test.util.RootBuilderWrapper;

import com.linkedin.test.util.retry.ThreeRetries;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import io.netty.channel.nio.NioEventLoopGroup;

/**
 * Integration tests for request compression.
 *
 * Tests that the client sends a compressed request when appropriate,
 * and the server decompresses the request successfully and sends back the correct response.
 *
 * @author Soojung Ha
 */
public class TestRequestCompression extends RestLiIntegrationTest
{
  // Header for sending test information to the server. The value will be set to either EXPECT_COMPRESSION or EXPECT_NO_COMPRESSION.
  private static final String TEST_HELP_HEADER = "Test-Help-Header";
  private static final String EXPECT_COMPRESSION = "Expect-Compression";
  private static final String EXPECT_NO_COMPRESSION = "Expect-No-Compression";
  private static final String SERVICE_NAME = "service1";

  @BeforeClass
  public void initClass() throws Exception
  {
    class CheckRequestCompressionFilter implements RestFilter
    {
      @Override
      public void onRestRequest(RestRequest req,
                                RequestContext requestContext,
                                Map<String, String> wireAttrs,
                                NextFilter<RestRequest, RestResponse> nextFilter)
      {
        Map<String, String> requestHeaders = req.getHeaders();
        if (requestHeaders.containsKey(TEST_HELP_HEADER))
        {
          String contentEncodingHeader = requestHeaders.get(HttpConstants.CONTENT_ENCODING);
          if (requestHeaders.get(TEST_HELP_HEADER).equals(EXPECT_COMPRESSION))
          {
            if (contentEncodingHeader == null)
            {
              throw new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST, "Request is not compressed when it should be.");
            }
            else if (!contentEncodingHeader.equals("x-snappy-framed"))
            {
              // Request should be compressed with the first encoding the client can compress with,
              // which is always snappy in this test.
              throw new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST,
                                               "Request is compressed with " + contentEncodingHeader + " instead of x-snappy-framed.");
            }
          }
          else
          {
            if (contentEncodingHeader != null)
            {
              throw new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST, "Request is compressed when it shouldn't be.");
            }
          }
        }
        nextFilter.onRequest(req, requestContext, wireAttrs);
      }
    }

    // Check that Content-Encoding and Content-Length headers are set correctly by ServerCompressionFilter.
    class CheckHeadersFilter implements RestFilter
    {
      @Override
      public void onRestRequest(RestRequest req,
                                RequestContext requestContext,
                                Map<String, String> wireAttrs,
                                NextFilter<RestRequest, RestResponse> nextFilter)
      {
        if (req.getHeaders().containsKey(HttpConstants.CONTENT_ENCODING))
        {
          throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR, "Content-Encoding header not removed.");
        }
        if (req.getEntity().length() != Integer.parseInt(req.getHeader(HttpConstants.CONTENT_LENGTH)))
        {
          throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR, "Content-Length header incorrect.");
        }
        nextFilter.onRequest(req, requestContext, wireAttrs);
      }
    }

    final FilterChain fc = FilterChains.empty().addLastRest(new CheckRequestCompressionFilter())
        .addLastRest(new ServerCompressionFilter(RestLiIntTestServer.supportedCompression))
        .addLastRest(new CheckHeadersFilter())
        .addLastRest(new SimpleLoggingFilter());
    super.init(null, fc, false);
  }

  @AfterClass
  public void shutDown() throws Exception
  {
    super.shutdown();
  }

  @DataProvider(name = "requestData")
  private Object[][] requestData()
  {
    int tiny = 10;
    int small = 100;
    int large = 1000;
    int huge = 10000;

    CompressionConfig tinyThresholdConfig = new CompressionConfig(tiny);
    CompressionConfig hugeThresholdConfig = new CompressionConfig(huge);

    String encodings = "unsupportedEncoding, x-snappy-framed, snappy, gzip";

    RestliRequestOptions forceOnOption = new RestliRequestOptionsBuilder().setProtocolVersionOption(ProtocolVersionOption.USE_LATEST_IF_AVAILABLE)
        .setRequestCompressionOverride(CompressionOption.FORCE_ON).build();
    RestliRequestOptions forceOffOption = new RestliRequestOptionsBuilder().setProtocolVersionOption(ProtocolVersionOption.USE_LATEST_IF_AVAILABLE)
        .setRequestCompressionOverride(CompressionOption.FORCE_OFF).build();

    return new Object[][] {
        // Compression depending on request size
        {null, encodings, RestliRequestOptions.DEFAULT_OPTIONS, large, EXPECT_COMPRESSION},
        {null, encodings, RestliRequestOptions.DEFAULT_OPTIONS, small, EXPECT_NO_COMPRESSION},

        // Override the default threshold and cause even small requests to be compressed
        {tinyThresholdConfig, encodings, RestliRequestOptions.DEFAULT_OPTIONS, large, EXPECT_COMPRESSION},
        {tinyThresholdConfig, encodings, RestliRequestOptions.DEFAULT_OPTIONS, small, EXPECT_COMPRESSION},

        // Override the default threshold and causes even large requests to be NOT compressed.
        {hugeThresholdConfig, encodings, RestliRequestOptions.DEFAULT_OPTIONS, large, EXPECT_NO_COMPRESSION},
        {hugeThresholdConfig, encodings, RestliRequestOptions.DEFAULT_OPTIONS, small, EXPECT_NO_COMPRESSION},

        // Force on/off using RestliRequestOptions
        {null, encodings, forceOnOption, large, EXPECT_COMPRESSION},
        {null, encodings, forceOnOption, small, EXPECT_COMPRESSION},
        {hugeThresholdConfig, encodings, forceOnOption, small, EXPECT_COMPRESSION},
        {null, encodings, forceOffOption, large, EXPECT_NO_COMPRESSION},
        {null, encodings, forceOffOption, small, EXPECT_NO_COMPRESSION},
        {tinyThresholdConfig, encodings, forceOffOption, large, EXPECT_NO_COMPRESSION},

        // Force on/off using RequestCompressionConfig
        {new CompressionConfig(0), encodings, RestliRequestOptions.DEFAULT_OPTIONS, large, EXPECT_COMPRESSION},
        {new CompressionConfig(0), encodings, RestliRequestOptions.DEFAULT_OPTIONS, small, EXPECT_COMPRESSION},
        {new CompressionConfig(Integer.MAX_VALUE), encodings, RestliRequestOptions.DEFAULT_OPTIONS, large, EXPECT_NO_COMPRESSION},
        {new CompressionConfig(Integer.MAX_VALUE), encodings, RestliRequestOptions.DEFAULT_OPTIONS, small, EXPECT_NO_COMPRESSION},

        // RestliRequestOptions takes precedence over RequestCompressionConfig
        {new CompressionConfig(0), encodings, forceOffOption, large, EXPECT_NO_COMPRESSION},
        {new CompressionConfig(Integer.MAX_VALUE), encodings, forceOnOption, small, EXPECT_COMPRESSION},

        // Can't compress if no encodings are available
        {null, "unsupportedEncoding", RestliRequestOptions.DEFAULT_OPTIONS, large, EXPECT_NO_COMPRESSION},
        {null, "", RestliRequestOptions.DEFAULT_OPTIONS, large, EXPECT_NO_COMPRESSION},
        {new CompressionConfig(0), "unsupportedEncoding", RestliRequestOptions.DEFAULT_OPTIONS, large, EXPECT_NO_COMPRESSION},
        {null, "", forceOnOption, large, EXPECT_NO_COMPRESSION}
    };
  }

  @Test(dataProvider = "requestData", retryAnalyzer = ThreeRetries.class)
  public void testUpdate(CompressionConfig requestCompressionConfig,
                         String supportedEncodings,
                         RestliRequestOptions restliRequestOptions,
                         int messageLength,
                         String testHelpHeader)
      throws RemoteInvocationException, CloneNotSupportedException, InterruptedException, ExecutionException,
             TimeoutException
  {
    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("R2 Netty Scheduler"));
    Map<String, CompressionConfig> requestCompressionConfigs = new HashMap<String, CompressionConfig>();
    if (requestCompressionConfig != null)
    {
      requestCompressionConfigs.put(SERVICE_NAME, requestCompressionConfig);
    }
    HttpClientFactory httpClientFactory = new HttpClientFactory.Builder()
        .setFilterChain(FilterChains.empty())
        .setNioEventLoopGroup(new NioEventLoopGroup())
        .setShutDownFactory(true)
        .setScheduleExecutorService(executor)
        .setShutdownScheduledExecutorService(true)
        .setCallbackExecutor(null)
        .setShutdownCallbackExecutor(false)
        .setJmxManager(AbstractJmxManager.NULL_JMX_MANAGER)
        .setRequestCompressionThresholdDefault(500)
        .setRequestCompressionConfigs(requestCompressionConfigs)
        .build();
    Map<String, String> properties = new HashMap<String, String>();

    properties.put(HttpClientFactory.HTTP_REQUEST_CONTENT_ENCODINGS, supportedEncodings);
    properties.put(HttpClientFactory.HTTP_SERVICE_NAME, SERVICE_NAME);
    TransportClientAdapter clientAdapter1 = new TransportClientAdapter(httpClientFactory.getClient(properties));
    RestClient client = new RestClient(clientAdapter1, FILTERS_URI_PREFIX);
    RootBuilderWrapper<Long, Greeting> builders = new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders(restliRequestOptions));

    // GET
    Request<Greeting> request = builders.get().id(1L).build();
    ResponseFuture<Greeting> future = client.sendRequest(request);
    Response<Greeting> greetingResponse = future.getResponse();
    String response1 = greetingResponse.getEntity().getMessage();
    Assert.assertNotNull(response1);

    // POST
    Greeting greeting = new Greeting(greetingResponse.getEntity().data().copy());
    char[] As = new char[messageLength];
    Arrays.fill(As, 'A');
    String message = new String(As);
    greeting.setMessage(message);

    Request<EmptyRecord> writeRequest = builders.update().id(1L).input(greeting).setHeader(TEST_HELP_HEADER, testHelpHeader).build();
    client.sendRequest(writeRequest).getResponse();

    // GET again, to verify that our POST worked.
    Request<Greeting> request2 = builders.get().id(1L).build();
    ResponseFuture<Greeting> future2 = client.sendRequest(request2);
    String response2 = future2.getResponse().getEntity().getMessage();

    Assert.assertEquals(response2, message);

    FutureCallback<None> callback1 = new FutureCallback<None>();
    client.shutdown(callback1);
    callback1.get(30, TimeUnit.SECONDS);

    FutureCallback<None> callback2 = new FutureCallback<None>();
    httpClientFactory.shutdown(callback2);
    callback2.get(30, TimeUnit.SECONDS);
  }
}
