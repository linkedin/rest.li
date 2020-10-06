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

package com.linkedin.restli.examples;


import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.filter.CompressionConfig;
import com.linkedin.r2.filter.CompressionOption;
import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.filter.compression.EncodingType;
import com.linkedin.r2.filter.compression.ServerCompressionFilter;
import com.linkedin.r2.filter.logging.SimpleLoggingFilter;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.AbstractJmxManager;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.r2.transport.http.common.HttpConstants;
import com.linkedin.r2.util.NamedThreadFactory;
import com.linkedin.restli.client.BatchGetRequestBuilder;
import com.linkedin.restli.client.ProtocolVersionOption;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.client.RestliRequestOptions;
import com.linkedin.restli.client.RestliRequestOptionsBuilder;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.client.GreetingsBuilders;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.filter.Filter;
import com.linkedin.restli.server.filter.FilterRequestContext;
import com.linkedin.test.util.retry.SingleRetry;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import io.netty.channel.nio.NioEventLoopGroup;

/**
 * Integration tests for response compression.
 *
 * Tests that the client sends the correct Accept-Encoding and Response-Compression-Threshold headers,
 * and the server sends back a compressed response when appropriate.
 *
 * @author Soojung Ha
 */
public class TestResponseCompression extends RestLiIntegrationTest
{
  // Headers for sending test information to the server.
  private static final String EXPECTED_ACCEPT_ENCODING = "Expected-Accept-Encoding";
  private static final String DEFAULT_ACCEPT_ENCODING = "gzip;q=1.00,snappy;q=0.83,x-snappy-framed;q=0.67,deflate;q=0.50,bzip2;q=0.33";
  private static final String NONE = "None";
  private static final String EXPECTED_COMPRESSION_THRESHOLD = "Expected-Response-Compression-Threshold";
  private static final String SERVICE_NAME = "service1";

