package com.linkedin.d2.balancer.util.downstreams;

import com.linkedin.common.callback.SuccessCallback;
import com.linkedin.d2.xds.XdsClient;
import io.grpc.Status;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class fetches downstream services from INDIS.
 */
public final class IndisBasedDownstreamServicesFetcher implements DownstreamServicesFetcher {
  final private static Logger LOGGER = LoggerFactory.getLogger(IndisBasedDownstreamServicesFetcher.class);
  private final XdsClient _xdsClient;
  private final Duration _timeout;
  private final ScheduledExecutorService _executorService;
  private final DownstreamServicesFetcher _delegate;

  public IndisBasedDownstreamServicesFetcher(@Nonnull XdsClient xdsClient, @Nonnull Duration timeout,
      @Nonnull ScheduledExecutorService executorService, @Nonnull DownstreamServicesFetcher delegate) {
    _xdsClient = xdsClient;
    _timeout = timeout;
    _executorService = executorService;
    _delegate = delegate;
  }

  @Override
  public void getServiceNames(SuccessCallback<List<String>> callback) {
    _delegate.getServiceNames(callback);
  }

  @Override
  public void getServiceNames(String appName, String appInstance, String clientScope, SuccessCallback<List<String>> callback) {
    String resourceName = appName + ":" + appInstance + ":" + clientScope;
    LOGGER.info("Fetching xDS resource for callee services: {}", resourceName);

    _xdsClient.watchXdsResource(resourceName,
        new XdsClient.D2CalleesResourceWatcher() {
          private final AtomicBoolean _completed = new AtomicBoolean(false);
          private final ScheduledFuture<?> timeoutHandle =
              _executorService.schedule(() -> {
                if (_completed.compareAndSet(false, true)) {
                  LOGGER.warn("Timeout fetching xDS resource: {}, falling back to delegate", resourceName);
                  _delegate.getServiceNames(appName, appInstance, clientScope, callback);
                }
              }, _timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);

          @Override
          public void onChanged(XdsClient.D2CalleesUpdate update) {
            if (_completed.compareAndSet(false, true)) {
              timeoutHandle.cancel(false);
              if (update.isValid()) {
                List<String> services = update.getCalleeServices().getServicesList();
                LOGGER.info("Successfully retrieved {} callee services from INDIS for resource: {}",
                    services.size(), resourceName);
                LOGGER.debug("Callee services for {}: {}", resourceName, services);
                callback.onSuccess(services);
              } else {
                LOGGER.warn("Received invalid D2CalleesUpdate for resource: {}, falling back to delegate", resourceName);
                _delegate.getServiceNames(appName, appInstance, clientScope, callback);
              }
            }
          }

          @Override
          public void onError(Status error) {
            if (_completed.compareAndSet(false, true)) {
              LOGGER.error("Error fetching xDS resource: {}, error: {}, falling back to delegate", resourceName,
                  error.getDescription());
              timeoutHandle.cancel(false);
              _delegate.getServiceNames(appName, appInstance, clientScope, callback);
            }
          }

          @Override
          public void onReconnect() {
            if (!_completed.get()) {
              LOGGER.info("xDS connection reconnected for resource: {}", resourceName);
            }
          }
        });
  }
}
