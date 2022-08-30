package com.linkedin.d2.xds.example;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.d2.balancer.D2Client;
import com.linkedin.d2.balancer.D2ClientBuilder;
import com.linkedin.d2.xds.balancer.XdsLoadBalancerWithFacilitiesFactory;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


public class XdsD2GreeterClient
{
  private final D2Client _client;

  public XdsD2GreeterClient()
  {
    _client = new D2ClientBuilder()
        .setXdsServer("localhost:15010")
        .setLoadBalancerWithFacilitiesFactory(new XdsLoadBalancerWithFacilitiesFactory())
        .build();
  }

  public void start()
  {
    _client.start(new FutureCallback<>());
  }

  public void sendGreeterRequest()
  {
    URI uri = URI.create("d2://grpc-demo-service-1.prod.linkedin.com");
    RestRequest request = new RestRequestBuilder(uri)
        .setEntity("rest.li D2".getBytes(StandardCharsets.UTF_8))
        .build();

    try
    {
      Future<RestResponse> response = _client.restRequest(request);
      String responseString = response.get().getEntity().asString(StandardCharsets.UTF_8);

      System.err.println(uri + " response: " + responseString);
    }
    catch (ExecutionException | InterruptedException e)
    {
      System.err.println("future.get() failed for " + uri + ": " + e);
    }
  }

  public static void main(String[] args)
  {
    XdsD2GreeterClient client = new XdsD2GreeterClient();
    client.start();
    client.sendGreeterRequest();
  }
}
