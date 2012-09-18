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
import java.util.Collection;


/**
 * {@link DataTemplate} for a string array.
 */
public final class StringArray extends DirectArrayTemplate<String>
{
  private static final ArrayDataSchema SCHEMA = (ArrayDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"array\", \"items\" : \"string\" }");

  public StringArray()
  {
    this(new DataList());
  }

  public StringArray(int initialCapacity)
  {
    this(new DataList(initialCapacity));
  }

  public StringArray(Collection<String> c)
  {
    this(new DataList(c.size()));
    addAll(c);
  }

  public StringArray(DataList list)
  {
    super(list, SCHEMA, String.class, String.class);
  }

  @Override
  public StringArray clone() throws CloneNotSupportedException
  {
    return (StringArray) super.clone();
  }
}
