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

package com.linkedin.restli.examples.custom.types;

import com.linkedin.data.template.Custom;
import com.linkedin.data.template.DirectCoercer;
import com.linkedin.data.template.TemplateOutputCastException;

/**
 * @author Moira Tagle
 * @version $Revision: $
 */

public class CustomLong
{
  private Long l;

  static
  {
    Custom.registerCoercer(new CustomLongCoercer(), CustomLong.class);
  }

  public CustomLong(Long l)
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
    public Object coerceInput(CustomLong object)
            throws ClassCastException
    {
      return object.toLong();
    }

    @Override
    public CustomLong coerceOutput(Object object)
            throws TemplateOutputCastException
    {
      if (!(object instanceof Long) && !(object instanceof Integer))
      {
        throw new TemplateOutputCastException("Output " + object + " is not a long or integer, and cannot be coerced to " + CustomLong.class.getName());
      }
      return new CustomLong(((Number)object).longValue());
    }
  }

  @Override
  public boolean equals(Object obj)
  {
    if (obj instanceof CustomLong)
    {
      CustomLong other = (CustomLong)obj;
      return l.equals(other.l);
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    return l.hashCode();
  }

  @Override
  public String toString()
  {
    return "CustomLong:" + l.toString();
  }
}