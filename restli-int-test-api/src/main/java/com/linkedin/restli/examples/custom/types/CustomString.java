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

package com.linkedin.restli.examples.custom.types;


import com.linkedin.data.template.Custom;
import com.linkedin.data.template.DirectCoercer;
import com.linkedin.data.template.TemplateOutputCastException;


/**
 * @author nshankar
 */

public class CustomString
{
  private final String _s;

  static
  {
    Custom.registerCoercer(new CustomStringCoercer(), CustomString.class);
  }

  public CustomString(String s)
  {
    _s = s;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (obj instanceof CustomString)
    {
      CustomString other = (CustomString) obj;
      return _s.equals(other._s);
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    return _s.hashCode();
  }

  @Override
  public String toString()
  {
    return _s;
  }

  public static class CustomStringCoercer implements DirectCoercer<CustomString>
  {
    @Override
    public Object coerceInput(CustomString object) throws ClassCastException
    {
      return object.toString();
    }

    @Override
    public CustomString coerceOutput(Object object) throws TemplateOutputCastException
    {
      if (!(object instanceof String))
      {
        throw new TemplateOutputCastException("Output " + object + " is not a String and cannot be coerced to " + CustomString.class
            .getName());
      }
      return new CustomString((String) object);
    }
  }
}
