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

package com.linkedin.r2.filter.compression;

import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.filter.CompressionConfig;
import com.linkedin.r2.filter.CompressionOption;
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
  private static final String ACCEPT_ENCODING_HEADER = "gzip;q=1.00,deflate;q=0.80,bzip2;q=0.60,snappy;q=0.40";
  private static final String URI = "http://test";

  /**
   * Inspects a header value and optionally checks the entity length.
   *
   * @author Karan Parikh
   */
  class HeaderCaptureFilter implements NextFilter<RestRequest, RestResponse>
  {
    private String _headerName;
    private String _expectedValue;
    private int _entityLength = 0;

    public HeaderCaptureFilter(String headerName, String expectedValue)
    {
      _headerName = headerName;
      _expectedValue = expectedValue;
    }

    public HeaderCaptureFilter(String headerName, String expectedValue, int entityLength)
    {
      this(headerName, expectedValue);
      _entityLength = entityLength;
    }

    @Override
    public void onRequest(RestRequest restRequest, RequestContext requestContext, Map<String, String> wireAttrs)
    {
      String header = restRequest.getHeader(_headerName);
      Assert.assertEquals(header, _expectedValue);
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

  @DataProvider(name = "requestCompressionData")
  private Object[][] provideRequestCompressionData()
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

  @Test(dataProvider = "requestCompressionData")
  public void testRequestCompressionRules(CompressionConfig requestCompressionConfig,
                                          CompressionOption requestCompressionOverride, boolean headerShouldBePresent)
      throws CompressionException, URISyntaxException
  {
    ClientCompressionFilter clientCompressionFilter = new ClientCompressionFilter(EncodingType.SNAPPY.getHttpName(),
                                                                                  requestCompressionConfig,
                                                                                  ACCEPT_COMPRESSIONS,
                                                                                  new CompressionConfig(Integer.MAX_VALUE),
                                                                                  Collections.<String>emptyList());
    // The entity should be compressible for this test.
    int original = 100;
    byte[] entity = new byte[original];
    Arrays.fill(entity, (byte)'A');
    RestRequest restRequest = new RestRequestBuilder(new URI(URI)).setMethod(RestMethod.POST).setEntity(entity).build();
    int compressed = EncodingType.SNAPPY.getCompressor().deflate(new ByteArrayInputStream(entity)).length;
    RequestContext context = new RequestContext();
    context.putLocalAttr(R2Constants.REQUEST_COMPRESSION_OVERRIDE, requestCompressionOverride);
    int entityLength = headerShouldBePresent ? compressed : original;
    String expectedContentEncoding = headerShouldBePresent ? EncodingType.SNAPPY.getHttpName() : null;
    clientCompressionFilter.onRestRequest(restRequest, context, Collections.<String, String>emptyMap(),
                                          new HeaderCaptureFilter(HttpConstants.CONTENT_ENCODING, expectedContentEncoding, entityLength));
  }

  @DataProvider(name = "responseCompressionData")
  private Object[][] provideResponseCompressionData()
  {
    CompressionConfig smallThresholdConfig = new CompressionConfig(1);
    CompressionConfig largeThresholdConfig = new CompressionConfig(10000);

    return new Object[][] {
        {new CompressionConfig(Integer.MAX_VALUE), CompressionOption.FORCE_OFF, null, null},
        {new CompressionConfig(Integer.MAX_VALUE), CompressionOption.FORCE_ON, ACCEPT_ENCODING_HEADER, Integer.toString(0)},
        {new CompressionConfig(Integer.MAX_VALUE), null, ACCEPT_ENCODING_HEADER, Integer.toString(Integer.MAX_VALUE)},
        {new CompressionConfig(0), CompressionOption.FORCE_OFF, null, null},
        {new CompressionConfig(0), CompressionOption.FORCE_ON, ACCEPT_ENCODING_HEADER, Integer.toString(0)},
        {new CompressionConfig(0), null, ACCEPT_ENCODING_HEADER, Integer.toString(0)},
        {smallThresholdConfig, CompressionOption.FORCE_OFF, null, null},
        {smallThresholdConfig, CompressionOption.FORCE_ON, ACCEPT_ENCODING_HEADER, Integer.toString(0)},
        {smallThresholdConfig, null, ACCEPT_ENCODING_HEADER, Integer.toString(1)},
        {largeThresholdConfig, CompressionOption.FORCE_OFF, null, null},
        {largeThresholdConfig, CompressionOption.FORCE_ON, ACCEPT_ENCODING_HEADER, Integer.toString(0)},
        {largeThresholdConfig, null, ACCEPT_ENCODING_HEADER, Integer.toString(10000)}
    };
  }

  @Test(dataProvider = "responseCompressionData")
  public void testResponseCompressionRules(CompressionConfig responseCompressionConfig,
                                           CompressionOption responseCompressionOverride,
                                           String expectedAcceptEncoding,
                                           String expectedCompressionThreshold)
      throws CompressionException, URISyntaxException
  {
    ClientCompressionFilter clientCompressionFilter = new ClientCompressionFilter(EncodingType.SNAPPY.getHttpName(),
                                                                                  new CompressionConfig(Integer.MAX_VALUE),
                                                                                  ACCEPT_COMPRESSIONS,
                                                                                  responseCompressionConfig,
                                                                                  Arrays.asList(ClientCompressionFilter.COMPRESS_ALL_RESPONSES_INDICATOR));
    RestRequest restRequest = new RestRequestBuilder(new URI(URI)).build();
    RequestContext context = new RequestContext();
    context.putLocalAttr(R2Constants.OPERATION, "get");
    context.putLocalAttr(R2Constants.RESPONSE_COMPRESSION_OVERRIDE, responseCompressionOverride);
    clientCompressionFilter.onRestRequest(restRequest, context, Collections.<String, String>emptyMap(),
                                          new HeaderCaptureFilter(HttpConstants.ACCEPT_ENCODING, expectedAcceptEncoding));
    clientCompressionFilter.onRestRequest(restRequest, context, Collections.<String, String>emptyMap(),
                                          new HeaderCaptureFilter(HttpConstants.HEADER_RESPONSE_COMPRESSION_THRESHOLD, expectedCompressionThreshold));

  }

  //Provides tests client generating requests.
  //Q values are generated by the formula 1-n/(k+1), where
  //n is the number of places behind the first entry and
  //k is the number of total entries
  @DataProvider
  public Object[][] contentEncodingGeneratorDataProvider()
  {
    //Length computation:
    //1 for 0 length
    //n for 1 length
    //n^2-n for 2 length since it doesn't make sense for the same scheme to be requested twice
    //1 for 3 corner case.
    int current = 0;
    int length = 1 + EncodingType.values().length*EncodingType.values().length + 1;
    Object[][] encoding = new Object[length][];
    encoding[current++] = new Object[]{new EncodingType[]{}, ""};

    //1's
    for(EncodingType type : EncodingType.values())
    {
      encoding[current++] = new Object[]{
          new EncodingType[]{type},
          type.getHttpName() + ";q=" + "1.00"};
    }

    //2's
    for(EncodingType prev : EncodingType.values())
    {
      for(EncodingType next : EncodingType.values())
      {
        if (prev != next)
        {
          encoding[current++] = new Object[]{new EncodingType[]{prev, next},
              prev.getHttpName() + ";q=" + "1.00" + ","
                  + next.getHttpName() + ";q=" + "0.67"
          };
        }
      }
    }

    //One random 3's case
    encoding[current++] = new Object[]{new EncodingType[]{
        EncodingType.DEFLATE, EncodingType.IDENTITY, EncodingType.GZIP},
        "deflate;q=1.00,identity;q=0.75,gzip;q=0.50"};

    return encoding;
  }

  @Test(dataProvider = "contentEncodingGeneratorDataProvider")
  public void testEncodingGeneration(EncodingType[] encoding, String acceptEncoding)
  {
    Assert.assertEquals(ClientCompressionFilter.buildAcceptEncodingHeader(encoding), acceptEncoding);
  }
}
