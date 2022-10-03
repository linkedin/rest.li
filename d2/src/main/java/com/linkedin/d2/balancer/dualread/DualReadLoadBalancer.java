package com.linkedin.d2.balancer.dualread;

import com.google.common.util.concurrent.RateLimiter;
import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.Callbacks;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.Directory;
import com.linkedin.d2.balancer.KeyMapper;
import com.linkedin.d2.balancer.LoadBalancerWithFacilities;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.util.ClusterInfoProvider;
import com.linkedin.d2.balancer.util.LoadBalancerUtil;
import com.linkedin.d2.balancer.util.hashing.HashRingProvider;
import com.linkedin.d2.balancer.util.partitions.PartitionInfoProvider;
import com.linkedin.d2.discovery.event.PropertyEventThread;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SuppressWarnings("UnstableApiUsage")
public class DualReadLoadBalancer implements LoadBalancerWithFacilities
{
  private static final Logger _log = LoggerFactory.getLogger(DualReadLoadBalancer.class);
  private final LoadBalancerWithFacilities _oldLb;
  private final LoadBalancerWithFacilities _newLb;
  private final DualReadModeProvider _dualReadModeProvider;
  private final ScheduledExecutorService _executorService;
  private final RateLimiter _rateLimiter;

  // Stores service-level dual read mode
  private final Map<String, DualReadModeProvider.DualReadMode> _serviceDualReadModes;
  // Stores global dual read mode
  private DualReadModeProvider.DualReadMode _dualReadMode = DualReadModeProvider.DualReadMode.OLD_LB_ONLY;
  private boolean _isNewLbReady;

  public DualReadLoadBalancer(LoadBalancerWithFacilities oldLb, LoadBalancerWithFacilities newLb,
      DualReadModeProvider dualReadModeProvider, ScheduledExecutorService executorService, int modeSwitchInterval)
  {
    _oldLb = oldLb;
    _newLb = newLb;
    _dualReadModeProvider = dualReadModeProvider;
    _executorService = executorService;
    _rateLimiter = RateLimiter.create((double) 1 / modeSwitchInterval);
    _serviceDualReadModes = new HashMap<>();
    _isNewLbReady = false;
  }

  @Override
  public void start(Callback<None> callback)
  {
    _newLb.start(new Callback<None>()
    {
      @Override
      public void onError(Throwable e)
      {
        _log.warn("Failed to start new load balancer. Fall back to read from old balancer only", e);
        _isNewLbReady = false;
      }

      @Override
      public void onSuccess(None result)
      {
        _log.info("New load balancer successfully started");
        _isNewLbReady = true;
      }
    });

    // Call back will succeed as long as the old balancer is successfully started. New load balancer failure
    // won't block application start up.
    _oldLb.start(callback);

    // Prefetch the global dual read mode
    _dualReadMode = _dualReadModeProvider.getDualReadMode();
  }

  @Override
  public void getClient(Request request, RequestContext requestContext, Callback<TransportClient> clientCallback)
  {
    String serviceName = LoadBalancerUtil.getServiceNameFromUri(request.getURI());
    switch (getDualReadMode(serviceName))
    {
      case NEW_LB_ONLY:
        _newLb.getClient(request, requestContext, clientCallback);
        break;
      case DUAL_READ:

        _newLb.getLoadBalancedServiceProperties(serviceName, new Callback<ServiceProperties>()
        {
          @Override
          public void onError(Throwable e)
          {
            _log.error("Double read failure. Unable to read service properties from: " + serviceName, e);
          }

          @Override
          public void onSuccess(ServiceProperties result)
          {
            String clusterName = result.getClusterName();
            _newLb.getLoadBalancedClusterProperties(clusterName, new Callback<Pair<ClusterProperties, UriProperties>>()
            {
              @Override
              public void onError(Throwable e)
              {
                _log.error("Dual read failure. Unable to read cluster properties from: " + clusterName, e);
              }

              @Override
              public void onSuccess(Pair<ClusterProperties, UriProperties> result)
              {
                _log.debug("Dual read is successful. Get cluster and uri properties: " + result);
              }
            });
          }
        });
        _oldLb.getClient(request, requestContext, clientCallback);
        break;
      case OLD_LB_ONLY:
      default:
        _oldLb.getClient(request, requestContext, clientCallback);
    }
  }

  @Override
  public void getLoadBalancedServiceProperties(String serviceName, Callback<ServiceProperties> clientCallback)
  {
    switch (getDualReadMode(serviceName))
    {
      case NEW_LB_ONLY:
        _newLb.getLoadBalancedServiceProperties(serviceName, clientCallback);
        break;
      case DUAL_READ:
        _newLb.getLoadBalancedServiceProperties(serviceName, Callbacks.empty());
        _oldLb.getLoadBalancedServiceProperties(serviceName, clientCallback);
        break;
      case OLD_LB_ONLY:
      default:
        _oldLb.getLoadBalancedServiceProperties(serviceName, clientCallback);
    }
  }

