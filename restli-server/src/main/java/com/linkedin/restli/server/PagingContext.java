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

package com.linkedin.restli.server;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * @author dellamag
 */
public class PagingContext
{
  private final int     _start;
  private final int     _count;
  private final boolean _isUserProvidedStart;
  private final boolean _isUserProvidedCount;

  /**
   * Constructor to create an instance of {@link PagingContext}.
   *
   * @param start The start value to use
   * @param count The count value to use
   */
  public PagingContext(final int start, final int count)
  {
    this(start, count, true, true);
  }

  /**
   * Constructor to create an instance of {@link PagingContext}.
   *
   * @param start The start value to use
   * @param count The count value to use
   * @param isUserProvidedStart True if the start value is a user provided value, false otherwise
   * @param isUserProvidedCount True if the count value is a user provided value, false otherwise
   */
  public PagingContext(final int start,
                       final int count,
                       final boolean isUserProvidedStart,
                       final boolean isUserProvidedCount)
  {
    _start = start;
    _count = count;
    _isUserProvidedStart = isUserProvidedStart;
    _isUserProvidedCount = isUserProvidedCount;
  }

  /**
   * Method to return the start value.
   *
   * @return Returns the stored start value.
   */
  public int getStart()
  {
    return _start;
  }

  /**
   * Method to return the count value.
   *
   * @return Returns the stored count value.
   */
  public int getCount()
  {
    return _count;
  }

  /**
   * Method to check if the start value is a user provided value or a framework default value.
   *
   * @return Returns true if the start value is a user provided value and false otherwise.
   */
  public boolean hasStart()
  {
    return _isUserProvidedStart;
  }

  /**
   * Method to check if the count value is a user provided value or a framework default value.
   *
   * @return Returns true if the count value is a user provided value and false otherwise.
   */
  public boolean hasCount()
  {
    return _isUserProvidedCount;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("{\"start\"=")
      .append(_start)
      .append(",\"count\"=")
      .append(_count)
      .append("}");
    return sb.toString();
  }

  @Override
  public int hashCode()
  {
    return new HashCodeBuilder(1, 31).append(_count)
                                     .append(_start)
                                     .append(_isUserProvidedCount)
                                     .append(_isUserProvidedStart)
                                     .hashCode();
  }

  @Override
  public boolean equals(final Object obj)
  {
    if (this == obj)
    {
      return true;
    }
    if (obj == null)
    {
      return false;
    }
    if (getClass() != obj.getClass())
    {
      return false;
    }
    PagingContext other = (PagingContext) obj;

    return new EqualsBuilder().append(_count, other._count)
                              .append(_start, other._start)
                              .append(_isUserProvidedCount, other._isUserProvidedCount)
                              .append(_isUserProvidedStart, other._isUserProvidedStart)
                              .isEquals();
  }
}
