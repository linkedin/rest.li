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

package com.linkedin.restli.common;

import com.linkedin.data.DataList;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.WrappingArrayTemplate;
import java.util.Collection;


public class LinkArray extends WrappingArrayTemplate<Link>
{
  private static final ArrayDataSchema SCHEMA = (ArrayDataSchema) DataTemplateUtil.parseSchema("{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"Link\",\"namespace\":\"com.linkedin.common.rest\",\"doc\":\"A atom:link-inspired link\",\"fields\":[{\"name\":\"rel\",\"type\":\"string\",\"doc\":\"The link relation e.g. 'self' or 'next'\"},{\"name\":\"href\",\"type\":\"string\",\"doc\":\"The link URI\"},{\"name\":\"type\",\"type\":\"string\",\"doc\":\"The type (media type) of the resource\"}]}}");

  /**
   * Initialize a new basic LinkArray.
   */
  public LinkArray()
  {
    super(new DataList(), SCHEMA, Link.class);
  }

  /**
   * Initialize a new LinkArray with the given initial capacity.
   *
   * @param initialCapacity initial capacity of the LinkArray
   */
  public LinkArray(int initialCapacity)
  {
    this(new DataList(initialCapacity));
  }

  /**
   * Initialize a new LinkArray that contains the given items.
   *
   * @param c Collection of Links
   */
  public LinkArray(Collection<Link> c)
  {
    this(new DataList(c.size()));
    addAll(c);
  }

  /**
   * Initialize a new LinkArray that contains the given data.
   *
   * @param data DataList containing Links
   */
  public LinkArray(DataList data)
  {
    super(data, SCHEMA, Link.class);
  }
}
