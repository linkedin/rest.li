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
 * {@link DataTemplate} for a map with double values.
 */
public final class DoubleMap extends DirectMapTemplate<Double>
{
  private static final MapDataSchema SCHEMA = (MapDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"map\", \"values\" : \"double\" }");

  public DoubleMap()
  {
    this(new DataMap());
  }

  public DoubleMap(int initialCapacity)
  {
    this(new DataMap(initialCapacity));
  }

  public DoubleMap(int initialCapacity, float loadFactor)
  {
    this(new DataMap(initialCapacity, loadFactor));
  }

  public DoubleMap(Map<String, Double> m)
  {
    this(newDataMapOfSize(m.size()));
    putAll(m);
  }

  public DoubleMap(DataMap map)
  {
    super(map, SCHEMA, Double.class, Double.class);
  }

  @Override
  public DoubleMap clone() throws CloneNotSupportedException
  {
    return (DoubleMap) super.clone();
  }

  @Override
  public DoubleMap copy() throws CloneNotSupportedException
  {
    return (DoubleMap) super.copy();
  }

  @Override
  protected Object coerceInput(Double object) throws ClassCastException
  {
    ArgumentUtil.notNull(object, "object");
    return DataTemplateUtil.coerceDoubleInput(object);
  }

  @Override
  protected Double coerceOutput(Object object) throws TemplateOutputCastException
  {
    return DataTemplateUtil.coerceDoubleOutput(object);
  }
}