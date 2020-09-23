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

package com.linkedin.data.template;

import com.linkedin.data.DataMap;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.util.ArgumentUtil;
import java.util.Map;


/**
 * {@link DataTemplate} for a map with boolean values.
 */
public final class BooleanMap extends DirectMapTemplate<Boolean>
{
  private static final MapDataSchema SCHEMA = (MapDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"map\", \"values\" : \"boolean\" }");

  public BooleanMap()
  {
    this(new DataMap());
  }

  public BooleanMap(int initialCapacity)
  {
    this(new DataMap(initialCapacity));
  }

  public BooleanMap(int initialCapacity, float loadFactor)
  {
    this(new DataMap(initialCapacity, loadFactor));
  }

  public BooleanMap(Map<String, Boolean> m)
  {
    this(newDataMapOfSize(m.size()));
    putAll(m);
  }

  public BooleanMap(DataMap map)
  {
    super(map, SCHEMA, Boolean.class, Boolean.class);
  }

  @Override
  public BooleanMap clone() throws CloneNotSupportedException
  {
    return (BooleanMap) super.clone();
  }

  @Override
  public BooleanMap copy() throws CloneNotSupportedException
  {
    return (BooleanMap) super.copy();
  }

  @Override
  protected Object coerceInput(Boolean object) throws ClassCastException
  {
    ArgumentUtil.notNull(object, "object");
    return object;
  }

  @Override
  protected Boolean coerceOutput(Object object) throws TemplateOutputCastException
  {
    return DataTemplateUtil.coerceBooleanOutput(object);
  }
}