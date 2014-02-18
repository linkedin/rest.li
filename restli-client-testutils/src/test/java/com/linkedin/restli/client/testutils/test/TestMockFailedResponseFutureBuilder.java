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
import com.linkedin.restli.client.ErrorHandlingBehavior;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.ResponseFuture;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.client.testutils.MockFailedResponseFutureBuilder;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.examples.greetings.api.Greeting;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author kparikh
 */
public class TestMockFailedResponseFutureBuilder
{
  @Test
  public void testBuildIllegalStatus()
  {
    MockFailedResponseFutureBuilder builder = new MockFailedResponseFutureBuilder();
    try
    {
      builder.setStatus(200);
      Assert.fail("Setting a 2xx status should have failed!");
    }
    catch (IllegalArgumentException e)
    {
      // expected
    }
  }

  @Test
  public void testOnlyOneOfErrorResponseOrEntityIsSet()
  {
    MockFailedResponseFutureBuilder<Greeting> builder = new MockFailedResponseFutureBuilder<Greeting>();
    builder.setEntity(new Greeting());
    try
    {
      builder.setErrorResponse(new ErrorResponse());
      Assert.fail();
    }
    catch (IllegalStateException e)
    {
      // expected
    }

    builder = new MockFailedResponseFutureBuilder<Greeting>();
    builder.setErrorResponse(new ErrorResponse());
    try
    {
      builder.setEntity(new Greeting());
      Assert.fail();
    }
    catch (IllegalStateException e)
    {
      // expected
    }
  }

  private ResponseFuture<Greeting> buildWithErrorResponse(ErrorHandlingBehavior errorHandlingBehavior)
  {
    MockFailedResponseFutureBuilder<Greeting> builder = new MockFailedResponseFutureBuilder<Greeting>();
    ErrorResponse errorResponse = new ErrorResponse().setStatus(404).setMessage("foo");

    builder.setErrorResponse(errorResponse).setErrorHandlingBehavior(errorHandlingBehavior);
    return builder.build();
  }

  @Test
  public void testBuildWithErrorResponseFailOnError()
  {
    ResponseFuture<Greeting> future = buildWithErrorResponse(ErrorHandlingBehavior.FAIL_ON_ERROR);

    try
    {
      future.getResponse();
      Assert.fail("GetResponse should have thrown an exception!");
    }
    catch (Exception e)
    {
      Assert.assertTrue(e instanceof RestLiResponseException);
      Assert.assertEquals(((RestLiResponseException) e).getStatus(), 404);
      Assert.assertEquals(((RestLiResponseException) e).getServiceErrorMessage(), "foo");
    }
  }

  @Test
  public void testBuildWithErrorResponseTreatServerErrorAsSuccess()
  {
    ResponseFuture<Greeting> future = buildWithErrorResponse(ErrorHandlingBehavior.TREAT_SERVER_ERROR_AS_SUCCESS);

    try
    {
      Response<Greeting> response = future.getResponse();

      Assert.assertNull(response.getEntity());
      Assert.assertEquals(response.getStatus(), 404);

      RestLiResponseException restLiResponseException = response.getError();
      Assert.assertEquals(restLiResponseException.getStatus(), 404);
      Assert.assertEquals(restLiResponseException.getServiceErrorMessage(), "foo");
    }
    catch (Exception e)
    {
      Assert.fail("No exception should have been thrown!");
    }
  }

  private ResponseFuture<Greeting> buildWithEntity(ErrorHandlingBehavior errorHandlingBehavior)
  {
    MockFailedResponseFutureBuilder<Greeting> builder = new MockFailedResponseFutureBuilder<Greeting>();
    Greeting greeting = new Greeting().setId(1L).setMessage("foo");

    builder.setEntity(greeting).setErrorHandlingBehavior(errorHandlingBehavior).setStatus(500);
    return builder.build();
  }

  @Test
  public void testBuildWithEntityFailOnError()
  {
    ResponseFuture<Greeting> future = buildWithEntity(ErrorHandlingBehavior.FAIL_ON_ERROR);

    try
    {
      future.getResponse();
    }
    catch (RemoteInvocationException e)
    {
      Assert.assertTrue(e instanceof RestLiResponseException);
      Assert.assertEquals(((RestLiResponseException) e).getDecodedResponse().getEntity(),
                          new Greeting().setId(1L).setMessage("foo"));
      Assert.assertEquals(((RestLiResponseException) e).getDecodedResponse().getStatus(), 500);
    }
  }

  @Test
  public void testBuildWithEntityTreatServerErrorAsSuccess()
  {
    ResponseFuture<Greeting> future = buildWithEntity(ErrorHandlingBehavior.TREAT_SERVER_ERROR_AS_SUCCESS);

    try
    {
      Response<Greeting> response = future.getResponse();
      Assert.assertEquals(response.getEntity(), new Greeting().setId(1L).setMessage("foo"));
      Assert.assertEquals(response.getStatus(), 500);
      Assert.assertEquals(response.getError().getStatus(), 500);
    }
    catch (RemoteInvocationException e)
    {
      Assert.fail("No exception should have been thrown!");
    }
  }

  private ResponseFuture<Greeting> buildWithNoEntityOrErrorResponse(ErrorHandlingBehavior errorHandlingBehavior)
  {
    return new MockFailedResponseFutureBuilder<Greeting>()
        .setErrorHandlingBehavior(errorHandlingBehavior)
        .setStatus(409)
        .setId("1")
        .build();
  }

  @Test
  public void testBuildWithNoEntityOrErrorResponseFailOnError()
  {
    ResponseFuture<Greeting> future = buildWithNoEntityOrErrorResponse(ErrorHandlingBehavior.FAIL_ON_ERROR);

    try
    {
      future.getResponse();
      Assert.fail("An exception should have been thrown!");
    }
    catch (Exception e)
    {
      Assert.assertTrue(e instanceof RestLiResponseException);
      Assert.assertEquals(((RestLiResponseException) e).getStatus(), 409);
    }
  }

  @Test
  public void testBuildWithNoEntityOrErrorResponseTreatServerErrorAsSuccess()
  {
    ResponseFuture<Greeting> future = buildWithNoEntityOrErrorResponse(ErrorHandlingBehavior.TREAT_SERVER_ERROR_AS_SUCCESS);

    try
    {
      Response<Greeting> response = future.getResponse();
      Assert.assertNull(response.getEntity());
      Assert.assertEquals(response.getId(), "1");
      Assert.assertEquals(response.getStatus(), 409);
      Assert.assertEquals(response.getError().getStatus(), 409);
    }
    catch (Exception e)
    {
      Assert.fail("No exception should have been thrown!");
    }
  }
}
