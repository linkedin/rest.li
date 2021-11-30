/*
   Copyright (c) 2013 LinkedIn Corp.

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

package com.linkedin.r2.filter.compression;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.data.ByteString;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.filter.CompressionConfig;
import com.linkedin.r2.filter.CompressionOption;
import com.linkedin.r2.filter.compression.streaming.StreamEncodingType;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestMethod;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamRequestBuilder;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.entitystream.ByteStringWriter;
import com.linkedin.r2.message.stream.entitystream.EntityStream;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.message.stream.entitystream.FullEntityReader;
import com.linkedin.r2.message.stream.entitystream.Reader;
import com.linkedin.r2.transport.http.common.HttpConstants;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test compression rules
 *
 * @author Ang Xu
 */
public class TestClientStreamCompressionFilter
{

  private static final String ACCEPT_COMPRESSIONS = "gzip, deflate, bzip2, x-snappy-framed";
  private static final String URI = "http://test";

  /**
   * Inspects the header for the Accept-Encoding header
   *
   * @author Karan Parikh
   */
  class HeaderCaptureFilter implements NextFilter<StreamRequest, StreamResponse>
  {

    private boolean _shouldBePresent;
    private String _headerName;
    private int _entityLength = 0;
    private EntityStream _entityStream;
    private final Reader _entityReader;

    public HeaderCaptureFilter(String headerName, boolean shouldBePresent, Reader entityReader)
    {
      _shouldBePresent = shouldBePresent;
      _headerName = headerName;
      _entityReader = entityReader;

    }

    public HeaderCaptureFilter(String headerName, boolean shouldBePresent, int entityLength, Reader entityReader)
    {
      this(headerName, shouldBePresent, entityReader);
      _entityLength = entityLength;
    }

    @Override
    public void onRequest(StreamRequest streamRequest, RequestContext requestContext, Map<String, String> wireAttrs)
    {
      String header = streamRequest.getHeader(_headerName);
      if (_shouldBePresent)
      {
        Assert.assertNotNull(header);
      }
      else
      {
        Assert.assertNull(header);
      }
      if (_entityLength > 0)
      {
        _entityStream = streamRequest.getEntityStream();
        if (_entityReader != null)
        {
          _entityStream.setReader(_entityReader);
        }
      }
    }

    @Override
    public void onResponse(StreamResponse streamResponse, RequestContext requestContext, Map<String, String> wireAttrs)
    {

    }

    @Override
    public void onError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs)
    {

    }

