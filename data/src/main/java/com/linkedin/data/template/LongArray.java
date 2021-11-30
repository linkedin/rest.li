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

import com.linkedin.data.DataList;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.util.ArgumentUtil;
import java.util.Arrays;
import java.util.Collection;


/**
 * {@link DataTemplate} for a long array.
 */
public final class LongArray extends DirectArrayTemplate<Long>
{
  private static final ArrayDataSchema SCHEMA = (ArrayDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"array\", \"items\" : \"long\" }");

  public LongArray()
  {
    this(new DataList());
  }

  public LongArray(int initialCapacity)
  {
    this(new DataList(initialCapacity));
  }

  public LongArray(Collection<Long> c)
  {
    this(new DataList(c.size()));
    addAll(c);
  }

  public LongArray(DataList list)
  {
    super(list, SCHEMA, Long.class, Long.class);
  }

  public LongArray(Long first, Long... rest)
  {
    this(new DataList(rest.length + 1));
    add(first);
    addAll(Arrays.asList(rest));
  }

  @Override
  public LongArray clone() throws CloneNotSupportedException
  {
    return (LongArray) super.clone();
  }

  @Override
  public LongArray copy() throws CloneNotSupportedException
  {
    return (LongArray) super.copy();
  }

  @Override
  protected Object coerceInput(Long object) throws ClassCastException
  {
    ArgumentUtil.notNull(object, "object");
    return DataTemplateUtil.coerceLongInput(object);
  }

  @Override
  protected Long coerceOutput(Object object) throws TemplateOutputCastException
  {
    assert(object != null);
    return DataTemplateUtil.coerceLongOutput(object);
  }
}
