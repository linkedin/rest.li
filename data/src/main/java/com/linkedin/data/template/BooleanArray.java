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
 * {@link DataTemplate} for a boolean array.
 */
public final class BooleanArray extends DirectArrayTemplate<Boolean>
{
  private static final ArrayDataSchema SCHEMA = (ArrayDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"array\", \"items\" : \"boolean\" }");

  public BooleanArray()
  {
    this(new DataList());
  }

  public BooleanArray(int initialCapacity)
  {
    this(new DataList(initialCapacity));
  }

  public BooleanArray(Collection<Boolean> c)
  {
    this(new DataList(c.size()));
    addAll(c);
  }

  public BooleanArray(DataList list)
  {
    super(list, SCHEMA, Boolean.class, Boolean.class);
  }

  public BooleanArray(Boolean first, Boolean... rest)
  {
    this(new DataList(rest.length + 1));
    add(first);
    addAll(Arrays.asList(rest));
  }

  @Override
  public BooleanArray clone() throws CloneNotSupportedException
  {
    return (BooleanArray) super.clone();
  }

  @Override
  public BooleanArray copy() throws CloneNotSupportedException
  {
    return (BooleanArray) super.copy();
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
    assert(object != null);
    return DataTemplateUtil.coerceBooleanOutput(object);
  }
}
