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


package com.linkedin.restli.internal.server.response;

import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.internal.server.ResponseType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Utility class to help determine the {@link ResponseType} for any given {@link ResourceMethod}.
 *
 * @author Evan Williams
 */
public class ResponseTypeUtil
{
  private static final Set<ResourceMethod> DYNAMICALLY_DETERMINED = new HashSet<>(Arrays.asList(ResourceMethod.PARTIAL_UPDATE));

  private static final Map<ResourceMethod, ResponseType> BY_RESOURCE_METHOD;
  static
  {
    BY_RESOURCE_METHOD = new HashMap<>();
    BY_RESOURCE_METHOD.put(ResourceMethod.GET,                  ResponseType.SINGLE_ENTITY);
    BY_RESOURCE_METHOD.put(ResourceMethod.ACTION,               ResponseType.SINGLE_ENTITY);
    BY_RESOURCE_METHOD.put(ResourceMethod.CREATE,               ResponseType.SINGLE_ENTITY);
    BY_RESOURCE_METHOD.put(ResourceMethod.GET_ALL,              ResponseType.GET_COLLECTION);
    BY_RESOURCE_METHOD.put(ResourceMethod.FINDER,               ResponseType.GET_COLLECTION);
    BY_RESOURCE_METHOD.put(ResourceMethod.BATCH_CREATE,         ResponseType.CREATE_COLLECTION);
    BY_RESOURCE_METHOD.put(ResourceMethod.BATCH_GET,            ResponseType.BATCH_ENTITIES);
    BY_RESOURCE_METHOD.put(ResourceMethod.BATCH_UPDATE,         ResponseType.BATCH_ENTITIES);
    BY_RESOURCE_METHOD.put(ResourceMethod.BATCH_PARTIAL_UPDATE, ResponseType.BATCH_ENTITIES);
    BY_RESOURCE_METHOD.put(ResourceMethod.BATCH_DELETE,         ResponseType.BATCH_ENTITIES);
    BY_RESOURCE_METHOD.put(ResourceMethod.UPDATE,               ResponseType.STATUS_ONLY);
    BY_RESOURCE_METHOD.put(ResourceMethod.DELETE,               ResponseType.STATUS_ONLY);
    BY_RESOURCE_METHOD.put(ResourceMethod.OPTIONS,              ResponseType.STATUS_ONLY);
  }

  /**
   * Determine the {@link ResponseType} for a given {@link ResourceMethod}.
   * Throws an {@link IllegalArgumentException} if the resource method's response type is determined at runtime.
   * @param resourceMethod
   * @return response type for the given resource method
   */
  public static ResponseType fromMethodType(ResourceMethod resourceMethod)
  {
    if (isDynamicallyDetermined(resourceMethod))
    {
      throw new IllegalArgumentException("Cannot statically determine response type of resource method \"" + resourceMethod + "\": it is determined at runtime.");
    }

    return BY_RESOURCE_METHOD.get(resourceMethod);
  }

  /**
   * Returns true if the {@link ResponseType} for a given {@link ResourceMethod} is determined at runtime.
   * @param resourceMethod
   * @return true if the resource method's response type is determined at runtime
   */
  public static boolean isDynamicallyDetermined(ResourceMethod resourceMethod)
  {
    return DYNAMICALLY_DETERMINED.contains(resourceMethod);
  }
}
