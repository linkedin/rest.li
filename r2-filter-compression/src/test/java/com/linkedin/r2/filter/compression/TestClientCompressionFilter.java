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

import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.filter.CompressionConfig;
import com.linkedin.r2.filter.CompressionOption;
import com.linkedin.r2.filter.compression.ClientCompressionFilter;
import com.linkedin.r2.filter.compression.CompressionException;
import com.linkedin.r2.filter.compression.EncodingType;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestMethod;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.transport.http.common.HttpConstants;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test compression rules
 *
 * @author Karan Parikh
 */
public class TestClientCompressionFilter
{

  private static final String ACCEPT_COMPRESSIONS = "gzip, deflate, bzip2, snappy";
  private static final String URI = "http://test";

  /**
   * Inspects the header for the Accept-Encoding header
   *
   * @author Karan Parikh
   */
  class HeaderCaptureFilter implements NextFilter<RestRequest, RestResponse>
  {

    private boolean _shouldBePresent;
    private String _headerName;
    private int _entityLength = 0;

    public HeaderCaptureFilter(String headerName, boolean shouldBePresent)
    {
      _shouldBePresent = shouldBePresent;
      _headerName = headerName;
    }

    public HeaderCaptureFilter(String headerName, boolean shouldBePresent, int entityLength)
    {
      this(headerName, shouldBePresent);
      _entityLength = entityLength;
    }

    @Override
    public void onRequest(RestRequest restRequest, RequestContext requestContext, Map<String, String> wireAttrs)
    {
      String header = restRequest.getHeader(_headerName);
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
        Assert.assertEquals(restRequest.getEntity().length(), _entityLength);
      }
    }

    @Override
    public void onResponse(RestResponse restResponse, RequestContext requestContext, Map<String, String> wireAttrs)
    {

    }

    @Override
    public void onError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs)
    {

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
    RestRequest restRequest = new RestRequestBuilder(new URI(URI)).build();
    ClientCompressionFilter clientCompressionFilter = new ClientCompressionFilter(EncodingType.IDENTITY.getHttpName(),
                                                                                  new CompressionConfig(Integer.MAX_VALUE),
                                                                                  ACCEPT_COMPRESSIONS,
                                                                                  Arrays.asList(compressionConfig.split(",")));

    for (String operation: operations)
    {
      RequestContext context = new RequestContext();
      context.putLocalAttr(R2Constants.OPERATION, operation);

      clientCompressionFilter.onRestRequest(restRequest,
                                            context,
                                            Collections.<String, String>emptyMap(),
                                            new HeaderCaptureFilter(HttpConstants.ACCEPT_ENCODING, headerShouldBePresent));
    }
  }

  @DataProvider(name = "requestData")
  private Object[][] provideRequestData()
  {
    CompressionConfig smallThresholdConfig = new CompressionConfig(1);
    CompressionConfig largeThresholdConfig = new CompressionConfig(10000);

    return new Object[][] {
        {new CompressionConfig(Integer.MAX_VALUE), CompressionOption.FORCE_OFF, false},
        {new CompressionConfig(Integer.MAX_VALUE), CompressionOption.FORCE_ON, true},
        {new CompressionConfig(Integer.MAX_VALUE), null, false},
        {new CompressionConfig(0), CompressionOption.FORCE_OFF, false},
        {new CompressionConfig(0), CompressionOption.FORCE_ON, true},
        {new CompressionConfig(0), null, true},
        {smallThresholdConfig, CompressionOption.FORCE_OFF, false},
        {smallThresholdConfig, CompressionOption.FORCE_ON, true},
        {smallThresholdConfig, null, true},
        {largeThresholdConfig, CompressionOption.FORCE_OFF, false},
        {largeThresholdConfig, CompressionOption.FORCE_ON, true},
        {largeThresholdConfig, null, false}
    };
  }

  @Test(dataProvider = "requestData")
  public void testRequestCompressionRules(CompressionConfig requestCompressionConfig,
                                          CompressionOption requestCompressionOverride, boolean headerShouldBePresent)
      throws CompressionException, URISyntaxException
  {
    ClientCompressionFilter clientCompressionFilter = new ClientCompressionFilter(EncodingType.SNAPPY.getHttpName(),
        requestCompressionConfig,
        ACCEPT_COMPRESSIONS,
        Collections.<String>emptyList());
    // The entity should be compressible for this test.
    int original = 100;
    byte[] entity = new byte[original];
    Arrays.fill(entity, (byte)'A');
    RestRequest restRequest = new RestRequestBuilder(new URI(URI)).setMethod(RestMethod.POST).setEntity(entity).build();
    int compressed = EncodingType.SNAPPY.getCompressor().deflate(new ByteArrayInputStream(entity)).length;
    RequestContext context = new RequestContext();
    context.putLocalAttr(R2Constants.OPERATION, "");
    context.putLocalAttr(R2Constants.REQUEST_COMPRESSION_OVERRIDE, requestCompressionOverride);
    int entityLength = headerShouldBePresent ? compressed : original;
    clientCompressionFilter.onRestRequest(restRequest, context, Collections.<String, String>emptyMap(),
        new HeaderCaptureFilter(HttpConstants.CONTENT_ENCODING, headerShouldBePresent, entityLength));
  }
}
