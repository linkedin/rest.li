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
 * {@link DataTemplate} for a double array.
 */
public final class DoubleArray extends DirectArrayTemplate<Double>
{
  private static final ArrayDataSchema SCHEMA = (ArrayDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"array\", \"items\" : \"double\" }");

  public DoubleArray()
  {
    this(new DataList());
  }

  public DoubleArray(int initialCapacity)
  {
    this(new DataList(initialCapacity));
  }

  public DoubleArray(Collection<Double> c)
  {
    this(new DataList(c.size()));
    addAll(c);
  }

  public DoubleArray(DataList list)
  {
    super(list, SCHEMA, Double.class, Double.class);
  }

  public DoubleArray(Double first, Double... rest)
  {
    this(new DataList(rest.length + 1));
    add(first);
    addAll(Arrays.asList(rest));
  }

  @Override
  public DoubleArray clone() throws CloneNotSupportedException
  {
    return (DoubleArray) super.clone();
  }

  @Override
  public DoubleArray copy() throws CloneNotSupportedException
  {
    return (DoubleArray) super.copy();
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
    assert(object != null);
    return DataTemplateUtil.coerceDoubleOutput(object);
  }
}
