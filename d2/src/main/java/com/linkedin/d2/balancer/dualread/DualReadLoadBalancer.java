/*
   Copyright (c) 2023 LinkedIn Corp.

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

package com.linkedin.d2.balancer.dualread;

import com.google.common.util.concurrent.MoreExecutors;
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
import com.linkedin.util.RateLimitedLogger;
import com.linkedin.util.clock.SystemClock;
import java.util.concurrent.ExecutorService;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A load balancer that supports dual read from two different service discovery data sources. It can be
 * used to roll out a new load balancer by reading from it, monitoring it, but still rely on the old
 * balancer to do the actual service discovery. This helps validate the correctness and efficiency of
 * the new load balancer and ensures a safer transition from the old load balancer to the new load balancer.
 *
 * If supports three read modes, OLD_LB_ONLY, NEW_LB_ONLY and DUAL_READ.
 *
 * In OLD_LB_ONLY mode, it reads exclusively from the old load balancer.
 * In NEW_LB_ONLY mode, it reads exclusively from the new load balancer.
 * In DUAL_READ mode, it reads from both the old and the new load balancer, but relies on the data from old
 * load balancer only.
 */
public class DualReadLoadBalancer implements LoadBalancerWithFacilities
{
  private static final Logger LOG = LoggerFactory.getLogger(DualReadLoadBalancer.class);
  private final RateLimitedLogger _rateLimitedLogger;
  private static final long ERROR_REPORT_PERIOD = 10 * 1000; // Limit error report logging to every 10 seconds
  private final LoadBalancerWithFacilities _oldLb;
  private final LoadBalancerWithFacilities _newLb;
  private final DualReadStateManager _dualReadStateManager;
  private ExecutorService _newLbExecutor;
  private boolean _isNewLbReady;

  @Deprecated
  public DualReadLoadBalancer(LoadBalancerWithFacilities oldLb, LoadBalancerWithFacilities newLb,
      @Nonnull DualReadStateManager dualReadStateManager)
  {
    this(oldLb, newLb, dualReadStateManager, null);
  }

  public DualReadLoadBalancer(LoadBalancerWithFacilities oldLb, LoadBalancerWithFacilities newLb,
      @Nonnull DualReadStateManager dualReadStateManager, ExecutorService newLbExecutor)
  {
    _rateLimitedLogger = new RateLimitedLogger(LOG, ERROR_REPORT_PERIOD, SystemClock.instance());
    _oldLb = oldLb;
    _newLb = newLb;
    _dualReadStateManager = dualReadStateManager;
    _isNewLbReady = false;
    if (newLbExecutor == null)
    {
      // Using a direct executor here means the code is executed directly,
      // blocking the caller. This means the old behavior is preserved.
      _newLbExecutor = MoreExecutors.newDirectExecutorService();
      LOG.warn("The newLbExecutor is null, will use a direct executor instead.");
    }
    else
    {
      _newLbExecutor = newLbExecutor;
    }
  }

  @Override
  public void start(Callback<None> callback)
  {
    // Prefetch the global dual read mode
    DualReadModeProvider.DualReadMode mode = _dualReadStateManager.getGlobalDualReadMode();

    // if in new-lb-only mode, new lb needs to start successfully to call the callback. Otherwise, the old lb does.
    // Use a separate executor service to start the new lb, so both lbs can start concurrently.
    if (!_newLbExecutor.isShutdown())
    {
      _newLbExecutor.execute(() -> _newLb.start(getStartUpCallback(true,
              mode == DualReadModeProvider.DualReadMode.NEW_LB_ONLY ? callback : null)
      ));
    }

    _oldLb.start(getStartUpCallback(false,
        mode == DualReadModeProvider.DualReadMode.NEW_LB_ONLY ? null : callback
    ));
  }

