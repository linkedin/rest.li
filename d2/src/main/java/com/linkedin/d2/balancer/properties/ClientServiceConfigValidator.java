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

package com.linkedin.d2.balancer.properties;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Validates values for configs provided by the clients
 *
 * @author Karan Parikh
 */
public class ClientServiceConfigValidator
{
  private static final Logger _log = LoggerFactory.getLogger(ClientServiceConfigValidator.class);

  public static boolean isValidValue(Map<String, Object> serviceSuppliedConfig,
                                     Map<String, Object> clientSuppliedServiceConfig,
                                     String propertyName)
  {
    // prevent clients from violating SLAs as published by the service
    if (propertyName.equals(PropertyKeys.HTTP_REQUEST_TIMEOUT))
    {
      String clientSuppliedTimeout = (String)clientSuppliedServiceConfig.get(propertyName);
      String serviceSuppliedTimeout = (String)serviceSuppliedConfig.get(propertyName);
      try
      {
        return Integer.parseInt(clientSuppliedTimeout) >= Integer.parseInt(serviceSuppliedTimeout);
      }
      catch (NumberFormatException e)
      {
        _log.error("Failed to convert HTTP Request Timeout to an int. clientSuppliedTimeout is " + clientSuppliedTimeout
                       + ". serviceSuppliedTimeout is " + serviceSuppliedTimeout, e);
        return false;
      }
    }
    return true;
  }
}
