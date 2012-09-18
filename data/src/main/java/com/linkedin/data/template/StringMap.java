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
 * {@link DataTemplate} for a map with string values.
 */
public final class StringMap extends DirectMapTemplate<String>
{
  private static final MapDataSchema SCHEMA = (MapDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"map\", \"values\" : \"string\" }");

  public StringMap()
  {
    this(new DataMap());
  }

  public StringMap(int initialCapacity)
  {
    this(new DataMap(initialCapacity));
  }

  public StringMap(int initialCapacity, float loadFactor)
  {
    this(new DataMap(initialCapacity, loadFactor));
  }

  public StringMap(Map<String, String> m)
  {
    this(newDataMapOfSize(m.size()));
    putAll(m);
  }

  public StringMap(DataMap map)
  {
    super(map, SCHEMA, String.class, String.class);
  }

  @Override
  public StringMap clone() throws CloneNotSupportedException
  {
    return (StringMap) super.clone();
  }
}