  private Callback<None> getStartUpCallback(boolean isForNewLb, Callback<None> callback)
  {
    return new Callback<None>() {
      @Override
      public void onError(Throwable e) {
        LOG.warn("Failed to start {} load balancer.", isForNewLb ? "new" : "old", e);
        if (isForNewLb)
        {
          _isNewLbReady = false;
        }

        if (callback != null)
        {
          callback.onError(e);
        }
      }

      @Override
      public void onSuccess(None result) {
        LOG.info("{} load balancer successfully started", isForNewLb ? "New" : "Old");
        if (isForNewLb)
        {
          _isNewLbReady = true;
        }

        if (callback != null)
        {
          callback.onSuccess(None.none());
        }
      }
    };
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
        if (_newLbExecutor.isShutdown())
        {
          _rateLimitedLogger.info("newLb executor is shutdown already. Skipping getClient on newLb executor.");
        }
        else
        {
          _newLbExecutor.execute(() -> _newLb.getLoadBalancedServiceProperties(serviceName, new Callback<ServiceProperties>()
          {
            @Override
            public void onError(Throwable e)
            {
              _rateLimitedLogger.error("Dual read failure. Unable to read service properties from: {}", serviceName, e);
            }

            @Override
            public void onSuccess(ServiceProperties result)
            {
              String clusterName = result.getClusterName();
              _dualReadStateManager.updateCluster(clusterName, DualReadModeProvider.DualReadMode.DUAL_READ);
              _newLb.getLoadBalancedClusterAndUriProperties(clusterName, new Callback<Pair<ClusterProperties, UriProperties>>()
              {
                @Override
                public void onError(Throwable e)
                {
                  _rateLimitedLogger.error("Dual read failure. Unable to read cluster and uri properties " + "from: {}", clusterName, e);
                }

                @Override
                public void onSuccess(Pair<ClusterProperties, UriProperties> result)
                {
                  LOG.debug("Dual read is successful. Get cluster and uri properties: {}", result);
                }
              });
            }
          }));
        }
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
        if (_newLbExecutor.isShutdown())
        {
          _rateLimitedLogger.info("newLb executor is shutdown already. Skipping getLoadBalancedServiceProperties on newLb executor.");
        }
        else
        {
          _newLbExecutor.execute(() -> _newLb.getLoadBalancedServiceProperties(serviceName, Callbacks.empty()));
        }
        _oldLb.getLoadBalancedServiceProperties(serviceName, clientCallback);
        break;
      case OLD_LB_ONLY:
      default:
        _oldLb.getLoadBalancedServiceProperties(serviceName, clientCallback);
    }
  }

  @Override
  public void getLoadBalancedClusterAndUriProperties(String clusterName,
      Callback<Pair<ClusterProperties, UriProperties>> callback)
  {
    switch (getDualReadMode())
    {
      case NEW_LB_ONLY:
        _newLb.getLoadBalancedClusterAndUriProperties(clusterName, callback);
        break;
      case DUAL_READ:
        if (_newLbExecutor.isShutdown())
        {
          _rateLimitedLogger.info("newLb executor is shutdown already. Skipping getLoadBalancedClusterAndUriProperties on newLb executor.");
        }
        else
        {
          _newLbExecutor.execute(() -> _newLb.getLoadBalancedClusterAndUriProperties(clusterName, Callbacks.empty()));
        }
        _oldLb.getLoadBalancedClusterAndUriProperties(clusterName, callback);
        break;
      case OLD_LB_ONLY:
      default:
        _oldLb.getLoadBalancedClusterAndUriProperties(clusterName, callback);
    }
  }

  @Override
  public Directory getDirectory()
  {
    if (shouldReadFromOldLb())
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
    if (shouldReadFromOldLb())
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
    if (shouldReadFromOldLb())
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
    if (shouldReadFromOldLb())
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
    if (shouldReadFromOldLb())
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
    if (shouldReadFromOldLb())
    {
      return _oldLb.getClusterInfoProvider();
    } else
    {
      return _newLb.getClusterInfoProvider();
    }
  }

  private boolean shouldReadFromOldLb()
  {
    DualReadModeProvider.DualReadMode dualReadMode = getDualReadMode();
    return (dualReadMode == DualReadModeProvider.DualReadMode.DUAL_READ
        || dualReadMode == DualReadModeProvider.DualReadMode.OLD_LB_ONLY);
  }

  private DualReadModeProvider.DualReadMode getDualReadMode()
  {
    if (!_isNewLbReady)
    {
      return DualReadModeProvider.DualReadMode.OLD_LB_ONLY;
    }

    return _dualReadStateManager.getGlobalDualReadMode();
  }

  private DualReadModeProvider.DualReadMode getDualReadMode(String d2ServiceName)
  {
    if (!_isNewLbReady)
    {
      return DualReadModeProvider.DualReadMode.OLD_LB_ONLY;
    }

    return _dualReadStateManager.getServiceDualReadMode(d2ServiceName);
  }

  @Override
  public void shutdown(PropertyEventThread.PropertyEventShutdownCallback callback)
  {
    _newLb.shutdown(() ->
    {
      LOG.info("New load balancer successfully shut down");
    });

    _oldLb.shutdown(callback);
  }
}
