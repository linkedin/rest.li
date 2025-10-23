/*
   Copyright (c) 2018 LinkedIn Corp.

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

package com.linkedin.d2.balancer.util.downstreams;

import com.linkedin.common.callback.SuccessCallback;
import java.util.List;


/**
 * The interface should return the list of services that will be probably contacted by D2 during its lifecycle.
 *
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public interface DownstreamServicesFetcher
{
  void getServiceNames(SuccessCallback<List<String>> callback);
  default void getServiceNames(String appName, String appInstance, String clientScope, SuccessCallback<List<String>> callback)
  {
    getServiceNames(callback);
  }
}
