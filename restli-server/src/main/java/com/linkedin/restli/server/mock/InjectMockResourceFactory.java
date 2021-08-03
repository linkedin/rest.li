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

package com.linkedin.restli.server.mock;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.server.resources.BeanProvider;
import com.linkedin.restli.server.resources.Jsr330Adapter;
import com.linkedin.restli.server.resources.ResourceFactory;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class InjectMockResourceFactory implements ResourceFactory
{
  private Jsr330Adapter      _containerAdaptor;
  private final BeanProvider _beanProvider;

  public InjectMockResourceFactory(final BeanProvider beans)
  {
    _beanProvider = beans;
  }

  public InjectMockResourceFactory(final BeanProvider beans,
                                   final Class<?>... resourceClasses)
  {
    _beanProvider = beans;
    _containerAdaptor = new Jsr330Adapter(Arrays.asList(resourceClasses), beans);
  }

  @Override
  public <R> R create(final Class<R> resourceClass)
  {
    return _containerAdaptor.getBean(resourceClass);
  }

  @Override
  public void setRootResources(final Map<String, ResourceModel> rootResources)
  {
    Collection<Class<?>> allResourceClasses = new HashSet<>();
    for (ResourceModel resourceModel : rootResources.values())
    {
      processChildResource(resourceModel, allResourceClasses);
    }

    _containerAdaptor = new Jsr330Adapter(allResourceClasses, _beanProvider);
  }

  public void processChildResource(final ResourceModel resourceModel,
                                   final Collection<Class<?>> allResourceClasses)
  {
    if (resourceModel == null)
    {
      return;
    }

    allResourceClasses.add(resourceModel.getResourceClass());

    for (ResourceModel child : resourceModel.getSubResources())
    {
      processChildResource(child, allResourceClasses);
    }
  }

}
