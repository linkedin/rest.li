/*
   Copyright (c) 2017 LinkedIn Corp.

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

/**
 * WarmUpService provides capabilities of warming up the Load Balancer state given a service name
 *
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public interface WarmUpService
{
  void warmUpService(String serviceName, Callback<None> callback);
}
