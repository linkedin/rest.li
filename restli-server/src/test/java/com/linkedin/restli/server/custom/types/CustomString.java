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

public class CustomString
{
  private final String str;

  static
  {
    Custom.registerCoercer(new CustomString.CustomStringCoercer(), CustomString.class);
  }

  public CustomString (final String str)
  {
    this.str = str;
  }

  @Override
  public String toString()
  {
    return str;
  }

  public static class CustomStringCoercer implements DirectCoercer<CustomString>
  {
    @Override
    public Object coerceInput(final CustomString object)
            throws ClassCastException
    {
      return object.toString();
    }

    @Override
    public CustomString coerceOutput(final Object object)
            throws TemplateOutputCastException
    {
      if (!(object instanceof String))
      {
        throw new TemplateOutputCastException("Output " + object
            + " is not a string, and cannot be coerced to "
            + CustomString.class.getName());
      }
      return new CustomString((String) object);
    }
  }

  @Override
  public boolean equals(final Object obj)
  {
    if (obj instanceof CustomString)
    {
      CustomString other = (CustomString) obj;
      return str.equals(other.str);
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    return super.hashCode();
  }
}
