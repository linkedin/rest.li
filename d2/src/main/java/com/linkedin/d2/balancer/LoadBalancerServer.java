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

package com.linkedin.d2.balancer;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.properties.PartitionData;

import java.net.URI;
import java.util.Map;

public interface LoadBalancerServer
{
  void markUp(String clusterName, URI uri, Map<Integer, PartitionData> partitionDataMap, Callback<None> callback);

  void markDown(String clusterName, URI uri, Callback<None> callback);

  void start(Callback<None> callback);

  void shutdown(Callback<None> callback);
}
