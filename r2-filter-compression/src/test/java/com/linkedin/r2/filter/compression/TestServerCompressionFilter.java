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

import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.transport.http.common.HttpConstants;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

/**
 * Test compression rules
 *
 * @author Soojung Ha
 */
public class TestServerCompressionFilter
{
  private static final String ACCEPT_COMPRESSIONS = "gzip, deflate, bzip2, snappy";

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

    }

    @Override
    public void onResponse(RestResponse restResponse, RequestContext requestContext, Map<String, String> wireAttrs)
    {
      String header = restResponse.getHeader(_headerName);
      Assert.assertEquals(header, _expectedValue);
      if (_entityLength > 0)
      {
        Assert.assertEquals(restResponse.getEntity().length(), _entityLength);
      }
    }

    @Override
    public void onError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs)
    {

    }
  }

  @DataProvider(name = "headersData")
  private Object[][] provideHeadersData()
  {
    return new Object[][] {
        {"gzip;q=1.00,deflate;q=0.80,bzip2;q=0.60,snappy;q=0.40", 99, EncodingType.GZIP},
        {"snappy", 99, EncodingType.SNAPPY},
        {"unknown;q=1.00,bzip2;q=0.70", 99, EncodingType.BZIP2},
        {"gzip;q=1.00,deflate;q=0.80,bzip2;q=0.60,snappy;q=0.40", 100, null},
        {"snappy", 100, null},
        {"unknown;q=1.00,bzip2;q=0.70", 100, null},
        {"gzip;q=1.00,deflate;q=0.80,bzip2;q=0.60,snappy;q=0.40", Integer.MAX_VALUE, null},
        {"gzip;q=1.00,deflate;q=0.80,bzip2;q=0.60,snappy;q=0.40", 0, EncodingType.GZIP},
        {"snappy", Integer.MAX_VALUE, null},
        {"snappy", 0, EncodingType.SNAPPY},
        {"unknown;q=1.00,bzip2;q=0.70", 0, EncodingType.BZIP2},
        {"gzip;q=1.00,deflate;q=0.80,bzip2;q=0.60,snappy;q=0.40", 1000, null},
        {"snappy", 1000, null},
        {"unknown;q=1.00,bzip2;q=0.70", 1000, null}
    };
  }

  // Test response compression rules where the server has a default threshold of Integer.MAX_VALUE.
  @Test(dataProvider = "headersData")
  public void testResponseCompressionRules(String acceptEncoding, int compressionThreshold, EncodingType expectedContentEncoding)
      throws CompressionException, URISyntaxException
  {
    ServerCompressionFilter serverCompressionFilter = new ServerCompressionFilter(ACCEPT_COMPRESSIONS);
    RequestContext context = new RequestContext();
    context.putLocalAttr(HttpConstants.ACCEPT_ENCODING, acceptEncoding);
    context.putLocalAttr(HttpConstants.HEADER_RESPONSE_COMPRESSION_THRESHOLD, compressionThreshold);
    int originalLength = 100;
    byte[] entity = new byte[originalLength];
    Arrays.fill(entity, (byte) 'A');
    int compressedLength = (expectedContentEncoding == null) ? originalLength :
        expectedContentEncoding.getCompressor().deflate(new ByteArrayInputStream(entity)).length;
    String expectedContentEncodingName = (expectedContentEncoding == null) ? null : expectedContentEncoding.getHttpName();
    RestResponse restResponse = new RestResponseBuilder().setEntity(entity).build();
    serverCompressionFilter.onRestResponse(restResponse, context, Collections.<String, String>emptyMap(),
                                           new HeaderCaptureFilter(HttpConstants.CONTENT_ENCODING, expectedContentEncodingName, compressedLength));
  }
}
