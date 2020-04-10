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

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.linkedin.common.util.Notifier;
import com.linkedin.d2.balancer.LoadBalancerTestState;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancer;
import com.linkedin.d2.balancer.util.ClusterInfoProvider;
import com.linkedin.darkcluster.api.DarkClusterDispatcher;
import com.linkedin.darkcluster.api.DarkClusterManager;
import com.linkedin.darkcluster.api.DarkClusterStrategyFactory;
import com.linkedin.darkcluster.api.DarkClusterVerifier;
import com.linkedin.darkcluster.api.DarkClusterVerifierManager;
import com.linkedin.darkcluster.api.NoOpDarkClusterVerifier;
import com.linkedin.darkcluster.filter.DarkClusterFilter;
import com.linkedin.darkcluster.impl.DarkClusterManagerImpl;
import com.linkedin.darkcluster.impl.DarkClusterStrategyFactoryImpl;
import com.linkedin.darkcluster.impl.DarkClusterVerifierManagerImpl;
import com.linkedin.darkcluster.impl.DefaultDarkClusterDispatcherImpl;

import org.testng.annotations.Test;

public class TestDarkClusterFilter
{
  @Test
  public void testDarkClusterAssembly()
  {
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    DarkClusterDispatcher darkClusterDispatcher = new DefaultDarkClusterDispatcherImpl(new MockClient(false));
    ClusterInfoProvider clusterInfoProvider = new SimpleLoadBalancer(new LoadBalancerTestState(), scheduledExecutorService);
    Notifier notifier = new DoNothingNotifier();
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    String sourceClusterName = "MyCluster";
    DarkClusterVerifier darkClusterVerifier = new NoOpDarkClusterVerifier();
    DarkClusterVerifierManager verifierManager = new DarkClusterVerifierManagerImpl(darkClusterVerifier, executorService);
    Random random = new Random();

    DarkClusterStrategyFactory darkClusterStrategyFactory = new DarkClusterStrategyFactoryImpl(clusterInfoProvider, sourceClusterName,
                                                                                               darkClusterDispatcher,
                                                                                               notifier, random,
                                                                                               verifierManager);

    DarkClusterManager darkClusterManager = new DarkClusterManagerImpl(sourceClusterName,
                                                                       clusterInfoProvider,
                                                                       darkClusterStrategyFactory,
                                                                       "", "",
                                                                       notifier);
    DarkClusterFilter darkClusterFilter = new DarkClusterFilter(darkClusterManager, verifierManager);
  }
}
