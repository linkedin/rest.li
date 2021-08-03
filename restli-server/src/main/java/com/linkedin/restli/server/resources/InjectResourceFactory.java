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

package com.linkedin.restli.server.resources;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linkedin.restli.internal.server.model.ResourceModel;

/**
 * This ResourceFactory implementation scans fields of all Resource classes for JSR330
 * "@Inject" annotations and creates an internal mapping for Resource dependencies. It
 * also verifies that all declared dependencies are actually available from the DI
 * container. At runtime, the factory's {@link #create(Class)} method just delegates to
 * BeanProvider to fetch the required dependencies for the requested Resource.
 *
 * @author dellamag
 * @author jodzga
 */
public class InjectResourceFactory implements ResourceFactory
{
  private static final Logger log = LoggerFactory.getLogger(InjectResourceFactory.class);

  private Map<String, ResourceModel> _rootResources;
  private Jsr330Adapter              _jsr330Adapter;
  private final BeanProvider         _beanProvider;

  public InjectResourceFactory(final BeanProvider beanProvider)
  {
    _beanProvider = beanProvider;
  }

  @Override
  public <R> R create(final Class<R> resourceClass)
  {
    return _jsr330Adapter.getBean(resourceClass);
  }

  @Override
  public void setRootResources(final Map<String, ResourceModel> rootResources)
  {
    log.debug("Setting root resources");

    _rootResources = rootResources;

    Collection<Class<?>> allResourceClasses = new HashSet<>();
    for (ResourceModel resourceModel : _rootResources.values())
    {
      processChildResource(resourceModel, allResourceClasses);
    }

    _jsr330Adapter = new Jsr330Adapter(allResourceClasses, _beanProvider);
  }

  protected void processChildResource(final ResourceModel resourceModel,
                                      final Collection<Class<?>> allResourceClasses)
  {
    if (resourceModel == null)
    {
      return;
    }

    log.debug("Adding resource class for DI: " + resourceModel.getResourceClass());

    allResourceClasses.add(resourceModel.getResourceClass());

    for (ResourceModel child : resourceModel.getSubResources())
    {
      processChildResource(child, allResourceClasses);
    }
  }
}
