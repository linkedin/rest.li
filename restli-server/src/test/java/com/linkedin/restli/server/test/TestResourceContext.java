/*
   Copyright (c) 2012 LinkedIn Corp.

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

package com.linkedin.restli.server.test;


import com.linkedin.data.ByteString;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.r2.message.RequestBuilder;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestMessageBuilder;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.TestConstants;
import com.linkedin.restli.internal.server.PathKeysImpl;
import com.linkedin.restli.internal.server.ResourceContextImpl;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.server.ResourceContext;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class TestResourceContext
{
  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "projectionMask")
  public Object[][] projectionMask()
  {
    return new Object[][]
      {
        { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "groups/?count=10&emailDomain=foo.com&fields=locale,state&q=emailDomain&start=0"},
        { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "groups/?count=10&emailDomain=foo.com&fields=locale,state&q=emailDomain&start=0"}
      };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "projectionMask")
  public void testResourceContextGetProjectionMask(ProtocolVersion version, String stringUri) throws Exception
  {
    URI uri = URI.create(stringUri);
    Map<String, String> headers = new HashMap<String, String>(1);
    headers.put(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, version.toString());

    ResourceContext context = new ResourceContextImpl(new PathKeysImpl(),
                                                      new MockRequest(uri, headers),
                                                      new RequestContext());
    MaskTree mask = context.getProjectionMask();
    Assert.assertEquals(mask.toString(), "{locale=1, state=1}");
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "queryParamsProjectionMaskWithSyntax")
  public Object[][] queryParamsProjectionMaskWithSyntax()
  {
    return new Object[][]
      {
        { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "groups/?count=10&emailDomain=foo.com&fields=locale,state,location:(longitude,latitude)&q=emailDomain&start=0"},
        { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "groups/?count=10&emailDomain=foo.com&fields=locale,state,location:(longitude,latitude)&q=emailDomain&start=0"}
      };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "queryParamsProjectionMaskWithSyntax")
  public void testResourceContextWithQueryParamsGetProjectionMaskWithMaskSyntax(ProtocolVersion version, String stringUri) throws Exception
  {
    URI uri = URI.create(stringUri);
    Map<String, String> headers = new HashMap<String, String>(1);
    headers.put(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, version.toString());

    ResourceContext context = new ResourceContextImpl(new PathKeysImpl(),
                                                      new MockRequest(uri, headers),
                                                      new RequestContext());
    MaskTree mask = context.getProjectionMask();
    Assert.assertEquals(mask.toString(), "{location={longitude=1, latitude=1}, locale=1, state=1}");
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "projectionMaskWithSyntax")
  public Object[][] projectionMaskWithSyntax()
  {
    return new Object[][]
      {
        { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "groups/?fields=a:($*),b:($*:(c)),d:($*:(e,f))"},
        { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "groups/?fields=a:($*),b:($*:(c)),d:($*:(e,f))"}
      };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "projectionMaskWithSyntax")
  public void testResourceContextGetProjectionMaskWithSyntax(ProtocolVersion version, String stringUri) throws Exception
  {
    URI uri = URI.create(stringUri);
    Map<String, String> headers = new HashMap<String, String>(1);
    headers.put(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, version.toString());

    ResourceContext context = new ResourceContextImpl(new PathKeysImpl(),
                                                      new MockRequest(uri, headers),
                                                      new RequestContext());
    MaskTree mask = context.getProjectionMask();
    Assert.assertEquals(mask.toString(), "{d={$*={f=1, e=1}}, b={$*={c=1}}, a={$*=1}}");
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "uriDecoding")
  public Object[][] uriDecoding()
  {
    return new Object[][]
      {
        { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "groups/?fields=foo,bar,baz&testParam.a%5B0%5D=b&testParam.a%5B1%5D=c&testParam.a%5B2%5D=d&q=test"},
        { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "groups/?fields=foo,bar,baz&testParam=(a:List(b,c,d))&q=test"}
      };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "uriDecoding")
  public void testResourceContextURIDecoding(ProtocolVersion version, String stringUri) throws Exception
  {
    URI uri = URI.create(stringUri);
    Map<String, String> headers = new HashMap<String, String>(1);
    headers.put(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, version.toString());

    ServerResourceContext context = new ResourceContextImpl(new PathKeysImpl(),
                                                            new MockRequest(uri, headers),
                                                            new RequestContext());

    MaskTree projectionMask = context.getProjectionMask();
    Assert.assertEquals(projectionMask.toString(), "{baz=1, foo=1, bar=1}");

    DataMap parameters = context.getParameters();
    DataMap expectedParameters = new DataMap();
    expectedParameters.put("fields", "foo,bar,baz");
    expectedParameters.put("q", "test");
    DataMap testParam = new DataMap();
    DataList aValue = new DataList();
    aValue.add("b");
    aValue.add("c");
    aValue.add("d");
    testParam.put("a", aValue);
    expectedParameters.put("testParam", testParam);
    Assert.assertEquals(parameters, expectedParameters);
  }

  public static class MockRequest implements RestRequest
  {
    private final URI _uri;
    private final Map<String, String> _headers;

    public MockRequest(URI uri)
    {
      _uri = uri;
      _headers = new HashMap<String, String>();
    }

    public MockRequest(URI uri, Map<String, String> headers)
    {
      _uri = uri;
      _headers = headers;
    }

    @Override
    public String getMethod()
    {
      return null;
    }

    @Override
    public RestRequestBuilder builder()
    {
      return null;
    }

    @Override
    public ByteString getEntity()
    {
      return null;
    }

    @Override
    public RestMessageBuilder<? extends RestMessageBuilder<?>> restBuilder()
    {
      return null;
    }

    @Override
    public Map<String, String> getHeaders()
    {
      return _headers;
    }

    @Override
    public List<String> getHeaderValues(String name)
    {
      return null;
    }

    @Override
    public String getHeader(String name)
    {
      return _headers.get(name);
    }

    @Override
    public RequestBuilder<? extends RequestBuilder<?>> requestBuilder()
    {
      return null;
    }

    @Override
    public URI getURI()
    {
      return _uri;
    }
  }

}
