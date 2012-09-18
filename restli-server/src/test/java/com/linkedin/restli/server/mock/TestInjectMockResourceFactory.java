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

package com.linkedin.restli.server.mock;

import com.linkedin.data.ByteString;
import com.linkedin.common.callback.Callback;
import com.linkedin.r2.message.RequestBuilder;
import com.linkedin.r2.message.rest.RestMessageBuilder;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.restli.server.RestLiConfig;
import com.linkedin.restli.server.RestLiServer;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.RestLiActions;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class TestInjectMockResourceFactory
{
  public static class CounterBean
  {
    public int _counter = 0;
  }

  public static class SpecialBean
  {

  }

  public static class DerivedBean extends SpecialBean
  {

  }

  @RestLiActions(name="test")
  public static class Resource
  {
    @Inject @Named("counterBean")
    private CounterBean _counter;

    @Inject
    private SpecialBean _myOtherBean;

    @Action(name="validateBeans")
    public void validateBeans()
    {
      Assert.assertNotNull(_counter);
      Assert.assertNotNull(_myOtherBean);
      ++_counter._counter;
    }
  }

  @Test
  public void testMockInjection()
  {
    InjectMockResourceFactory factory =
            new InjectMockResourceFactory(new SimpleBeanProvider()
                                                  .add("counterBean", new CounterBean())
                                                  .add("mySpecialBean", new SpecialBean()),
                                          Resource.class);
    Resource instance = factory.create(Resource.class);
    instance.validateBeans();
  }

  @Test
  public void testMockInjectionSubClass()
  {
    InjectMockResourceFactory factory =
            new InjectMockResourceFactory(new SimpleBeanProvider()
                                                  .add("counterBean", new CounterBean())
                                                  .add("mySpecialBean", new DerivedBean()),
                                          Resource.class);

    Resource instance = factory.create(Resource.class);
    instance.validateBeans();
  }

  public static class MockRequest implements RestRequest
  {
    @Override
    public String getMethod()
    {
      return "POST";
    }

    @Override
    public RestRequestBuilder builder()
    {
      return null;
    }

    @Override
    public URI getURI()
    {
      return URI.create("/test?action=validateBeans");
    }

    @Override
    public RequestBuilder<? extends RequestBuilder<?>> requestBuilder()
    {
      return null;
    }

    @Override
    public String getHeader(String name)
    {
      return null;
    }

    @Override
    public List<String> getHeaderValues(String name)
    {
      return Collections.emptyList();
    }

    @Override
    public Map<String, String> getHeaders()
    {
      return Collections.emptyMap();
    }

    @Override
    public RestMessageBuilder<? extends RestMessageBuilder<?>> restBuilder()
    {
      return null;
    }

    @Override
    public ByteString getEntity()
    {
      return ByteString.copy(new byte[0]);
    }
  }

  @Test
  public void testMockInjectionViaServer()
  {
    final CounterBean counter = new CounterBean();

    InjectMockResourceFactory factory =
            new InjectMockResourceFactory(new SimpleBeanProvider()
                                                  .add("counterBean", counter)
                                                  .add("mySpecialBean", new SpecialBean()));
    RestLiConfig config = new RestLiConfig();
    config.addResourcePackageNames("com.linkedin.restli.server.mock");
    RestLiServer server = new RestLiServer(config, factory);
    server.handleRequest(new MockRequest(), new Callback<RestResponse>()
    {
      @Override
      public void onError(Throwable e)
      {
        Assert.fail("expected success response");
      }

      @Override
      public void onSuccess(RestResponse result)
      {
        Assert.assertEquals(counter._counter, 1);
      }
    });
  }
}
