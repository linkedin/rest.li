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

/**
 * $Id: $
 */

package com.linkedin.d2.balancer;

import com.linkedin.common.callback.Callback;

import java.util.List;

/**
 * The Directory allows querying of the current list of services or clusters.  All methods
 * on the directory may involve remote calls.
 * @author Steven Ihde
 * @version $Revision: $
 */

public interface Directory
{
  /**
   * Get the list of services
   * @param callback a callback which will be passed a list of all service names within the directory's scope
   */
  void getServiceNames(Callback<List<String>> callback);

  /**
   * Get the list of clusters
   * @param callback a callback which will be passed a list of all cluster names within the directory's scope
   */
  void getClusterNames(Callback<List<String>> callback);
}
