package com.linkedin.d2.balancer.util.healthcheck;

import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.jersey.api.uri.UriBuilder;
import com.linkedin.util.clock.Clock;
import java.net.URI;
import java.net.URISyntaxException;

import static com.linkedin.r2.message.rest.RestMethod.OPTIONS;


/**
 * {@link HealthCheckClientBuilder} creates TransportHeathCheck client for health checking
 */

public class HealthCheckClientBuilder
{
  private HealthCheckOperations _healthOperations;
  private String _healthCheckPath;
  private String _servicePath;
  private Clock _clock;
  private long _latency;
  private TrackerClient _client;
  private String _method;

  public HealthCheckClientBuilder()
  {
    this(null, "", "", null, 0L, null, OPTIONS);
  }

  public HealthCheckClientBuilder(HealthCheckOperations ops, String path, String servicePath,
                                  Clock clk, long latency, TrackerClient client, String method)
  {
    _healthOperations = ops;
    _healthCheckPath = path;
    _servicePath = servicePath;
    _clock = clk;
    _latency = latency;
    _client = client;
    _method = method;
  }


  public HealthCheck build() throws URISyntaxException
  {
    URI curUri = _client.getUri();
    String fullPath = _healthCheckPath;

    if (_healthCheckPath == null || _healthCheckPath.isEmpty())
    {
      // If the path is not specified, always use the service's path
      fullPath = curUri.getPath();
      if (_servicePath != null && !_servicePath.isEmpty())
      {
        fullPath += _servicePath;
      }
    }
    UriBuilder uriBuilder = UriBuilder.fromUri(curUri);
    URI newUri = uriBuilder.replacePath(fullPath).build();

    HealthCheckOperations operations = _healthOperations;
    if (operations == null)
    {
      operations = new HealthCheckOperations();
    }

    return new TransportHealthCheck(_clock,
                                    _client.getTransportClient(),
                                    operations.buildRestRequest(_method, newUri),
                                    operations.buildRequestContextSupplier(),
                                    operations.buildWireAttributesSupplier(),
                                    operations.buildResponseValidate(),
                                    _latency);
  }

  public HealthCheckClientBuilder setHealthCheckOperations(HealthCheckOperations ops)
  {
    _healthOperations = ops;
    return this;
  }

  public HealthCheckClientBuilder setHealthCheckPath(String path)
  {
    _healthCheckPath = path;
    return this;
  }

  public HealthCheckClientBuilder setServicePath(String path)
  {
    _servicePath = path;
    return this;
  }

  public HealthCheckClientBuilder setClock(Clock clk)
  {
    _clock = clk;
    return this;
  }

  public HealthCheckClientBuilder setLatency(long latency)
  {
    _latency = latency;
    return this;
  }

  public HealthCheckClientBuilder setClient(TrackerClient client)
  {
    _client = client;
    return this;
  }

  public HealthCheckClientBuilder setMethod(String method)
  {
    _method = method;
    return this;
  }
}
