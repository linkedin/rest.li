/*
   Copyright (c) 2017 LinkedIn Corp.

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
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.examples.greetings.client.GreetingsRequestBuilders;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.filter.Filter;
import com.linkedin.restli.server.filter.FilterRequestContext;
import com.linkedin.restli.server.filter.FilterResponseContext;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


public class TestCustomContextData extends RestLiIntegrationTest
{
  @BeforeClass
  public void initClass()
    throws Exception
  {
    super.init();
  }

  @AfterClass
  public void shutDown()
    throws Exception
  {
    super.shutdown();
  }

  @Test
  public void testUpdateCustomData()
    throws RemoteInvocationException, IOException
  {
    List<Filter> filters = Arrays.asList(new TestFilter());
    init(filters);

    final Request<Void> req = new GreetingsRequestBuilders()
        .actionModifyCustomContext()
        .build();
    Response<Void> response = getClient().sendRequest(req).getResponse();
    Assert.assertEquals(response.getStatus(), HttpStatus.S_200_OK.getCode());
  }

  private class TestFilter implements Filter
  {
    @Override
    public CompletableFuture<Void> onRequest(FilterRequestContext requestContext)
    {
      requestContext.putCustomContextData("foo", "bar");
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> onResponse(FilterRequestContext requestContext,
                                              FilterResponseContext responseContext)
    {
      Optional<Object> customData = requestContext.getCustomContextData("foo");
      if (customData.isPresent() && customData.get().equals("newbar")) {
        return CompletableFuture.completedFuture(null);
      }
      CompletableFuture<Void> future = new CompletableFuture<Void>();
      future.completeExceptionally(new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR));
      return future;
    }
  }
}
