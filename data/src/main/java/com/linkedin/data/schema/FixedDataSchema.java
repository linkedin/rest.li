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

package com.linkedin.data.schema;


/**
 * {@link DataSchema} for fixed.
 *
 * @author slim
 */
public final class FixedDataSchema extends NamedDataSchema
{
  public FixedDataSchema(Name name)
  {
    super(Type.FIXED, name);
  }

  /**
   * Set the size of the fixed size data.
   *
   * @param size of the fixed size data.
   * @param errorMessageBuilder to append error message to.
   * @return false if size is negative.
   */
  public boolean setSize(int size, StringBuilder errorMessageBuilder)
  {
    boolean ok = true;
    if (size < 0)
    {
      errorMessageBuilder.append(size).append(" is negative, size must not be negative.\n");
      ok = false;
    }
    else
    {
      _size = size;
    }
    if (ok == false)
    {
      setHasError();
    }
    return ok;
  }

  /**
   * Return the size of the fixed type in bytes.
   *
   * @return the size of the fixed type in bytes.
   */
  public int getSize()
  {
    return _size;
  }

  @Override
  public boolean equals(Object object)
  {
    if (this == object)
    {
      return true;
    }
    if (object != null && object.getClass() == FixedDataSchema.class)
    {
      FixedDataSchema other = (FixedDataSchema) object;
      return super.equals(other) && _size == other._size;
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    return super.hashCode() ^ _size;
  }

  private int _size = 0;
}
