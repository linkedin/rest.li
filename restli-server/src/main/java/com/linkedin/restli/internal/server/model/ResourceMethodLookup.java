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

package com.linkedin.restli.internal.server.model;

import java.util.HashMap;
import java.util.Map;

import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.server.resources.AssociationResourceTemplate;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;
import com.linkedin.restli.server.resources.ComplexKeyResourceTemplate;

/**
 * A utility class to look up {@link ResourceMethod} by resource class method.
 *
 * @author adubman
 */
public final class ResourceMethodLookup
{
  private ResourceMethodLookup()
  {
  }

  private static final Map<String, ResourceMethod> _methodNameToResourceMethodMap =
                       new HashMap<String, ResourceMethod>(ResourceMethod.values().length);
  static
  {
    _methodNameToResourceMethodMap.put(includePartial("create", false),
                                       ResourceMethod.CREATE);
    _methodNameToResourceMethodMap.put(includePartial("batchCreate", false),
                                       ResourceMethod.BATCH_CREATE);
    _methodNameToResourceMethodMap.put(includePartial("get", false),
                                       ResourceMethod.GET);
    _methodNameToResourceMethodMap.put(includePartial("batchGet", false),
                                       ResourceMethod.BATCH_GET);
    _methodNameToResourceMethodMap.put(includePartial("update", false),
                                       ResourceMethod.UPDATE);
    _methodNameToResourceMethodMap.put(includePartial("batchUpdate", false),
                                       ResourceMethod.BATCH_UPDATE);
    _methodNameToResourceMethodMap.put(includePartial("update", true),
                                       ResourceMethod.PARTIAL_UPDATE);
    _methodNameToResourceMethodMap.put(includePartial("batchUpdate", true),
                                       ResourceMethod.BATCH_PARTIAL_UPDATE);
    _methodNameToResourceMethodMap.put(includePartial("delete", false),
                                       ResourceMethod.DELETE);
    _methodNameToResourceMethodMap.put(includePartial("batchDelete", false),
                                       ResourceMethod.BATCH_DELETE);
    _methodNameToResourceMethodMap.put(includePartial("getAll", false),
                                       ResourceMethod.GET_ALL);
  }

  private static String includePartial(String methodName, final boolean partial)
  {
    if (partial)
    {
      methodName = "partial" + methodName;
    }
    return methodName;
  }

  /**
   * Look up the ResourceMethod value based on the method name and a boolean flag
   * indicating whether this is a partial update (to differentiate between UPDATE and
   * PARTIAL_UPDATE as well as BATCH_UPDATE and BATCH_PARTIAL_UPDATE.
   *
   * @param resourceMethodName a method name defined in {@link CollectionResourceTemplate}
   *          , {@link AssociationResourceTemplate} or {@link ComplexKeyResourceTemplate}
   * @param partial true if this is a partial_update or batch_partial_update, false
   *          otherwise
   * @return {@link ResourceMethod} for provided name and partial flag
   */
  public static ResourceMethod fromResourceMethodName(final String resourceMethodName,
                                                      final boolean partial)
  {
    return _methodNameToResourceMethodMap.get(includePartial(resourceMethodName, partial));
  }
}
