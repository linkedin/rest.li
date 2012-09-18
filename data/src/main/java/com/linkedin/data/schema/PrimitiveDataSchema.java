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

package com.linkedin.data.schema;

import java.util.Collections;
import java.util.Map;


/**
 * {@link DataSchema} for primitive types.
 *
 * @author slim
 */
abstract public class PrimitiveDataSchema extends DataSchema
{
  protected PrimitiveDataSchema(Type type, String name)
  {
    super(type);
    assert(name != null);
    _name = name;
  }

  @Override
  public boolean hasError()
  {
    return false;
  }

  @Override
  public boolean isPrimitive()
  {
    return true;
  }

  @Override
  public Map<String, Object> getProperties()
  {
    return Collections.emptyMap();
  }

  @Override
  public String getUnionMemberKey()
  {
    return _name;
  }

  @Override
  public boolean equals(Object object)
  {
    if (this == object)
    {
      return true;
    }
    if (object != null && object.getClass() == getClass())
    {
      PrimitiveDataSchema other = (PrimitiveDataSchema) object;
      return (getType() == other.getType()) && (_name.equals(other._name));
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    return getType().hashCode() ^ _name.hashCode();
  }

  private final String _name;
}
