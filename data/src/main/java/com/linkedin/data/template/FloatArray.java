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
 * {@link DataTemplate} for a float array.
 */
public final class FloatArray extends DirectArrayTemplate<Float>
{
  private static final ArrayDataSchema SCHEMA = (ArrayDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"array\", \"items\" : \"float\" }");

  public FloatArray()
  {
    this(new DataList());
  }

  public FloatArray(int initialCapacity)
  {
    this(new DataList(initialCapacity));
  }

  public FloatArray(Collection<Float> c)
  {
    this(new DataList(c.size()));
    addAll(c);
  }

  public FloatArray(DataList list)
  {
    super(list, SCHEMA, Float.class, Float.class);
  }

  public FloatArray(Float first, Float... rest)
  {
    this(new DataList(rest.length + 1));
    add(first);
    addAll(Arrays.asList(rest));
  }

  @Override
  public FloatArray clone() throws CloneNotSupportedException
  {
    return (FloatArray) super.clone();
  }

  @Override
  public FloatArray copy() throws CloneNotSupportedException
  {
    return (FloatArray) super.copy();
  }

  @Override
  protected Object coerceInput(Float object) throws ClassCastException
  {
    ArgumentUtil.notNull(object, "object");
    return DataTemplateUtil.coerceFloatInput(object);
  }

  @Override
  protected Float coerceOutput(Object object) throws TemplateOutputCastException
  {
    assert(object != null);
    return DataTemplateUtil.coerceFloatOutput(object);
  }
}
