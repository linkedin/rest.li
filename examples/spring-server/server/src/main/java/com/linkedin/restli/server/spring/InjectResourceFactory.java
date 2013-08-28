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
package com.linkedin.restli.server.spring;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.server.resources.ResourceFactory;

/**
 * This is Spring specific {@link ResourceProvider}, which injects dependencies
 * to the resource classes, which are expressed using JSR-330 annotations.
 * This class delegates calls to the more general InjectResourceProvider, which
 * is declared in {@link com.linkedin.restli.server.resources.InjectResourceFactory}.
 * This class initialization order which occurs in Spring: first, when this class is
 * instantiated by Spring, {@link #setApplicationContext(ApplicationContext)} method is
 * invoked, later RestLi invokes {@link #setRootResources(Map)} method, which concludes
 * initialization process.
 *
 * @author jodzga
 */
public class InjectResourceFactory implements ResourceFactory, ApplicationContextAware
{
  private static final Logger log = LoggerFactory.getLogger(InjectResourceFactory.class);

  private com.linkedin.restli.server.resources.InjectResourceFactory _delegate;

  @Override
  public <R> R create(Class<R> resourceClass)
  {
    return _delegate.create(resourceClass);
  }

  @Override
  public void setApplicationContext(ApplicationContext ctx) throws BeansException
  {
    log.debug(String.format("Setting application context '%s'", ctx.getDisplayName()));
    _delegate = new com.linkedin.restli.server.resources.InjectResourceFactory(new SpringBeanProvider(ctx));
  }

  /**
   * @see com.linkedin.restli.server.spring.LinkedInSpringResourceFactory#setRootResources(java.util.Map)
   */
  @Override
  public void setRootResources(Map<String, ResourceModel> rootResources)
  {
    _delegate.setRootResources(rootResources);
  }

}