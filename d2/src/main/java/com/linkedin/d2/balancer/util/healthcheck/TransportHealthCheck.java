/*
   Copyright (c) 2016 LinkedIn Corp.

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

package com.linkedin.d2.balancer.util.healthcheck;


import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.util.clock.Clock;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * TransportHealthCheck implements the HealthCheck interface by sending single request through
 * the transportClient and confirms the correct response comes back within the given
 * time threshold.
 *
 * The restRequest for the health checking is supposed to be idempotent
 */
public class TransportHealthCheck implements HealthCheck
{
  private static final Logger _log = LoggerFactory.getLogger(TransportHealthCheck.class);

  final private Clock _clock;

  final private TransportClient _clientToCheck;

  // Request for healthChecking
  final private RestRequest _restRequest;
  final private Supplier<RequestContext> _requestContextSupplier;
  final private Supplier<Map<String, String>> _wireAttrsSupplier;

  final private HealthCheckResponseValidator _healthCheckResponseValidator;

  // HealthChecking criteria
  long _responseTimeThreshold;

  /**
   * @deprecated Use {@link TransportHealthCheck#TransportHealthCheck(Clock, TransportClient, RestRequest, Supplier, Supplier, HealthCheckResponseValidator, long)} instead.
   */
  @Deprecated
  public TransportHealthCheck(Clock clock, TransportClient client, RestRequest request,
                              RequestContext requestContext, Map<String, String> wireAttrs,
                              HealthCheckResponseValidator healthCheckResponseValidator, long threshold)
  {
    this(clock, client, request, () -> requestContext, () -> wireAttrs, healthCheckResponseValidator, threshold);
  }

  public TransportHealthCheck(Clock clock, TransportClient client, RestRequest request,
                              Supplier<RequestContext> requestContextSupplier, Supplier<Map<String, String>> wireAttrsSupplier,
                              HealthCheckResponseValidator healthCheckResponseValidator, long threshold)
  {
    _clock = clock;
    _clientToCheck = client;
    _restRequest = request;
    _requestContextSupplier = requestContextSupplier;
    _wireAttrsSupplier = wireAttrsSupplier;
    _healthCheckResponseValidator = healthCheckResponseValidator;
    _responseTimeThreshold = threshold;
  }

  @Override
  public void checkHealth(Callback<None> callback)
  {
    final long startTime = _clock.currentTimeMillis();

    TransportCallback<RestResponse> transportCallback = response -> {
      long delay = _clock.currentTimeMillis() - startTime;
      if (response.hasError())
      {
        // Currently treat all errors as failure
        _log.debug("checkHealth: error response for request ({}): {}", _restRequest.getURI(),
            response.getError());
        callback.onError(new Exception("Error from " + _restRequest.getURI() + " : " + response.getError()));
      }
      else if (delay > _responseTimeThreshold)
      {
        _log.debug("checkHealth: return delay ({}ms) longer than threshold for request {}", delay,
            _restRequest.getURI());
        callback.onError(new TimeoutException("HealthCheck Timeout: " + delay + "ms for " + _restRequest.getURI()));
      }
      else if (!_healthCheckResponseValidator.validateResponse(response.getResponse()))
      {
        _log.error("checkHealth: response validating error for request ({}): {}", _restRequest.getURI(),
            response);
        callback.onError(new Throwable("HealthCheck Response Error"));
      }
      else
      {
        _log.debug("checkHealth successful for client {}", _clientToCheck);

        callback.onSuccess(None.none());
      }
    };

    _clientToCheck.restRequest(_restRequest, _requestContextSupplier.get(), _wireAttrsSupplier.get(), transportCallback);
  }

  // For testing only
  public RequestContext getRequestContext()
  {
    return _requestContextSupplier.get();
  }

  public RestRequest getRestRequest()
  {
    return _restRequest;
  }
}
