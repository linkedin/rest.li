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

/**
 * $Id: $
 */

package com.linkedin.restli.server.custom.types;

import com.linkedin.data.template.Custom;
import com.linkedin.data.template.DirectCoercer;
import com.linkedin.data.template.TemplateOutputCastException;

/**
 * @author Moira Tagle
 * @version $Revision: $
 */

public class CustomLong
{
  private final Long l;

  static
  {
    Custom.registerCoercer(new CustomLong.CustomLongCoercer(), CustomLong.class);
  }

  public CustomLong(final Long l)
  {
    this.l = l;
  }

  public Long toLong()
  {
    return l;
  }

  public static class CustomLongCoercer implements DirectCoercer<CustomLong>
  {
    @Override
    public Object coerceInput(final CustomLong object) throws ClassCastException
    {
      return object.toLong();
    }

    @Override
    public CustomLong coerceOutput(final Object object) throws TemplateOutputCastException
    {
      if (!(object instanceof Long))
      {
        throw new TemplateOutputCastException("Output " + object
            + " is not a long, and cannot be coerced to " + CustomLong.class.getName());
      }
      return new CustomLong((Long) object);
    }
  }

  @Override
  public boolean equals(final Object obj)
  {
    if (obj instanceof CustomLong)
    {
      CustomLong other = (CustomLong) obj;
      return l.equals(other.l);
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    return super.hashCode();
  }

}
