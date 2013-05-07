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
import java.util.Map;


/**
 * {@link DataTemplate} for a map with integer values.
 */
public final class IntegerMap extends DirectMapTemplate<Integer>
{
  private static final MapDataSchema SCHEMA = (MapDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"map\", \"values\" : \"int\" }");

  public IntegerMap()
  {
    this(new DataMap());
  }

  public IntegerMap(int initialCapacity)
  {
    this(new DataMap(initialCapacity));
  }

  public IntegerMap(int initialCapacity, float loadFactor)
  {
    this(new DataMap(initialCapacity, loadFactor));
  }

  public IntegerMap(Map<String, Integer> m)
  {
    this(newDataMapOfSize(m.size()));
    putAll(m);
  }

  public IntegerMap(DataMap map)
  {
    super(map, SCHEMA, Integer.class, Integer.class);
  }

  @Override
  public IntegerMap clone() throws CloneNotSupportedException
  {
    return (IntegerMap) super.clone();
  }

  @Override
  public IntegerMap copy() throws CloneNotSupportedException
  {
    return (IntegerMap) super.copy();
  }
}