  @BeforeClass
  public void initClass() throws Exception
  {
    class TestHelperFilter implements Filter
    {
      @Override
      public CompletableFuture<Void> onRequest(FilterRequestContext requestContext)
      {
        Map<String, String> requestHeaders = requestContext.getRequestHeaders();
        if (requestHeaders.containsKey(EXPECTED_ACCEPT_ENCODING))
        {
          String expected = requestHeaders.get(EXPECTED_ACCEPT_ENCODING);
          if (expected.equals(NONE))
          {
            if (requestHeaders.containsKey(HttpConstants.ACCEPT_ENCODING))
            {
              throw new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST, "Accept-Encoding header should not be present.");
            }
          }
          else
          {
            if (!expected.equals(requestHeaders.get(HttpConstants.ACCEPT_ENCODING)))
            {
              throw new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST, "Accept-Encoding header should be " + expected
                  + ", but received " + requestHeaders.get(HttpConstants.ACCEPT_ENCODING));
            }
          }
        }
        if (requestHeaders.containsKey(EXPECTED_COMPRESSION_THRESHOLD))
        {
          if (!requestHeaders.get(EXPECTED_COMPRESSION_THRESHOLD).equals(requestHeaders.get(HttpConstants.HEADER_RESPONSE_COMPRESSION_THRESHOLD)))
          {
            throw new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST, "Expected " + HttpConstants.HEADER_RESPONSE_COMPRESSION_THRESHOLD
                + " " + requestHeaders.get(EXPECTED_COMPRESSION_THRESHOLD)
                + ", but received " + requestHeaders.get(HttpConstants.HEADER_RESPONSE_COMPRESSION_THRESHOLD));
          }
        }
        return CompletableFuture.completedFuture(null);
      }
    }
    // The default compression threshold is between tiny and huge threshold.
    final FilterChain fc = FilterChains.empty().addLastRest(new TestCompressionServer.SaveContentEncodingHeaderFilter())
        .addLastRest(new ServerCompressionFilter("x-snappy-framed,snappy,gzip,deflate", new CompressionConfig(10000)))
        .addLastRest(new SimpleLoggingFilter());
    super.init(Arrays.asList(new TestHelperFilter()), fc, false);
  }

  @AfterClass
  public void shutDown() throws Exception
  {
    super.shutdown();
  }

  @DataProvider(name = "requestData")
  private Object[][] requestData()
  {
    Integer zero = 0;
    Integer tiny = 100;
    Integer huge = 1000000;
    Integer max = Integer.MAX_VALUE;

    int largeIdCount = 100;
    int smallIdCount = 1;

    RestliRequestOptions forceOnOption = new RestliRequestOptionsBuilder().setProtocolVersionOption(ProtocolVersionOption.USE_LATEST_IF_AVAILABLE)
        .setResponseCompressionOverride(CompressionOption.FORCE_ON).build();
    RestliRequestOptions forceOffOption = new RestliRequestOptionsBuilder().setProtocolVersionOption(ProtocolVersionOption.USE_LATEST_IF_AVAILABLE)
        .setResponseCompressionOverride(CompressionOption.FORCE_OFF).build();

    return new Object[][] {
        // Large responses are compressed
        {true, null, RestliRequestOptions.DEFAULT_OPTIONS, largeIdCount, DEFAULT_ACCEPT_ENCODING, null, true},
        {true, null, RestliRequestOptions.DEFAULT_OPTIONS, smallIdCount, DEFAULT_ACCEPT_ENCODING, null, false},

        // Override the default threshold and cause small responses to be compressed
        {true, new CompressionConfig(tiny), RestliRequestOptions.DEFAULT_OPTIONS, largeIdCount, DEFAULT_ACCEPT_ENCODING, tiny.toString(), true},
        {true, new CompressionConfig(tiny), RestliRequestOptions.DEFAULT_OPTIONS, smallIdCount, DEFAULT_ACCEPT_ENCODING, tiny.toString(), true},

        // Override the default threshold and cause large responses to be NOT compressed
        {true, new CompressionConfig(huge), RestliRequestOptions.DEFAULT_OPTIONS, largeIdCount, DEFAULT_ACCEPT_ENCODING, huge.toString(), false},
        {true, new CompressionConfig(huge), RestliRequestOptions.DEFAULT_OPTIONS, smallIdCount, DEFAULT_ACCEPT_ENCODING, huge.toString(), false},

        // Force on/off using RestliRequestOptions
        {true, null, forceOnOption, largeIdCount, DEFAULT_ACCEPT_ENCODING, zero.toString(), true},
        {true, null, forceOnOption, smallIdCount, DEFAULT_ACCEPT_ENCODING, zero.toString(), true},
        {true, new CompressionConfig(huge), forceOnOption, smallIdCount, DEFAULT_ACCEPT_ENCODING, zero.toString(), true},
        {true, null, forceOffOption, largeIdCount, NONE, null, false},
        {true, null, forceOffOption, smallIdCount, NONE, null, false},
        {true, new CompressionConfig(huge), forceOffOption, largeIdCount, NONE, null, false},

        // Force on/off using ResponseCompressionConfig
        {true, new CompressionConfig(0), RestliRequestOptions.DEFAULT_OPTIONS, largeIdCount, DEFAULT_ACCEPT_ENCODING, zero.toString(), true},
        {true, new CompressionConfig(0), RestliRequestOptions.DEFAULT_OPTIONS, smallIdCount, DEFAULT_ACCEPT_ENCODING, zero.toString(), true},
        {true, new CompressionConfig(Integer.MAX_VALUE), RestliRequestOptions.DEFAULT_OPTIONS, largeIdCount, DEFAULT_ACCEPT_ENCODING, max.toString(), false},
        {true, new CompressionConfig(Integer.MAX_VALUE), RestliRequestOptions.DEFAULT_OPTIONS, smallIdCount, DEFAULT_ACCEPT_ENCODING, max.toString(), false},

        // RestliRequestOptions takes precedence over ResponseCompressionConfig
        {true, new CompressionConfig(0), forceOffOption, largeIdCount, NONE, null, false},
        {true, new CompressionConfig(Integer.MAX_VALUE), forceOnOption, smallIdCount, DEFAULT_ACCEPT_ENCODING, zero.toString(), true},

        // If http.useResponseCompression is false or null, Accept-Encoding header is not sent and response is not compressed
        {false, null, RestliRequestOptions.DEFAULT_OPTIONS, largeIdCount, NONE, null, false},
        {false, new CompressionConfig(tiny), RestliRequestOptions.DEFAULT_OPTIONS, largeIdCount, NONE, null, false},
        {false, null, forceOnOption, largeIdCount, NONE, null, false},
        {null, new CompressionConfig(0), RestliRequestOptions.DEFAULT_OPTIONS, largeIdCount, NONE, null, false},
        {null, new CompressionConfig(Integer.MAX_VALUE), forceOnOption, smallIdCount, NONE, null, false}
    };
  }

  @Test(dataProvider = "requestData", retryAnalyzer = SingleRetry.class) // Often fails in CI without a retry
  public void testResponseCompression(Boolean useResponseCompression, CompressionConfig responseCompressionConfig,
                                      RestliRequestOptions restliRequestOptions, int idCount, String expectedAcceptEncoding,
                                      String expectedCompressionThreshold, boolean responseShouldBeCompressed)
      throws RemoteInvocationException, CloneNotSupportedException
  {
    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("R2 Netty Scheduler"));
    Map<String, CompressionConfig> responseCompressionConfigs = new HashMap<String, CompressionConfig>();
    if (responseCompressionConfig != null)
    {
      responseCompressionConfigs.put(SERVICE_NAME, responseCompressionConfig);
    }
    HttpClientFactory httpClientFactory = new HttpClientFactory.Builder()
        .setEventLoopGroup(new NioEventLoopGroup(0 /* use default settings */, new NamedThreadFactory("R2 Nio Event Loop")))
        .setShutDownFactory(true)
        .setScheduleExecutorService(executor)
        .setShutdownScheduledExecutorService(true)
        .setCallbackExecutor(executor)
        .setShutdownCallbackExecutor(false)
        .setJmxManager(AbstractJmxManager.NULL_JMX_MANAGER)
        .setRequestCompressionThresholdDefault(Integer.MAX_VALUE)
        .setRequestCompressionConfigs(Collections.<String, CompressionConfig>emptyMap())
        .setResponseCompressionConfigs(responseCompressionConfigs)
        .setUseClientCompression(true)
        .build();
    Map<String, Object> properties = new HashMap<String, Object>();
    properties.put(HttpClientFactory.HTTP_SERVICE_NAME, SERVICE_NAME);
    if (useResponseCompression != null)
    {
      properties.put(HttpClientFactory.HTTP_USE_RESPONSE_COMPRESSION, String.valueOf(useResponseCompression));
    }
    TransportClientAdapter clientAdapter1 = new TransportClientAdapter(httpClientFactory.getClient(properties));
    RestClient client = new RestClient(clientAdapter1, FILTERS_URI_PREFIX);
    Long[] ids = new Long[idCount];
    for (int i = 0; i < ids.length; i++)
    {
      ids[i] = (long) i;
    }
    BatchGetRequestBuilder<Long, Greeting> builder = new GreetingsBuilders(restliRequestOptions).batchGet().ids(Arrays.asList(ids))
        .setHeader(EXPECTED_ACCEPT_ENCODING, expectedAcceptEncoding);
    if (expectedCompressionThreshold != null)
    {
      builder.setHeader(EXPECTED_COMPRESSION_THRESHOLD, expectedCompressionThreshold);
    }
    Request<BatchResponse<Greeting>> request = builder.build();
    Response<BatchResponse<Greeting>> response = client.sendRequest(request).getResponse();

    if (responseShouldBeCompressed)
    {
      Assert.assertEquals(response.getHeader(TestCompressionServer.CONTENT_ENCODING_SAVED), EncodingType.GZIP.getHttpName());
    }
    else
    {
      Assert.assertNull(response.getHeader(TestCompressionServer.CONTENT_ENCODING_SAVED));
    }
  }

  @DataProvider(name = "encodingsData")
  private Object[][] encodingsData()
  {
    return new Object[][]
        {
            {"snappy,gzip", "snappy;q=1.00,gzip;q=0.67", "snappy"},
            {"gzip,snappy", "gzip;q=1.00,snappy;q=0.67", "gzip"},
            {"deflate,gzip,snappy", "deflate;q=1.00,gzip;q=0.75,snappy;q=0.50", "deflate"},
            {"sdch,gzip,snappy", "gzip;q=1.00,snappy;q=0.67", "gzip"}, // client doesn't support sdch
            {"bzip2,snappy", "bzip2;q=1.00,snappy;q=0.67", "snappy"}, // server doesn't support bzip2
            {"bzip2,x-snappy-framed", "bzip2;q=1.00,x-snappy-framed;q=0.67", "x-snappy-framed"} // server doesn't support bzip2
        };
  }

  @Test(dataProvider = "encodingsData", retryAnalyzer = SingleRetry.class) // Often fails in CI without a retry
  public void testAcceptEncodingConfiguration(String responseContentEncodings, String expectedAcceptEncoding, String expectedContentEncoding) throws RemoteInvocationException
  {
    Map<String, Object> properties = new HashMap<String, Object>();
    properties.put(HttpClientFactory.HTTP_RESPONSE_CONTENT_ENCODINGS, responseContentEncodings);
    properties.put(HttpClientFactory.HTTP_USE_RESPONSE_COMPRESSION, "true");
    Client client = newTransportClient(properties);
    Long[] ids = new Long[100];
    for (int i = 0; i < ids.length; i++)
    {
      ids[i] = (long) i;
    }
    Request<BatchResponse<Greeting>> request = new GreetingsBuilders().batchGet().ids(Arrays.asList(ids))
        .setHeader(EXPECTED_ACCEPT_ENCODING, expectedAcceptEncoding).build();
    RestClient restClient = new RestClient(client, FILTERS_URI_PREFIX);
    Response<BatchResponse<Greeting>> response = restClient.sendRequest(request).getResponse();
    Assert.assertEquals(response.getHeader(TestCompressionServer.CONTENT_ENCODING_SAVED), expectedContentEncoding);
  }
}
