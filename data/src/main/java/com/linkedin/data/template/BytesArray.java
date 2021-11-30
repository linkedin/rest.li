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

import com.linkedin.data.ByteString;
import com.linkedin.data.DataList;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.util.ArgumentUtil;
import java.util.Arrays;
import java.util.Collection;


/**
 * {@link DataTemplate} for a boolean array.
 */
public final class BytesArray extends DirectArrayTemplate<ByteString>
{
  private static final ArrayDataSchema SCHEMA = (ArrayDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"array\", \"items\" : \"bytes\" }");

  public BytesArray()
  {
    this(new DataList());
  }

  public BytesArray(int initialCapacity)
  {
    this(new DataList(initialCapacity));
  }

  public BytesArray(Collection<ByteString> c)
  {
    this(new DataList(c.size()));
    addAll(c);
  }

  public BytesArray(DataList list)
  {
    super(list, SCHEMA, ByteString.class, ByteString.class);
  }

  public BytesArray(ByteString first, ByteString... rest)
  {
    this(new DataList(rest.length + 1));
    add(first);
    addAll(Arrays.asList(rest));
  }

  @Override
  public BytesArray clone() throws CloneNotSupportedException
  {
    return (BytesArray) super.clone();
  }

  @Override
  public BytesArray copy() throws CloneNotSupportedException
  {
    return (BytesArray) super.copy();
  }

  @Override
  protected Object coerceInput(ByteString object) throws ClassCastException
  {
    ArgumentUtil.notNull(object, "object");
    return object;
  }

  @Override
  protected ByteString coerceOutput(Object object) throws TemplateOutputCastException
  {
    assert(object != null);
    return DataTemplateUtil.coerceBytesOutput(object);
  }
}
