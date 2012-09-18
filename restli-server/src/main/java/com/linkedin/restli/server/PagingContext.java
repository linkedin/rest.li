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
  private final boolean _hasStart;
  private final boolean _hasCount;

  public PagingContext(final int start, final int count)
  {
    this(start, count, true, true);
  }

  public PagingContext(final int start,
                       final int count,
                       final boolean hasStart,
                       final boolean hasCount)
  {
    _start = start;
    _count = count;
    _hasStart = hasStart;
    _hasCount = hasCount;
  }

  public int getStart()
  {
    return _start;
  }

  public int getCount()
  {
    return _count;
  }

  public boolean hasStart()
  {
    return _hasStart;
  }

  public boolean hasCount()
  {
    return _hasCount;
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
                                     .append(_hasCount)
                                     .append(_hasStart)
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
                              .append(_hasCount, other._hasCount)
                              .append(_hasStart, other._hasStart)
                              .isEquals();
  }
}
