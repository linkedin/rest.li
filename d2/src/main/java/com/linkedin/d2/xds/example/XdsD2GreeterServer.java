package com.linkedin.d2.xds.example;

import com.linkedin.common.callback.Callback;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.transport.common.RestRequestHandler;
import com.linkedin.r2.transport.common.Server;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcherBuilder;
import com.linkedin.r2.transport.http.server.HttpServerFactory;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;


public class XdsD2GreeterServer
{
  public static final String GREETER_SERVICE_NAME = "greeter";
  private final int _port;

  public XdsD2GreeterServer(int port)
  {
    _port = port;
  }

  public void start() throws IOException
  {
    GreeterHandler greeterHandler = new GreeterHandler();
    TransportDispatcher transportDispatcher = new TransportDispatcherBuilder()
        .addRestHandler(URI.create("/" + GREETER_SERVICE_NAME), greeterHandler)
        .build();

    Server server = new HttpServerFactory().createServer(_port, transportDispatcher);
    server.start();
  }

  public static class GreeterHandler implements RestRequestHandler
  {
    @Override
    public void handleRequest(RestRequest request, RequestContext requestContext, Callback<RestResponse> callback)
    {
      String requestStr = request.getEntity().asString(StandardCharsets.UTF_8);
      String responseStr = "Hello " + requestStr;
      callback.onSuccess(new RestResponseBuilder().setEntity(responseStr.getBytes(StandardCharsets.UTF_8)).build());
    }
  }

  public static void main(String[] args) throws IOException
  {
    XdsD2GreeterServer server = new XdsD2GreeterServer(34567);
    server.start();
  }
}