    public EntityStream getEntityStream()
    {
      return _entityStream;
    }
  }

  @DataProvider(name = "operationsData")
  public Object[][] provideOperationsData()
  {
    return new Object[][] {
        {"*", new String[]{"foo", "bar", "foo:bar"}, true},

        {"foo", new String[]{"foo"}, true},
        {"foo", new String[]{"bar", "foo:bar"}, false},

        {"foo,bar,foobar", new String[]{"foo", "bar", "foobar"}, true},
        {"foo,bar,foobar", new String[]{"baz", "foo:bar", "bar:foo"}, false},

        {"foo:*", new String[]{"foo:foo", "foo:bar", "foo:baz"}, true},
        {"foo:*", new String[]{"foo", "bar"}, false},

        {"foo:*,bar:*", new String[]{"foo:foo", "foo:bar", "bar:bar", "bar:foo"}, true},
        {"foo:*,bar:*", new String[]{"baz", "baz:foo", "foo", "bar"}, false},

        {"foo:*,bar:*,baz,foo", new String[]{"foo:foo", "foo:bar", "bar:bar", "bar:foo", "baz", "foo"}, true},
        {"foo:*,bar:*,baz,foo", new String[]{"bar", "foobar", "foobarbaz", "baz:bar", "foobar:bazbar"}, false}
    };
  }

  @Test(dataProvider = "operationsData")
  public void testCompressionOperations(String compressionConfig, String[] operations, boolean headerShouldBePresent)
      throws URISyntaxException
  {
    StreamRequest streamRequest = new StreamRequestBuilder(new URI(URI)).build(EntityStreams.emptyStream());
    ClientStreamCompressionFilter clientCompressionFilter =
        new ClientStreamCompressionFilter(StreamEncodingType.IDENTITY.getHttpName(),
            new CompressionConfig(Integer.MAX_VALUE),
            ACCEPT_COMPRESSIONS,
            new CompressionConfig(Integer.MAX_VALUE),
            Arrays.asList(compressionConfig.split(",")),
            Executors.newCachedThreadPool() );

    for (String operation: operations)
    {
      RequestContext context = new RequestContext();
      context.putLocalAttr(R2Constants.OPERATION, operation);

      clientCompressionFilter.onStreamRequest(streamRequest, context, Collections.<String, String>emptyMap(),
          new HeaderCaptureFilter(HttpConstants.ACCEPT_ENCODING, headerShouldBePresent,null));
    }
  }

  @DataProvider(name = "requestData")
  private Object[][] provideRequestData()
  {
    CompressionConfig smallThresholdConfig = new CompressionConfig(1);
    CompressionConfig largeThresholdConfig = new CompressionConfig(10000);

    return new Object[][] {
        {new CompressionConfig(Integer.MAX_VALUE), CompressionOption.FORCE_OFF, false, ""},
        {new CompressionConfig(Integer.MAX_VALUE), CompressionOption.FORCE_ON, true, ""},
        {new CompressionConfig(Integer.MAX_VALUE), null, false, ""},
        {new CompressionConfig(0), CompressionOption.FORCE_OFF, false, ""},
        {new CompressionConfig(0), CompressionOption.FORCE_ON, true, ""},
        {new CompressionConfig(0), null, true, ""},
        {smallThresholdConfig, CompressionOption.FORCE_OFF, false, ""},
        {smallThresholdConfig, CompressionOption.FORCE_ON, true, ""},
        {smallThresholdConfig, null, true, ""},
        {largeThresholdConfig, CompressionOption.FORCE_OFF, false, ""},
        {largeThresholdConfig, CompressionOption.FORCE_ON, true, ""},
        {largeThresholdConfig, null, false, ""},
        // The same tests, but with null instead of an empty string
        {new CompressionConfig(Integer.MAX_VALUE), CompressionOption.FORCE_OFF, false, null},
        {new CompressionConfig(Integer.MAX_VALUE), CompressionOption.FORCE_ON, true, null},
        {new CompressionConfig(Integer.MAX_VALUE), null, false, null},
        {new CompressionConfig(0), CompressionOption.FORCE_OFF, false, null},
        {new CompressionConfig(0), CompressionOption.FORCE_ON, true, null},
        {new CompressionConfig(0), null, true, null},
        {smallThresholdConfig, CompressionOption.FORCE_OFF, false, null},
        {smallThresholdConfig, CompressionOption.FORCE_ON, true, null},
        {smallThresholdConfig, null, true, null},
        {largeThresholdConfig, CompressionOption.FORCE_OFF, false, null},
        {largeThresholdConfig, CompressionOption.FORCE_ON, true, null},
        {largeThresholdConfig, null, false, null}
    };
  }

  @Test(dataProvider = "requestData")
  public void testRequestCompressionRules(CompressionConfig requestCompressionConfig,
                                          CompressionOption requestCompressionOverride,
                                          boolean headerShouldBePresent,
                                          String operation)
      throws CompressionException, URISyntaxException, InterruptedException, ExecutionException, TimeoutException {
    Executor executor = Executors.newCachedThreadPool();
    ClientStreamCompressionFilter clientCompressionFilter = new ClientStreamCompressionFilter(
        StreamEncodingType.GZIP.getHttpName(),
        requestCompressionConfig,
        ACCEPT_COMPRESSIONS,
        new CompressionConfig(Integer.MAX_VALUE),
        Arrays.asList(ClientCompressionHelper.COMPRESS_ALL_RESPONSES_INDICATOR),
        executor);
    // The entity should be compressible for this test.
    int original = 100;
    byte[] entity = new byte[original];
    Arrays.fill(entity, (byte)'A');
    StreamRequest streamRequest =
        new StreamRequestBuilder(new URI(URI))
          .setMethod(RestMethod.POST)
          .build(EntityStreams.newEntityStream(new ByteStringWriter(ByteString.copy(entity))));

    int compressed = EncodingType.GZIP.getCompressor().deflate(new ByteArrayInputStream(entity)).length;
    RequestContext context = new RequestContext();
    if (operation != null)
    {
      context.putLocalAttr(R2Constants.OPERATION, operation);
    }
    context.putLocalAttr(R2Constants.REQUEST_COMPRESSION_OVERRIDE, requestCompressionOverride);
    int entityLength = headerShouldBePresent ? compressed : original;

    FutureCallback<ByteString> callback = new FutureCallback<>();
    FullEntityReader reader = new FullEntityReader(callback);

    HeaderCaptureFilter captureFilter =
        new HeaderCaptureFilter(HttpConstants.CONTENT_ENCODING, headerShouldBePresent, entityLength, reader);

    clientCompressionFilter.onStreamRequest(streamRequest, context, Collections.<String, String>emptyMap(),
        captureFilter);

    ByteString entityRead = callback.get(10, TimeUnit.SECONDS);
    Assert.assertEquals(entityRead.length(), entityLength);
  }

  @Test(dataProvider = "requestData")
  public void testAcceptEncodingHeader(CompressionConfig requestCompressionConfig,
                                       CompressionOption requestCompressionOverride,
                                       boolean headerShouldBePresent,
                                       String operation)
      throws CompressionException, URISyntaxException, InterruptedException, ExecutionException, TimeoutException {
    Executor executor = Executors.newCachedThreadPool();
    ClientStreamCompressionFilter clientCompressionFilter = new ClientStreamCompressionFilter(
        StreamEncodingType.GZIP.getHttpName(),
        requestCompressionConfig,
        ACCEPT_COMPRESSIONS,
        new CompressionConfig(Integer.MAX_VALUE),
        Arrays.asList(ClientCompressionHelper.COMPRESS_ALL_RESPONSES_INDICATOR),
        executor);
    // The entity should be compressible for this test.
    int original = 100;
    byte[] entity = new byte[original];
    Arrays.fill(entity, (byte)'A');
    StreamRequest streamRequest =
        new StreamRequestBuilder(new URI(URI))
            .setMethod(RestMethod.POST)
            .build(EntityStreams.newEntityStream(new ByteStringWriter(ByteString.copy(entity))));

    int compressed = EncodingType.GZIP.getCompressor().deflate(new ByteArrayInputStream(entity)).length;
    RequestContext context = new RequestContext();
    if (operation != null)
    {
      context.putLocalAttr(R2Constants.OPERATION, operation);
    }
    context.putLocalAttr(R2Constants.REQUEST_COMPRESSION_OVERRIDE, requestCompressionOverride);
    int entityLength = headerShouldBePresent ? compressed : original;

    clientCompressionFilter.onStreamRequest(streamRequest, context, Collections.<String, String>emptyMap(),
                                            new HeaderCaptureFilter(HttpConstants.ACCEPT_ENCODING, true, null));
  }
}
