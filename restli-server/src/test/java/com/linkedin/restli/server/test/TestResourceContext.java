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

/**
 * $Id: $
 */

package com.linkedin.restli.server.test;

import com.linkedin.data.ByteString;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.r2.message.RequestBuilder;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestMessageBuilder;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.restli.internal.server.PathKeysImpl;
import com.linkedin.restli.internal.server.ResourceContextImpl;
import com.linkedin.restli.server.PathKeys;
import com.linkedin.restli.server.ResourceContext;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class TestResourceContext
{

  @Test
  public void testResourceContextGetProjectionMask() throws Exception
  {
    URI uri = URI.create(
            "groups/?count=10&emailDomain=foo.com&fields=locale,state&q=emailDomain&start=0");

    ResourceContext context = new ResourceContextImpl(new PathKeysImpl(), new MockRequest(uri),
                                                      new RequestContext());
    MaskTree mask = context.getProjectionMask();
    Assert.assertEquals(mask.toString(), "{locale=1, state=1}");
  }

  @Test
  public void testResourceContextGetProjectionMaskWithMaskSyntax() throws Exception
  {
    URI uri = URI.create(
            "groups/?count=10&emailDomain=foo.com&fields=locale,state,location:(longitude,latitude)&q=emailDomain&start=0");

    ResourceContext context = new ResourceContextImpl(new PathKeysImpl(), new MockRequest(uri),
                                                      new RequestContext());
    MaskTree mask = context.getProjectionMask();
    Assert.assertEquals(mask.toString(), "{location={longitude=1, latitude=1}, locale=1, state=1}");

    uri = URI.create(
            "groups/?fields=a:($*),b:($*:(c)),d:($*:(e,f))");

    context = new ResourceContextImpl(new PathKeysImpl(), new MockRequest(uri),
                                      new RequestContext());
    mask = context.getProjectionMask();
    Assert.assertEquals(mask.toString(), "{d={$*={f=1, e=1}}, b={$*={c=1}}, a={$*=1}}");

  }



  public static class MockRequest implements RestRequest
  {
    private final URI _uri;

    public MockRequest(URI uri)
    {
      _uri = uri;
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
      return null;
    }

    @Override
    public List<String> getHeaderValues(String name)
    {
      return null;
    }

    @Override
    public String getHeader(String name)
    {
      return null;
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
