/*
   Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.restli.client.testutils.test;


import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.restli.client.ResponseFuture;
import com.linkedin.restli.client.testutils.MockSuccessfulResponseFutureBuilder;
import com.linkedin.restli.examples.greetings.api.Greeting;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author kparikh
 */
public class TestMockSuccessfulResponseFutureBuilder
{
  @Test
  public void testBuild()
      throws RemoteInvocationException
  {
    MockSuccessfulResponseFutureBuilder<Greeting> builder = new MockSuccessfulResponseFutureBuilder<Greeting>();
    Greeting greeting = new Greeting().setId(1L).setMessage("foo");
    ResponseFuture<Greeting> future = builder.setEntity(greeting).setId("1").setStatus(200).build();

    Assert.assertEquals(future.getResponseEntity(), greeting);
    Assert.assertEquals(future.getResponse().getId(), "1");
    Assert.assertEquals(future.getResponse().getStatus(), 200);
  }

  @Test
  public void testBuildIllegalStatus()
  {
    MockSuccessfulResponseFutureBuilder builder = new MockSuccessfulResponseFutureBuilder();
    try
    {
      builder.setStatus(404);
      Assert.fail("Settting a non 2xx status should have failed!");
    }
    catch (IllegalArgumentException e)
    {
      // expected
    }
  }
}
