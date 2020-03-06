/*
   Copyright (c) 2020 LinkedIn Corp.

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

package com.linkedin.darkcluster;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.linkedin.d2.balancer.LoadBalancerTestState;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancer;
import com.linkedin.d2.balancer.util.ClusterInfoProvider;
import com.linkedin.darkcluster.api.DarkClusterDispatcher;
import com.linkedin.darkcluster.api.DarkClusterManager;
import com.linkedin.darkcluster.api.DarkClusterStrategy;
import com.linkedin.darkcluster.impl.BaseDarkClusterDispatcher;
import com.linkedin.darkcluster.filter.DarkClusterFilter;
import com.linkedin.darkcluster.impl.DarkClusterManagerImpl;
import com.linkedin.darkcluster.impl.DefaultDarkClusterDispatcherImpl;
import com.linkedin.darkcluster.impl.RelativeTrafficDarkCanaryStrategy;

import org.testng.annotations.Test;

public class TestDarkClusterFilter
{
  @Test
  public void testDarkClusterAssembly()
  {
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    DarkClusterDispatcher darkClusterDispatcher = new DefaultDarkClusterDispatcherImpl(new MockClient());
    ClusterInfoProvider clusterInfoProvider = new SimpleLoadBalancer(new LoadBalancerTestState(), scheduledExecutorService);
    BaseDarkClusterDispatcher baseDarkClusterDispatcher = new BaseDarkClusterDispatcher("myDarkCluster",
                                                                                        darkClusterDispatcher,
                                                                                        new DoNothingNotifier(),
                                                                                        null,
                                                                                        scheduledExecutorService);
    DarkClusterStrategy darkClusterStrategy = new RelativeTrafficDarkCanaryStrategy(clusterInfoProvider,
                                                                                    "MyCluster",
                                                                                    baseDarkClusterDispatcher);
    DarkClusterManager darkClusterManager = new DarkClusterManagerImpl(scheduledExecutorService,
                                                                       "", "",
                                                                       new DoNothingNotifier(),
                                                                       darkClusterStrategy, null);
    DarkClusterFilter darkClusterFilter = new DarkClusterFilter(darkClusterManager);
  }
}
