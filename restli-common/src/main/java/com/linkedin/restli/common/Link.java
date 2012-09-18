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
 * A atom:link-inspired link
 */
public class Link extends RecordTemplate
{
  private static final RecordDataSchema SCHEMA = ((RecordDataSchema) DataTemplateUtil.parseSchema("{\"type\":\"record\",\"name\":\"Link\",\"namespace\":\"com.linkedin.common.rest\",\"doc\":\"A atom:link-inspired link\",\"fields\":[{\"name\":\"rel\",\"type\":\"string\",\"doc\":\"The link relation e.g. 'self' or 'next'\"},{\"name\":\"href\",\"type\":\"string\",\"doc\":\"The link URI\"},{\"name\":\"type\",\"type\":\"string\",\"doc\":\"The type (media type) of the resource\"},{\"name\":\"title\",\"type\":\"string\",\"doc\":\"The title of the link\"}]}"));
  private static final RecordDataSchema.Field FIELD_Rel = SCHEMA.getField("rel");
  private static final RecordDataSchema.Field FIELD_Href = SCHEMA.getField("href");
  private static final RecordDataSchema.Field FIELD_Type = SCHEMA.getField("type");
  private static final RecordDataSchema.Field FIELD_Title = SCHEMA.getField("title");

  /**
   * Initialize a new link with no data.
   */
  public Link()
  {
    super(new DataMap(), SCHEMA);
  }

  /**
   * Initialize a new link with the given underlying data.
   *
   * @param data a DataMap representing the data in a Link
   */
  public Link(DataMap data)
  {
    super(data, SCHEMA);
  }

  /**
   * @return true if the Link has a rel attribute, and false otherwise
   */
  public boolean hasRel()
  {
    return contains(FIELD_Rel);
  }

  /**
   * Remove the rel attribute from this Link.
   */
  public void removeRel()
  {
    remove(FIELD_Rel);
  }

  /**
   * Returns the rel attribute of the Link. If the attribute is not
   * present, behavior will be determined by the given {@link com.linkedin.data.template.GetMode mode}.
   *
   * @param mode determines behavior if the attribute is not present
   * @return a String
   */
  public String getRel(GetMode mode)
  {
    return obtainDirect(FIELD_Rel, String.class, mode);
  }

  /**
   * Get rel attribute of the Link.  If the attribute is not found, act in
   * accordance with {@link com.linkedin.data.template.GetMode GetMode.STRICT}.
   *
   * @return a String
   */
  public String getRel()
  {
    return getRel(GetMode.STRICT);
  }

  /**
   * Set the rel attribute.
   *
   * @param value rel attribute value
   */
  public void setRel(String value)
  {
    putDirect(FIELD_Rel, String.class, value);
  }

  /**
   * @return true if the Link has a href attribute, and false otherwise
   */
  public boolean hasHref()
  {
    return contains(FIELD_Href);
  }

  /**
   * Remove the href attribute from this Link.
   */
  public void removeHref()
  {
    remove(FIELD_Href);
  }

  /**
   * Returns the href attribute of the Link. If the attribute is not
   * present, behavior will be determined by the given {@link com.linkedin.data.template.GetMode mode}.
   *
   * @param mode determines behavior if the attribute is not present
   * @return a String
   */
  public String getHref(GetMode mode)
  {
    return obtainDirect(FIELD_Href, String.class, mode);
  }

  /**
   * Get href attribute of the Link.  If the attribute is not found, act in
   * accordance with {@link com.linkedin.data.template.GetMode GetMode.STRICT}.
   *
   * @return a String
   */
  public String getHref()
  {
    return getHref(GetMode.STRICT);
  }

  /**
   * Set the rel attribute.
   *
   * @param value rel attribute value
   */
  public void setHref(String value)
  {
    putDirect(FIELD_Href, String.class, value);
  }

  /**
   * @return true if the Link has a type attribute, and false otherwise
   */
  public boolean hasType()
  {
    return contains(FIELD_Type);
  }

  /**
   * Remove the type attribute from this Link.
   */
  public void removeType()
  {
    remove(FIELD_Type);
  }

  /**
   * Returns the type attribute of the Link. If the attribute is not
   * present, behavior will be determined by the given {@link com.linkedin.data.template.GetMode mode}.
   *
   * @param mode determines behavior if the attribute is not present
   * @return a String
   */
  public String getType(GetMode mode)
  {
    return obtainDirect(FIELD_Type, String.class, mode);
  }

  /**
   * Get type attribute of the Link.  If the attribute is not found, act in
   * accordance with {@link com.linkedin.data.template.GetMode GetMode.STRICT}.
   *
   * @return a String
   */
  public String getType()
  {
    return getType(GetMode.STRICT);
  }

  /**
   * Set the type attribute.
   *
   * @param value rel attribute value
   */
  public void setType(String value)
  {
    putDirect(FIELD_Type, String.class, value);
  }

  /**
   * @return true if the Link has a title attribute, and false otherwise
   */
  public boolean hasTitle()
  {
    return contains(FIELD_Title);
  }

  /**
   * Remove the title attribute from this Link.
   */
  public void removeTitle()
  {
    remove(FIELD_Title);
  }

  /**
   * Returns title href attribute of the Link. If the attribute is not
   * present, behavior will be determined by the given {@link com.linkedin.data.template.GetMode mode}.
   *
   * @param mode determines behavior if the attribute is not present
   * @return a String
   */
  public String getTitle(GetMode mode)
  {
    return obtainDirect(FIELD_Title, String.class, mode);
  }

  /**
   * Get title attribute of the Link.  If the attribute is not found, act in
   * accordance with {@link com.linkedin.data.template.GetMode GetMode.STRICT}.
   *
   * @return a String
   */
  public String getTitle()
  {
    return getType(GetMode.STRICT);
  }

  /**
   * Set the title attribute.
   *
   * @param value rel attribute value
   */
  public void setTitle(String value)
  {
    putDirect(FIELD_Title, String.class, value);
  }
}
