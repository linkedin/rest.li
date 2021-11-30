package com.linkedin.restli.examples;

import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.r2.transport.http.common.HttpProtocolVersion;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.client.GreetingsBuilders;
import com.linkedin.restli.server.RestLiConfig;
import com.linkedin.restli.test.util.RootBuilderWrapper;
import java.util.HashMap;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


public class TestHttp11With204AndException extends RestLiIntegrationTest
{
  @BeforeClass
  public void initClass() throws Exception
  {
    Map<String, String> transportProperties = new HashMap<>();
    final String httpRequestTimeout = System.getProperty("test.httpRequestTimeout", "1000000");
    transportProperties.put(HttpClientFactory.HTTP_REQUEST_TIMEOUT, httpRequestTimeout);
    transportProperties.put(HttpClientFactory.HTTP_PROTOCOL_VERSION, HttpProtocolVersion.HTTP_1_1.name());
    super.init(false, new RestLiConfig(), transportProperties);
  }

  @AfterClass
  public void shutDown() throws Exception
  {
    super.shutdown();
  }

  @Test
  public void test204ExceptionWithHttp11() throws Exception
  {
    RootBuilderWrapper<Long, Greeting> builderWrapper = new RootBuilderWrapper<>(new GreetingsBuilders());
    Request<Greeting> request = builderWrapper.get()
        .id(204L)
        .build();

    Response<Greeting> response = getClient().sendRequest(request).getResponse();
    System.out.println(response.getHeaders().toString());
    Assert.assertEquals(response.getStatus(), HttpStatus.S_204_NO_CONTENT.getCode());
    Assert.assertEquals(response.getHeaders().get("X-RestLi-Error-Response"), "true");
    boolean isClientStreaming = Boolean.parseBoolean(System.getProperty("test.useStreamCodecClient", "false"));
    if (isClientStreaming)
    {
      Assert.assertNull(response.getHeaders().get("Content-Length"));
    }
    else
    {
      Assert.assertEquals(response.getHeaders().get("Content-Length"), "0");
    }

  }
}
