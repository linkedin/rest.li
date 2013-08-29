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
import org.springframework.context.ApplicationContext;
import com.linkedin.restli.server.resources.BeanProvider;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class SpringBeanProvider implements BeanProvider
{
  private final ApplicationContext _context;

  public SpringBeanProvider(ApplicationContext context)
  {
    _context = context;
  }

  @Override
  public Object getBean(String name)
  {
    return _context.getBean(name);
  }

  @Override
  public <T> Map<String, T> getBeansOfType(Class<T> clazz)
  {
    return _context.getBeansOfType(clazz, false, true);
  }
}