  @Override
  public void getLoadBalancedClusterProperties(String clusterName,
      Callback<Pair<ClusterProperties, UriProperties>> callback)
  {
    switch (getDualReadMode())
    {
      case NEW_LB_ONLY:
        _newLb.getLoadBalancedClusterProperties(clusterName, callback);
        break;
      case DUAL_READ:
        _newLb.getLoadBalancedClusterProperties(clusterName, Callbacks.empty());
        _oldLb.getLoadBalancedClusterProperties(clusterName, callback);
        break;
      case OLD_LB_ONLY:
      default:
        _oldLb.getLoadBalancedClusterProperties(clusterName, callback);
    }
  }

  @Override
  public Directory getDirectory()
  {
    DualReadModeProvider.DualReadMode dualReadMode = getDualReadMode();
    if (dualReadMode == DualReadModeProvider.DualReadMode.DUAL_READ
        || dualReadMode == DualReadModeProvider.DualReadMode.OLD_LB_ONLY)
    {
      return _oldLb.getDirectory();
    } else
    {
      return _newLb.getDirectory();
    }
  }

  @Override
  public PartitionInfoProvider getPartitionInfoProvider()
  {
    DualReadModeProvider.DualReadMode dualReadMode = getDualReadMode();
    if (dualReadMode == DualReadModeProvider.DualReadMode.DUAL_READ
        || dualReadMode == DualReadModeProvider.DualReadMode.OLD_LB_ONLY)
    {
      return _oldLb.getPartitionInfoProvider();
    } else
    {
      return _newLb.getPartitionInfoProvider();
    }
  }

  @Override
  public HashRingProvider getHashRingProvider()
  {
    DualReadModeProvider.DualReadMode dualReadMode = getDualReadMode();
    if (dualReadMode == DualReadModeProvider.DualReadMode.DUAL_READ
        || dualReadMode == DualReadModeProvider.DualReadMode.OLD_LB_ONLY)
    {
      return _oldLb.getHashRingProvider();
    } else
    {
      return _newLb.getHashRingProvider();
    }
  }

  @Override
  public KeyMapper getKeyMapper()
  {
    DualReadModeProvider.DualReadMode dualReadMode = getDualReadMode();
    if (dualReadMode == DualReadModeProvider.DualReadMode.DUAL_READ
        || dualReadMode == DualReadModeProvider.DualReadMode.OLD_LB_ONLY)
    {
      return _oldLb.getKeyMapper();
    } else
    {
      return _newLb.getKeyMapper();
    }
  }

  @Override
  public TransportClientFactory getClientFactory(String scheme)
  {
    DualReadModeProvider.DualReadMode dualReadMode = getDualReadMode();
    if (dualReadMode == DualReadModeProvider.DualReadMode.DUAL_READ
        || dualReadMode == DualReadModeProvider.DualReadMode.OLD_LB_ONLY)
    {
      return _oldLb.getClientFactory(scheme);
    } else
    {
      return _newLb.getClientFactory(scheme);
    }
  }

  @Override
  public ClusterInfoProvider getClusterInfoProvider()
  {
    DualReadModeProvider.DualReadMode dualReadMode = getDualReadMode();
    if (dualReadMode == DualReadModeProvider.DualReadMode.DUAL_READ
        || dualReadMode == DualReadModeProvider.DualReadMode.OLD_LB_ONLY)
    {
      return _oldLb.getClusterInfoProvider();
    } else
    {
      return _newLb.getClusterInfoProvider();
    }
  }

  /**
   * Asynchronously check and update the dual read mode for the given D2 service
   * @param d2ServiceName the name of the D2 service
   */
  public void checkAndSwitchMode(String d2ServiceName)
  {
    _executorService.submit(() ->
    {
      boolean shouldCheck = _rateLimiter.tryAcquire();
      if (shouldCheck)
      {
        // Check and switch global dual read mode
        _dualReadMode = _dualReadModeProvider.getDualReadMode();
        _log.debug("Global dual read mode updated: " + _dualReadMode);

        // Check and switch service-level dual read mode}
        if (d2ServiceName != null)
        {
          DualReadModeProvider.DualReadMode mode = _dualReadModeProvider.getDualReadMode(d2ServiceName);
          _serviceDualReadModes.put(d2ServiceName, mode);
          _log.debug("Dual read mode for service " + d2ServiceName + " updated: " + mode);
        }
      }
    });
  }

  private DualReadModeProvider.DualReadMode getDualReadMode()
  {
    if (!_isNewLbReady)
    {
      return DualReadModeProvider.DualReadMode.OLD_LB_ONLY;
    }

    checkAndSwitchMode(null);
    return _dualReadMode;
  }

  private DualReadModeProvider.DualReadMode getDualReadMode(String d2ServiceName)
  {
    if (!_isNewLbReady)
    {
      return DualReadModeProvider.DualReadMode.OLD_LB_ONLY;
    }

    checkAndSwitchMode(d2ServiceName);
    return _serviceDualReadModes.getOrDefault(d2ServiceName, _dualReadMode);
  }

  @Override
  public void shutdown(PropertyEventThread.PropertyEventShutdownCallback callback)
  {
    _newLb.shutdown(() ->
    {
      _log.info("New load balancer successfully shut down");
    });

    _oldLb.shutdown(callback);
  }
}
