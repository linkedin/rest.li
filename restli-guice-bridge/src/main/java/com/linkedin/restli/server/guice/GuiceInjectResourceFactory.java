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
package com.linkedin.restli.server.guice;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.server.resources.ResourceFactory;
import java.util.Map;

/**
 * ResourceFactory for Guice.
 *
 * Uses Guice 3.0, which is a reference implementation of JSR-330, to construct and inject dependencies into
 * rest.li Resources.
 *
 * @author jpbetz
 */
@Singleton
public class GuiceInjectResourceFactory implements ResourceFactory
{
  private final Injector _guiceInjector;

  @Inject
  public GuiceInjectResourceFactory(Injector injector)
  {
    _guiceInjector = injector;
  }

  @Override
  public void setRootResources(Map<String, ResourceModel> rootResources)
  {
    // not needed for Guice injection
  }

  @Override
  public <R> R create(Class<R> resourceClass)
  {
    return _guiceInjector.getInstance(resourceClass);
  }
}
