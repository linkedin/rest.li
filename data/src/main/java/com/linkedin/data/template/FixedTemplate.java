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
import com.linkedin.data.schema.FixedDataSchema;


/**
 * Abstract {@link DataTemplate} for fixed types.
 */
public abstract class FixedTemplate implements DataTemplate<ByteString>
{
  /**
   * Constructor.
   *
   * @param object can either be a String or ByteString.
   * @param schema of the fixed type.
   * @throws TemplateOutputCastException if the provided object is not the right type
   *                                     (i.e. {@link String} or {@link ByteString}, or
   *                                     the length of the input object
   *                                     is not the same as specified by the
   *                                     schema of the fixed type.
   */
  protected FixedTemplate(Object object, FixedDataSchema schema) throws TemplateOutputCastException
  {
    Class<?> objectClass = object.getClass();
    if (objectClass == String.class)
    {
      String data = (String) object;
      if (data.length() != schema.getSize())
      {
       throw new TemplateOutputCastException("Fixed size is " + schema.getSize() + ", string length is " + data.length());
      }
      _schema = schema;
      _data = ByteString.copyAvroString(data, true);
      if (_data == null)
      {
        throw new TemplateOutputCastException("String is not a valid representation of bytes");
      }
    }
    else if (objectClass == ByteString.class)
    {
      ByteString bytes = (ByteString) object;
      if (bytes.length() != schema.getSize())
      {
        throw new TemplateOutputCastException("Fixed size is " + schema.getSize() + ", ByteString length is " + bytes.length());
      }
      _schema = schema;
      _data = bytes;
    }
    else
    {
      throw new TemplateOutputCastException("Fixed input " + object + " is not a string or ByteString");
    }
  }

  /**
   * Return the value as {@link ByteString}.
   *
   * @return the value as {@link ByteString}.
   */
  public ByteString bytes()
  {
    return _data;
  }

  @Override
  public FixedDataSchema schema()
  {
    return _schema;
  }

  /**
   * Return the underlying Data object.
   *
   * Same as {@link #bytes()}.
   *
   * @return the underlying Data object.
   */
  @Override
  public ByteString data()
  {
    return _data;
  }

  /**
   * Clone the {@link FixedTemplate}.
   *
   * @return a clone of the {@link FixedTemplate}.
   * @throws CloneNotSupportedException if the {@link FixedTemplate} cannot be cloned.
   */
  @Override
  public FixedTemplate clone() throws CloneNotSupportedException
  {
    FixedTemplate clone = (FixedTemplate) super.clone();
    return clone;
  }

  @Override
  public FixedTemplate copy() throws CloneNotSupportedException
  {
    return clone();
  }

  @Override
  public int hashCode()
  {
    return _data.hashCode();
  }

  /**
   * If both objects are {@link FixedTemplate}'s and the {@link ByteString}'s of these objects are
   * equal, then this method return true.
   *
   * @param object to compare.
   * @return true if both objects are {@link FixedTemplate}'s and their {@link ByteString}'s are equal.
   */
  @Override
  public boolean equals(Object object)
  {
    if (this == object)
    {
      return true;
    }
    if (object != null && object instanceof FixedTemplate)
    {
      return _data.equals(((FixedTemplate) object)._data);
    }
    return false;
  }

  @Override
  public String toString()
  {
    return _data.toString();
  }

  protected final FixedDataSchema _schema;
  protected final ByteString _data;
}
