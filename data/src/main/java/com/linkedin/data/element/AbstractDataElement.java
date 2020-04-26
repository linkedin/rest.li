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

package com.linkedin.data.element;

import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.PathSpec;
import java.util.Collections;
import java.util.List;

/**
 * Provides methods of {@link DataElement} that can be implemented
 * using get accessors, such as the various path calculation methods.
 */
public abstract class AbstractDataElement implements DataElement
{
  @Override
  public Object getChild(Object childName)
  {
    Object value = getValue();
    Class<?> valueClass = value.getClass();
    if (valueClass == DataList.class && childName.getClass() == Integer.class)
    {
      int index = ((Integer) childName);
      DataList list = (DataList) value;
      if (index >= 0 && index < list.size())
      {
        return list.get(index);
      }
    }
    else if (valueClass == DataMap.class && childName.getClass() == String.class)
    {
      String name = ((String) childName);
      DataMap map = (DataMap) value;
      return map.get(name);
    }
    return null;
  }

  @Override
  public String toString()
  {
    StringBuilder builder = new StringBuilder();
    path(builder, SEPARATOR).append(":").append(getValue());
    return builder.toString();
  }

  @Override
  public boolean equals(Object obj)
  {
    if (obj == null || obj.getClass() != getClass())
    {
      return false;
    }
    DataElement other = (DataElement) obj;
    return getValue() == other.getValue() &&
           getName() == other.getName() &&
           getSchema() == other.getSchema() &&
           getParent() == other.getParent() &&
           level() == other.level();
  }

  @Override
  public Object[] path()
  {
    int index = level();
    Object[] path = new Object[index];
    pathInternal(index, path);
    return path;
  }

  @Override
  public int hashCode()
  {
    int result = 17;
    result = result * 31 + getValue().hashCode();
    result = result * 31 + getName().hashCode();
    result = result * 31 + getSchema().hashCode();
    result = result * 31 + getParent().hashCode();
    result = result * 31 + level();
    return result;
  }

  @Override
  public Object[] path(Object... appends)
  {
    int index = level();
    Object[] path = new Object[index + appends.length];
    System.arraycopy(appends, 0, path, index, appends.length);
    pathInternal(index, path);
    return path;
  }

  private final void pathInternal(int index, Object path[])
  {
    DataElement element = this;
    while (index > 0)
    {
      path[--index] = element.getName();
      element = element.getParent();
    }
  }

  @Override
  public void pathAsList(List<Object> path)
  {
    path.clear();
    DataElement element = this;
    for (int level = level(); level > 0; --level)
    {
      path.add(element.getName());
      element = element.getParent();
    }
    Collections.reverse(path);
  }

  @Override
  public String pathAsString(Character separator)
  {
    return path(new StringBuilder(), separator).toString();
  }

  @Override
  public String pathAsString()
  {
    return pathAsString(SEPARATOR);
  }

  @Override
  public DataElement copyChain()
  {
    int index = level();
    DataElement[] elements = new DataElement[index + 1];
    DataElement element = this;
    while (element != null)
    {
      elements[index] = element;
      index--;
      element = element.getParent();
    }
    DataElement copy = null;
    for (DataElement source : elements)
    {
      copy = new SimpleDataElement(source.getValue(), source.getName(), source.getSchema(), copy);
    }
    return copy;
  }

  private StringBuilder path(StringBuilder builder, Character separator)
  {
    DataElement element = this;
    while (element.getParent() != null)
    {
      builder.insert(0, separator);
      builder.insert(1, element.getName());
      element = element.getParent();
    }
    return builder;
  }

  @Override
  public PathSpec getSchemaPathSpec()
  {
    int index = level();
    DataElement element = this;
    String[] pathSpec = new String[index];
    while (index > 0)
    {
      if (element.getSchema() == null)
      {
        return null;
      }
      index--;
      DataSchema parentDataSchema = element.getParent().getSchema();
      if (parentDataSchema != null && (parentDataSchema.getType() == DataSchema.Type.MAP ||
                                       parentDataSchema.getDereferencedType() == DataSchema.Type.MAP ||
                                       parentDataSchema.getType() == DataSchema.Type.ARRAY ||
                                       parentDataSchema.getDereferencedType() == DataSchema.Type.ARRAY))
      {
        pathSpec[index] = PathSpec.WILDCARD;
      }
      else
      {
        pathSpec[index] = (String) element.getName();
      }
      element = element.getParent();
    }
    return new PathSpec(pathSpec);
  }

}
