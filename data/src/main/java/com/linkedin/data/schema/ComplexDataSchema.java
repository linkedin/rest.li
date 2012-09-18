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

public abstract class ComplexDataSchema extends DataSchema
{
  protected ComplexDataSchema(Type type)
  {
    super(type);
  }

  @Override
  public boolean hasError()
  {
    return _hasError;
  }

  @Override
  public boolean isPrimitive()
  {
    return false;
  }

  protected void setHasError()
  {
    _hasError = true;
  }

  public void setProperties(Map<String, Object> properties)
  {
    _properties = Collections.unmodifiableMap(properties);
  }

  @Override
  public Map<String, Object> getProperties()
  {
    return _properties;
  }

  @Override
  public boolean equals(Object object)
  {
    if (object != null && object instanceof ComplexDataSchema)
    {
      ComplexDataSchema other = (ComplexDataSchema) object;
      return getType() == other.getType() && _hasError == other._hasError && _properties.equals(other._properties);
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    return getType().hashCode() ^ _properties.hashCode();
  }

  private boolean _hasError;
  private Map<String, Object> _properties = _emptyProperties;

  private static final Map<String, Object> _emptyProperties = Collections.emptyMap();
}
