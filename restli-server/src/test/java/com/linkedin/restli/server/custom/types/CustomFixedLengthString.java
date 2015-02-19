/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.restli.server.custom.types;


import com.linkedin.data.template.Custom;
import com.linkedin.data.template.DirectCoercer;
import com.linkedin.data.template.TemplateOutputCastException;


/**
 * @author Min Chen
 * @version $Revision: $
 */
public class CustomFixedLengthString
{
  private final String str;

  static
  {
    Custom.registerCoercer(new CustomFixedLengthString.CustomFixedLengthStringCoercer(), CustomFixedLengthString.class);
  }

  public CustomFixedLengthString(final String str)
  {
    this.str = str;
  }

  @Override
  public String toString()
  {
    return str;
  }

  public static class CustomFixedLengthStringCoercer implements DirectCoercer<CustomFixedLengthString>
  {
    @Override
    public Object coerceInput(final CustomFixedLengthString object)
        throws ClassCastException
    {
      return object.toString();
    }

    @Override
    public CustomFixedLengthString coerceOutput(final Object object)
        throws TemplateOutputCastException
    {
      if (!(object instanceof String))
      {
        throw new TemplateOutputCastException("Output " + object
            + " is not a string, and cannot be coerced to "
            + CustomFixedLengthString.class.getName());
      }
      String str = (String) object;
      if (str == null || str.length() != 5)
      {
        throw new TemplateOutputCastException("Output " + object
            + " is not of length 5, and cannot be coerced to "
            + CustomFixedLengthString.class.getName());
      }
      return new CustomFixedLengthString(str);
    }
  }

  @Override
  public boolean equals(final Object obj)
  {
    if (obj instanceof CustomFixedLengthString)
    {
      CustomFixedLengthString other = (CustomFixedLengthString) obj;
      return str.equals(other.str);
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    return 31 + (str == null ? 0 : str.hashCode());
  }
}
