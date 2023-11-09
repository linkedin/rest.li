package com.linkedin.d2.xds.LoadBalanceTaskPool;

import com.linkedin.common.callback.Callback;
import com.linkedin.d2.balancer.LoadBalancerWithFacilities;
import com.linkedin.d2.balancer.dualread.DualReadModeProvider;
import com.linkedin.d2.balancer.dualread.DualReadStateManager;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.UriProperties;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NewBalanceGetPropertiesTask implements Runnable {
  private LoadBalancerWithFacilities _newLb;
  private DualReadStateManager _dualReadStateManager;
  private String serviceName;
  private static final Logger LOG = LoggerFactory.getLogger(NewBalanceGetPropertiesTask.class);

  public NewBalanceGetPropertiesTask(LoadBalancerWithFacilities _newLb, DualReadStateManager _dualReadStateManager,
      String serviceName) {
    this._newLb = _newLb;
    this._dualReadStateManager = _dualReadStateManager;
    this.serviceName = serviceName;
  }

  @Override
  public void run() {
    _newLb.getLoadBalancedServiceProperties(serviceName, new Callback<ServiceProperties>() {
      @Override
      public void onError(Throwable e) {
        LOG.error("Double read failure. Unable to read service properties from: " + serviceName, e);
      }

      @Override
      public void onSuccess(ServiceProperties result) {
        String clusterName = result.getClusterName();
        _dualReadStateManager.updateCluster(clusterName, DualReadModeProvider.DualReadMode.DUAL_READ);
        _newLb.getLoadBalancedServiceProperties(serviceName, new Callback<ServiceProperties>() {
          @Override
          public void onError(Throwable e) {
            LOG.error("Double read failure. Unable to read service properties from: " + serviceName, e);
          }

          @Override
          public void onSuccess(ServiceProperties result) {
            String clusterName = result.getClusterName();
            _dualReadStateManager.updateCluster(clusterName, DualReadModeProvider.DualReadMode.DUAL_READ);
            _newLb.getLoadBalancedClusterAndUriProperties(clusterName,
                new Callback<Pair<ClusterProperties, UriProperties>>() {
                  @Override
                  public void onError(Throwable e) {
                    LOG.error("Dual read failure. Unable to read cluster properties from: " + clusterName, e);
                  }

                  @Override
                  public void onSuccess(Pair<ClusterProperties, UriProperties> result) {
                    //TODO change back to debug
                    LOG.info("Dual read is successful. Get cluster and uri properties: " + result);
                  }
                });
          }
        });
      }
    });
  }

  @Override
  public String toString() {
    return "NewBalanceGetPropertiesTask{" + "serviceName='" + serviceName + '\'' + '}';
  }
}

class NewBalanceTaskRejectedPolicy implements RejectedExecutionHandler {
  private static final Logger LOG = LoggerFactory.getLogger(NewBalanceTaskRejectedPolicy.class);
  public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
    // TODO log the rejected task info
    // TODO emit the rejected task info to a metric
    LOG.info(r.toString() + " is rejected");
  }
}

