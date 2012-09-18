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

import com.linkedin.data.schema.DataSchema;

/**
 * Simple implementation of {@link DataElement}.
 *
 * Each attribute is backed by a single instance variable.
 */
public class SimpleDataElement extends AbstractDataElement
{
  public SimpleDataElement(Object value, DataSchema schema)
  {
    this(value, DataElement.ROOT_NAME, schema, null);
  }

  public SimpleDataElement(Object value, Object name, DataSchema schema, DataElement parentElement)
  {
    _value = value;
    _name = name;
    _schema = schema;
    _parentElement = parentElement;
    _level = (parentElement == null ? 0 : parentElement.level() + 1);
  }

  @Override
  public Object getValue()
  {
    return _value;
  }

  @Override
  public Object getName()
  {
    return _name;
  }

  @Override
  public DataSchema getSchema()
  {
    return _schema;
  }

  @Override
  public DataElement getParent()
  {
    return _parentElement;
  }

  @Override
  public int level()
  {
    return _level;
  }

  protected final Object _value;
  protected final Object _name;
  protected final DataSchema _schema;
  protected final DataElement _parentElement;
  protected final int _level;
}

