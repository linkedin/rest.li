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
package com.linkedin.restli.server.util;

import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.restspec.ResourceEntityType;
import com.linkedin.restli.server.resources.unstructuredData.KeyUnstructuredDataResource;
import com.linkedin.restli.server.resources.unstructuredData.SingleUnstructuredDataResource;

/**
 * This class provides methods useful for UnstructuredData service.
 **/

public class UnstructuredDataUtil {
  public static ResourceEntityType getResourceEntityType(Class<?> clazz)
  {
    if (isUnstructuredDataClass(clazz))
    {
      return ResourceEntityType.UNSTRUCTURED_DATA;
    }
    return ResourceEntityType.STRUCTURED_DATA;
  }

  public static boolean isUnstructuredDataRouting(RoutingResult routingResult)
  {
    return isUnstructuredDataClass(routingResult.getResourceMethod().getMethod().getDeclaringClass());
  }

  public static boolean isUnstructuredDataClass(Class<?> clazz)
  {
    return KeyUnstructuredDataResource.class.isAssignableFrom(clazz) ||
        SingleUnstructuredDataResource.class.isAssignableFrom(clazz);
  }
}
