package com.linkedin.restli.examples;


import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.restli.client.GetRequest;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.client.ManualProjectionsBuilders;
import java.util.Collections;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


/**
 * Test a rest.li server that examines projection requests and manually applies the projection when building
 * the response.
 */
public class TestManualProjections extends RestLiIntegrationTest
{
  private static final Client CLIENT = new TransportClientAdapter(new HttpClientFactory().getClient(
      Collections.<String, String>emptyMap()));
  private static final String URI_PREFIX = "http://localhost:1338/";
  private static final RestClient REST_CLIENT = new RestClient(CLIENT, URI_PREFIX);
  private static final ManualProjectionsBuilders MANUAL_PROJECTIONS_BUILDERS = new ManualProjectionsBuilders();

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
  @Test
  public void testGetWithProjection() throws RemoteInvocationException
  {
    GetRequest<Greeting> request = MANUAL_PROJECTIONS_BUILDERS.get().id(1L)
        .fields(Greeting.fields().message())
        .build();
    Greeting greeting = REST_CLIENT.sendRequest(request).getResponseEntity();

    Assert.assertFalse(greeting.hasId());
    Assert.assertFalse(greeting.hasTone());
    Assert.assertEquals(greeting.getMessage(), "Projected message!");
  }

  /**
   * Test for when the client does not send a projection.  The full entity should be included in the response.
   * @throws RemoteInvocationException
   */
  @Test
  public void testGetFull() throws RemoteInvocationException
  {
    GetRequest<Greeting> request = MANUAL_PROJECTIONS_BUILDERS.get().id(1L).build();
    Greeting greeting = REST_CLIENT.sendRequest(request).getResponseEntity();

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
  @Test
  public void testDisableAutomaticProjection() throws RemoteInvocationException
  {
    GetRequest<Greeting> request = MANUAL_PROJECTIONS_BUILDERS.get().id(1L)
        .param("ignoreProjection", true)
        .fields(Greeting.fields().message())
        .build();
    Greeting greeting = REST_CLIENT.sendRequest(request).getResponseEntity();

    Assert.assertTrue(greeting.hasId());  // these fields would have been excluded by the framework if automatic projection was enabled
    Assert.assertTrue(greeting.hasTone()); // "   "
    Assert.assertEquals(greeting.getMessage(), "Full greeting.");
  }
}
