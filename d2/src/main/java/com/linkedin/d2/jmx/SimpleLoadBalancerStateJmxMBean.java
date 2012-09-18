/*
   Copyright (c) 2012 LinkedIn Corp.

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

package com.linkedin.d2.jmx;

import java.util.List;

public interface SimpleLoadBalancerStateJmxMBean
{
  int getUriCount();

  int getClusterCount();

  int getServiceCount();

  long getVersion();

  int getClusterListenCount();

  int getServiceListenCount();

  int getListenerCount();

  List<String> getSupportedStrategies();

  List<String> getSupportedSchemes();

  int getTrackerClientCount(String clusterName);

  String getUriProperty(String clusterName);

  String getClusterProperty(String clusterName);

  String getServiceProperty(String serviceName);

  boolean isListeningToCluster(String clusterName);

  boolean isListeningToService(String serviceName);

  void setVersion(long version);

  void listenToService(final String serviceName);

  void listenToCluster(final String clusterName);
}
