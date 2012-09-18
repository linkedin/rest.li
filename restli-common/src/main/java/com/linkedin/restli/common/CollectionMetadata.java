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

import com.linkedin.data.DataMap;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.GetMode;
import com.linkedin.data.template.RecordTemplate;


/**
 * Metadata and pagination links for this collection
 */
public class CollectionMetadata extends RecordTemplate
{
  static final RecordDataSchema SCHEMA = (RecordDataSchema) DataTemplateUtil.parseSchema("{\"type\":\"record\",\"name\":\"CollectionMetadata\",\"namespace\":\"com.linkedin.common.rest\",\"doc\":\"Metadata and pagination links for this collection\",\"fields\":[{\"name\":\"start\",\"type\":\"int\",\"doc\":\"The start index of this collection\"},{\"name\":\"count\",\"type\":\"int\",\"doc\":\"The number of elements in this collection segment\"},{\"name\":\"total\",\"type\":\"int\",\"doc\":\"The total number of elements in the entire collection (not just this segment)\",\"default\":0},{\"name\":\"links\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"Link\",\"doc\":\"A atom:link-inspired link\",\"fields\":[{\"name\":\"rel\",\"type\":\"string\",\"doc\":\"The link relation e.g. 'self' or 'next'\"},{\"name\":\"href\",\"type\":\"string\",\"doc\":\"The link URI\"},{\"name\":\"type\",\"type\":\"string\",\"doc\":\"The type (media type) of the resource\"}]}},\"doc\":\"Previous and next links for this collection\"}]}");
  private static final RecordDataSchema.Field FIELD_Start = SCHEMA.getField("start");
  private static final RecordDataSchema.Field FIELD_Count = SCHEMA.getField("count");
  private static final RecordDataSchema.Field FIELD_Total = SCHEMA.getField("total");
  private static final RecordDataSchema.Field FIELD_Links = SCHEMA.getField("links");

  /**
   * Initialize empty CollectionMetadata.
   */
  public CollectionMetadata()
  {
    super(new DataMap(), SCHEMA);
  }

  /**
   * Initialize CollectionMetadata based on the given map.
   *
   * @param data DataMap representing CollectionMetadata
   */
  public CollectionMetadata(DataMap data)
  {
    super(data, SCHEMA);
  }

  /**
   * @return true if CollectionMetadata has a start value, false otherwise.
   */
  public boolean hasStart()
  {
    return contains(FIELD_Start);
  }

  /**
   * Remove start value from CollectionMetadata.
   */
  public void removeStart()
  {
    remove(FIELD_Start);
  }

  /**
   * Returns the start value of the CollectionMetadata. If the value is not
   * present, behavior will be determined by the given {@link com.linkedin.data.template.GetMode mode}.
   *
   * @param mode determines behavior if the value is not present
   * @return an Integer
   */
  public Integer getStart(GetMode mode)
  {
    return obtainDirect(FIELD_Start, Integer.class, mode);
  }

  /**
   * Get start value of the CollectionMetadata.  If the value is not found, act in
   * accordance with {@link com.linkedin.data.template.GetMode GetMode.STRICT}.
   *
   * @return an int
   */
  public int getStart()
  {
    return getStart(GetMode.STRICT).intValue();
  }

  /**
   * Set the start value of the CollectionMetadata.
   *
   * @param value start value
   */
  public void setStart(int value)
  {
    putDirect(FIELD_Start, Integer.class, value);
  }

  /**
   * @return true if CollectionMetadata has a count value, false otherwise.
   */
  public boolean hasCount()
  {
    return contains(FIELD_Count);
  }

  /**
   * Remove count value from CollectionMetadata.
   */
  public void removeCount()
  {
    remove(FIELD_Count);
  }

  /**
   * Returns the count value of the CollectionMetadata. If the start value is not
   * present, behavior will be determined by the given {@link com.linkedin.data.template.GetMode mode}.
   *
   * @param mode determines behavior if the value is not present
   * @return an Integer
   */
  public Integer getCount(GetMode mode)
  {
    return obtainDirect(FIELD_Count, Integer.class, mode);
  }

  /**
   * Get count value of the CollectionMetadata.  If the value is not found, act in
   * accordance with {@link com.linkedin.data.template.GetMode GetMode.STRICT}.
   *
   * @return an int
   */
  public int getCount()
  {
    return getCount(GetMode.STRICT).intValue();
  }

  /**
   * Set the count value of the CollectionMetadata.
   *
   * @param value count value
   */
  public void setCount(int value)
  {
    putDirect(FIELD_Count, Integer.class, value);
  }

  /**
   * @return true if CollectionMetadata has a total value, false otherwise.
   */
  public boolean hasTotal()
  {
    return contains(FIELD_Total);
  }

  /**
   * Remove count value from CollectionMetadata.
   */
  public void removeTotal()
  {
    remove(FIELD_Total);
  }

  /**
   * Returns the total value of the CollectionMetadata. If the start value is not
   * present, behavior will be determined by the given {@link com.linkedin.data.template.GetMode mode}.
   *
   * @param mode determines behavior if the value is not present
   * @return an Integer
   */
  public Integer getTotal(GetMode mode)
  {
    return obtainDirect(FIELD_Total, Integer.class, mode);
  }

  /**
   * Get the total value of the CollectionMetadata.  If the value is not found, act in
   * accordance with {@link com.linkedin.data.template.GetMode GetMode.STRICT}.
   *
   * @return an int
   */
  public int getTotal()
  {
    return getTotal(GetMode.STRICT);
  }

  /**
   * Set the total value of the CollectionMetadata.
   *
   * @param value total value
   */
  public void setTotal(int value)
  {
    putDirect(FIELD_Total, Integer.class, value);
  }

  /**
   * @return true if CollectionMetadata has a links value, false otherwise.
   */
  public boolean hasLinks()
  {
    return contains(FIELD_Links);
  }

  /**
   * Remove count value from CollectionMetadata.
   */
  public void removeLinks()
  {
    remove(FIELD_Links);
  }

  /**
   * Returns the links of the CollectionMetadata. If the start value is not
   * present, behavior will be determined by the given {@link com.linkedin.data.template.GetMode mode}.
   *
   * @param mode determines behavior if the value is not present
   * @return a LinkArray
   */
  public LinkArray getLinks(GetMode mode)
  {
    return obtainWrapped(FIELD_Links, LinkArray.class, mode);
  }

  /**
   * Get the links of the CollectionMetadata.  If the value is not found, act in
   * accordance with {@link com.linkedin.data.template.GetMode GetMode.STRICT}.
   *
   * @return a LinkArray
   */
  public LinkArray getLinks()
  {
    return getLinks(GetMode.STRICT);
  }

  /**
   * Set the links value of the CollectionMetadata.
   *
   * @param value links value
   */
  public void setLinks(LinkArray value)
  {
    putWrapped(FIELD_Links, LinkArray.class, value);
  }
}
