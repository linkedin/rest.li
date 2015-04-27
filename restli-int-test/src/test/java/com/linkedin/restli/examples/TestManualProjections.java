package com.linkedin.restli.examples;


import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.client.ManualProjectionsBuilders;
import com.linkedin.restli.examples.greetings.client.ManualProjectionsRequestBuilders;
import com.linkedin.restli.test.util.RootBuilderWrapper;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * Test a rest.li server that examines projection requests and manually applies the projection when building
 * the response.
 */
public class TestManualProjections extends RestLiIntegrationTest
{
  @BeforeClass
  public void initClass() throws Exception
  {
    super.init();
  }

  @AfterClass
  public void shutDown() throws Exception
  {
    super.shutdown();
  }

  /**
   * Test that a simple projection is applied correctly.
   * @throws RemoteInvocationException
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testGetWithProjection(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Request<Greeting> request = builders.get().id(1L)
        .fields(Greeting.fields().message())
        .build();
    Greeting greeting = getClient().sendRequest(request).getResponseEntity();

    Assert.assertFalse(greeting.hasId());
    Assert.assertFalse(greeting.hasTone());
    Assert.assertEquals(greeting.getMessage(), "Projected message!");
  }

  /**
   * Test for when the client does not send a projection.  The full entity should be included in the response.
   * @throws RemoteInvocationException
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testGetFull(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Request<Greeting> request = builders.get().id(1L).build();
    Greeting greeting = getClient().sendRequest(request).getResponseEntity();

    Assert.assertTrue(greeting.hasId());
    Assert.assertTrue(greeting.hasTone());
    Assert.assertEquals(greeting.getMessage(), "Full greeting.");
  }

  /**
   * Hack to verify that the rest.li framework's automatic projection facility is disabled when
   * "context.setProjectionMode(ProjectionMode.MANUAL)" is called by the server to disable automatic
   * projection.
   *
   * @throws RemoteInvocationException
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testDisableAutomaticProjection(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Request<Greeting> request = builders.get().id(1L)
        .setQueryParam("ignoreProjection", true)
        .fields(Greeting.fields().message())
        .build();
    Greeting greeting = getClient().sendRequest(request).getResponseEntity();

    Assert.assertTrue(greeting.hasId());  // these fields would have been excluded by the framework if automatic projection was enabled
    Assert.assertTrue(greeting.hasTone()); // "   "
    Assert.assertEquals(greeting.getMessage(), "Full greeting.");
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  private static Object[][] requestBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<Long, Greeting>(new ManualProjectionsBuilders()) },
      { new RootBuilderWrapper<Long, Greeting>(new ManualProjectionsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
      { new RootBuilderWrapper<Long, Greeting>(new ManualProjectionsRequestBuilders()) },
      { new RootBuilderWrapper<Long, Greeting>(new ManualProjectionsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) }
    };
  }
}
