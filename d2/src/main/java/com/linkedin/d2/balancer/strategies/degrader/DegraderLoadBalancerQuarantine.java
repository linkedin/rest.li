package com.linkedin.d2.balancer.strategies.degrader;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.util.RateLimitedLogger;
import com.linkedin.d2.balancer.util.healthcheck.HealthCheck;
import com.linkedin.d2.balancer.util.healthcheck.HealthCheckClientBuilder;
import com.linkedin.util.clock.Clock;
import java.net.URISyntaxException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * DegraderLoadBalancerQuarantine quarantines the TrackerClients with problems. The advantages
 * of using quarantine includes:
 *
 * . Quick isolating the single host/service failures
 * . Can use sideband idempotent requests (instead of real traffic) to check/monitor the hosts
 *   with problem.
 * . Exponential backoff checking avoid unnecessary operations for bad hosts/networks.
 *
 *   The quarantine state transition:
 *
 * +-----------------+   Send Reqs    +----------------+         +------------------+
 * |                 +--------------->|                | success |                  |
 * |    FAILURE      |                |     WAIT       +-------->|     SUCCESS      |
 * |                 |<---------------+                |         |                  |
 * +-----------------+   Req Failed   +----------------+         +------------------+
 *     (exponential backoff before send req again)
 *
 *
 * Note: DegraderLoadBalancerQuarantine is not thread safe and supposed to updated only under the
 * lock of PartitionState update.
 */

public class DegraderLoadBalancerQuarantine
{
  private enum QuarantineStates
  {
    FAILURE,
    WAIT,
    SUCCESS,
    DISABLED,
  }

  private static final Logger _log = LoggerFactory.getLogger(DegraderLoadBalancerQuarantine.class);
  private static final long ERROR_REPORT_PERIOD = 60 * 1000; // Millisecond = 1 minute
  private volatile QuarantineStates _quarantineState;
  // TrackerClient with problem
  final private TrackerClient _trackerClient;
  final private HealthCheck _healthCheckClient;
  final private String _serviceName;

  final private ScheduledExecutorService _executorService;
  final private Clock _clock;

  final private long _timeBetweenHC;

  private volatile boolean _isShutdown;

  private long _lastChecked;
  // Waiting duration, ie the exponential back off time
  private long _timeTilNextCheck;

  final private DegraderLoadBalancerStrategyConfig _config;
  private final RateLimitedLogger _rateLimitedLogger;

  DegraderLoadBalancerQuarantine(TrackerClientUpdater client, DegraderLoadBalancerStrategyConfig config,
                                 String serviceName)
  {
    _trackerClient = client.getTrackerClient();
    _config = config;
    _executorService = config.getExecutorService();
    _clock = config.getClock();
    _timeBetweenHC = DegraderLoadBalancerStrategyConfig.DEFAULT_QUARANTINE_CHECK_INTERVAL;
    _serviceName = serviceName;

    _quarantineState = QuarantineStates.FAILURE;
    // Initial interval is the same as update interval
    _timeTilNextCheck = config.getUpdateIntervalMs();
    _lastChecked = Integer.MIN_VALUE;
    _isShutdown = false;
    _rateLimitedLogger = new RateLimitedLogger(_log, ERROR_REPORT_PERIOD, config.getClock());

    if (_timeBetweenHC < _config.getQuarantineLatency())
    {
      _log.error("Illegal quarantine configurations for service {}: Interval {} too short", _serviceName, _timeBetweenHC);
      throw new IllegalArgumentException("Quarantine interval too short");
    }

    // create healthCheckClient for the trackerClient. The quarantine object will be saved for future
    // use so this only need once for each trackerClient.
    HealthCheck healthCheckClient = null;
    try
    {
      healthCheckClient = new HealthCheckClientBuilder()
          .setHealthCheckOperations(config.getHealthCheckOperations())
          .setHealthCheckPath(config.getHealthCheckPath())
          .setServicePath(config.getServicePath())
          .setClock(config.getClock())
          .setLatency(config.getQuarantineLatency())
          .setMethod(config.getHealthCheckMethod())
          .setClient(_trackerClient)
          .build();
    }
    catch (URISyntaxException e)
    {
      _log.error("Error to generate healthCheckClient", e);
    }
    _healthCheckClient = healthCheckClient;
  }

