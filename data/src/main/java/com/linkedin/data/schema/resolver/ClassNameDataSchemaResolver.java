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

package com.linkedin.data.schema.resolver;

import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaLocation;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.template.DataTemplateUtil;

/**
 * Resolve Java class name into data schema, assuming the class is loaded using the {@link ClassLoader}.
 *
 * @author Keren Jin
 */
public class ClassNameDataSchemaResolver extends DefaultDataSchemaResolver
{
  /**
   * Construct a new instance that uses the {@link Thread#getContextClassLoader()} for the current thread.
   */
  public ClassNameDataSchemaResolver()
  {
    _classLoader = Thread.currentThread().getContextClassLoader();
  }

  /**
   * Construct a new instance that uses the specified {@link ClassLoader}.
   *
   * @param classLoader provides the {@link ClassLoader}.
   */
  public ClassNameDataSchemaResolver(ClassLoader classLoader)
  {
    _classLoader = classLoader;
  }

  @Override
  protected NamedDataSchema locateDataSchema(String className, StringBuilder errorMessageBuilder)
  {
    final DataSchemaLocation location = new ClassNameDataSchemaLocation(className);
    if (isBadLocation(location))
    {
      return null;
    }

    final Class<?> clazz;
    try
    {
      clazz = _classLoader.loadClass(className);
    }
    catch (ClassNotFoundException e)
    {
      addBadLocation(location);
      errorMessageBuilder.append(String.format("Unable to locate DataSchema: class \"%s\" not found", className));
      return null;
    }

    final DataSchema schema = DataTemplateUtil.getSchema(clazz);
    if (schema instanceof NamedDataSchema)
    {
      return (NamedDataSchema) schema;
    }

    addBadLocation(location);
    errorMessageBuilder.append(String.format("Unable to locate DataSchema: class \"%s\" is not a NamedDataSchema", className));
    return null;
  }

  private final ClassLoader _classLoader;
}
