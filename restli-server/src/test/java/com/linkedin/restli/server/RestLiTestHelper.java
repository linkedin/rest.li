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

package com.linkedin.restli.server;

import java.util.HashMap;
import java.util.Map;

import com.linkedin.restli.internal.server.model.RestLiAnnotationReader;
import com.linkedin.restli.internal.server.model.ResourceModel;

/**
 * TODO HIGH Share the test harness with rest-framework
 *
 * @author dellamag
 */
public class RestLiTestHelper
{
  @SuppressWarnings("unchecked")
  public static <M extends ResourceModel> M buildResourceModel(Class<?> rootResourceClass)
  {
    return (M)RestLiAnnotationReader.processResource(rootResourceClass);
  }

  public static Map<String, ResourceModel> buildResourceModels(Class<?>... rootResourceClasses)
  {
    Map<String, ResourceModel> map = new HashMap<String, ResourceModel>();
    for (Class<?> rootResourceClass : rootResourceClasses)
    {
      ResourceModel model = RestLiAnnotationReader.processResource(rootResourceClass);
      map.put("/" + model.getName(), model);
    }

    return map;
  }
}