  /**
   * healthCheckNTimes responsible for checking the health of the transportClient multiple times
   * at the given interval.
   *
   * @param n repeat times
   */
  private void healthCheckNTimes(int n)
  {
    if (n <= 0 || _isShutdown)
    {
      return;
    }

    final long startTime = _clock.currentTimeMillis();
    Callback<None> healthCheckCallback = new Callback<None>()
    {
      @Override
      public void onError(Throwable e)
      {
        _rateLimitedLogger.warn("Healthchecking failed for {} (service={}): {}", new Object[] {_trackerClient.getUri(),
                    _serviceName, e});
        _quarantineState = QuarantineStates.FAILURE;
      }

      @Override
      public void onSuccess(None result)
      {
        if (n > 1)
        {
          // do not schedule next checking if _isShutdown flag is set
          if (!_isShutdown)
          {
            // schedule next check
            long nextCheckDelay = _timeBetweenHC - (_clock.currentTimeMillis() - startTime);
            if (nextCheckDelay > 0)
            {
              _executorService.schedule(() -> healthCheckNTimes(n - 1), nextCheckDelay, TimeUnit.MILLISECONDS);
            }
            else
            {
              // should never happen since the delay time should be within the range for a successful callback.
              _log.error("Delay exceeded the defined checking interval");
            }
          }
        }
        else
        {
          _quarantineState = QuarantineStates.SUCCESS;
        }
      }
    };

    _healthCheckClient.checkHealth(healthCheckCallback);
  }

  /**
   * Check and update the quarantine state
   * @return: true if current client is ready to exist quarantine, false otherwise.
   */
  boolean checkUpdateQuarantineState()
  {
    long currentTime = _config.getClock().currentTimeMillis();
    _lastChecked = currentTime;
    int repeatNum = DegraderLoadBalancerStrategyConfig.DEFAULT_QUARANTINE_CHECKNUM;

    switch(_quarantineState)
    {
      case DISABLED:
        throw new IllegalStateException("State update for disabled quarantine");
      case FAILURE:
        if (_isShutdown)
        {
          _log.error("Could not check quarantine state since the executor is shutdown");
        }
        else
        {
          // Either this is a newly quarantined host, or previous checking fails.
          // Schedule new health checking task
          _executorService.schedule(() -> healthCheckNTimes(repeatNum), _timeTilNextCheck, TimeUnit.MILLISECONDS);
          // exponential backoff: double the interval time
          _timeTilNextCheck *= 2;
          _quarantineState = QuarantineStates.WAIT;
        }
        break;
      case WAIT:
        // Nothing to do for now. Just keep waiting
        if (_timeTilNextCheck > ERROR_REPORT_PERIOD)
        {
          _rateLimitedLogger.error("Client {}  for service {} is being kept in quarantine for {} seconds, "
              + "Please check to make sure it is healthy", new Object[] {_trackerClient.getUri(), _serviceName,
              (1.0 *_timeTilNextCheck / 1000)});
        }
        break;
      case SUCCESS:
        // success! ready to evict current trackerclient out of quarantine
        _quarantineState = QuarantineStates.DISABLED;
        _log.info("checkUpdateQuarantineState: quarantine state for client {} service {} is DISABLED",
                  _trackerClient.getUri(), _serviceName);
        return true;
    }

    return false;
  }

  /**
   * To shutdown quarantine, we only need to stop sending new requests.
   * Shutting down the executor is not feasible, because it is shared among strategies.
   */
  public void shutdown()
  {
    if (_isShutdown)
    {
      _log.error("Quarantine already shutdown");
      return;
    }
    _isShutdown = true;
  }

  /**
   * When resetInterval set to true, reset the interval time to Update Interval time.
   * Otherwise reuse the existing interval time
   * @param resetInterval
   */
  public void reset(boolean resetInterval)
  {
    _quarantineState = QuarantineStates.FAILURE;

    if (resetInterval)
    {
      _timeTilNextCheck = _config.getUpdateIntervalMs();
    }
    else
    {
      _log.warn("HealthCheck: Interval {}ms is not reset for client {}, because it is quarantined again within 30s. "
          + "This can happen if current health checking method is not sufficient for capturing when a node should stay in quarantine, "
          + "for example it returns fast but the real queries return slow.",
          _timeTilNextCheck, _trackerClient.getUri());
    }
  }

  long getLastChecked()
  {
    return _lastChecked;
  }

  public long getTimeTilNextCheck()
  {
    return _timeTilNextCheck;
  }

  // For testing only
  HealthCheck getHealthCheckClient()
  {
    return _healthCheckClient;
  }

  @Override
  public String toString()
  {
    return "TrackerClientQuarantine [_client=" + _trackerClient.getUri()
        + ", _quarantineState=" + _quarantineState
        + ", _timeTilNextCheck=" + (_timeTilNextCheck / 1000) + "s"
        +"]";
  }
}
