/*
   Copyright (c) 2013 LinkedIn Corp.

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


/**
 * Factory for creating instance of {@link LoadBalancerWithFacilities}
 */
public interface LoadBalancerWithFacilitiesFactory
{

  /**
   * Creates instance of {@link LoadBalancerWithFacilities}
   * @param config configuration of d2 client
   * @return new instance of {@link LoadBalancerWithFacilities}
   */
  LoadBalancerWithFacilities create(D2ClientConfig config);

  /**
   * Set whether the client is a raw d2 client or not.
   * @param isRawD2Client true if the client is a raw d2 client, false otherwise.
   */
  void setIsLiRawD2Client(boolean isRawD2Client);
}
