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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import com.linkedin.restli.internal.server.model.ResourceModel;

/**
 * Simple {@link ResourceFactory} that creates a new instance of the {@link BaseResource}
 * class per request.
 *
 * @author dellamag
 */
public class PrototypeResourceFactory implements ResourceFactory
{
  /**
   * @see com.linkedin.restli.server.resources.ResourceFactory#create(java.lang.Class)
   */
  @Override
  public <R> R create(final Class<R> resourceClass)
  {
    try
    {
      Constructor<R> defaultConstructor = resourceClass.getDeclaredConstructor();
      defaultConstructor.setAccessible(true);
      return defaultConstructor.newInstance();
    }
    catch (InstantiationException e)
    {
      throw new RuntimeException(e);
    }
    catch (IllegalAccessException e)
    {
      throw new RuntimeException(e);
    }
    catch (SecurityException e)
    {
      throw new RuntimeException(e);
    }
    catch (NoSuchMethodException e)
    {
      throw new RuntimeException(e);
    }
    catch (IllegalArgumentException e)
    {
      throw new RuntimeException(e);
    }
    catch (InvocationTargetException e)
    {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void setRootResources(final Map<String, ResourceModel> rootResources)
  {
    // not needed
  }
}
