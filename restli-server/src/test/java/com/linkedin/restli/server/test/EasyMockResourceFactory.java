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

package com.linkedin.restli.server.test;

import java.util.HashMap;
import java.util.Map;

import com.linkedin.restli.server.resources.ResourceFactory;
import org.easymock.classextension.EasyMock;

import com.linkedin.restli.internal.server.model.ResourceModel;

/**
 * @author dellamag
 */
public class EasyMockResourceFactory implements ResourceFactory
{
  private final Map<Class<?>, Object> _mockMap =
    new HashMap<Class<?>, Object>();

  /**
   * @see com.linkedin.restli.server.resources.ResourceFactory#setRootResources(java.util.Map)
   */
  @Override
  public void setRootResources(Map<String, ResourceModel> rootResources)
  {
    // ignored
  }

  /**
   * @see com.linkedin.restli.server.resources.ResourceFactory#create(java.lang.Class)
   */
  @Override
  public <R> R create(Class<R> resourceClass)
  {
    @SuppressWarnings("unchecked")
    R resource = (R)_mockMap.get(resourceClass);
    if (resource == null)
    {
      resource = EasyMock.createMock(resourceClass);
      _mockMap.put(resourceClass, resource);
    }

    return resource;
  }

  public <R> R getMock(Class<R> resourceClass)
  {
    return create(resourceClass);
  }
}
