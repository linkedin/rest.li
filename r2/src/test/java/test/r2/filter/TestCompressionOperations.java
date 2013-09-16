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

package test.r2.filter;

import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.filter.compression.ClientCompressionFilter;
import com.linkedin.r2.filter.compression.CompressionConstants;
import com.linkedin.r2.filter.compression.EncodingType;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.transport.http.common.HttpConstants;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import junit.framework.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test compression rules
 *
 * @author Karan Parikh
 */
public class TestCompressionOperations
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

    public HeaderCaptureFilter(boolean shouldBePresent)
    {
      _shouldBePresent = shouldBePresent;
    }

    @Override
    public void onRequest(RestRequest restRequest, RequestContext requestContext, Map<String, String> wireAttrs)
    {
      String acceptEncodingHeader = restRequest.getHeader(HttpConstants.ACCEPT_ENCODING);
      if (_shouldBePresent)
      {
        Assert.assertNotNull(acceptEncodingHeader);
      }
      else
      {
        Assert.assertNull(acceptEncodingHeader);
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

  @DataProvider(name = "data")
  public Object[][] provideData()
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

  @Test(dataProvider = "data")
  public void test(String compressionConfig, String[] operations, boolean headerShouldBePresent)
      throws URISyntaxException
  {
    RestRequest restRequest = new RestRequestBuilder(new URI(URI)).build();
    ClientCompressionFilter clientCompressionFilter = new ClientCompressionFilter(EncodingType.IDENTITY.getHttpName(),
                                                                                  ACCEPT_COMPRESSIONS,
                                                                                  Arrays.asList(compressionConfig.split(",")));

    for (String operation: operations)
    {
      RequestContext context = new RequestContext();
      context.putLocalAttr(CompressionConstants.OPERATION, operation);

      clientCompressionFilter.onRestRequest(restRequest,
                                            context,
                                            Collections.<String, String>emptyMap(),
                                            new HeaderCaptureFilter(headerShouldBePresent));
    }
  }
}
