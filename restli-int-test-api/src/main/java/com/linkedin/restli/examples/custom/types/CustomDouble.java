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

public class CustomDouble
{
  private final Double _d;

  static
  {
    Custom.registerCoercer(new CustomDoubleCoercer(), CustomDouble.class);
  }

  public CustomDouble(Double d)
  {
    _d = d;
  }

  public Double toDouble()
  {
    return _d;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (obj instanceof CustomDouble)
    {
      CustomDouble other = (CustomDouble) obj;
      return _d.equals(other._d);
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    return _d.hashCode();
  }

  @Override
  public String toString()
  {
    return "CustomDouble:" + _d.toString();
  }

  public static class CustomDoubleCoercer implements DirectCoercer<CustomDouble>
  {
    @Override
    public Object coerceInput(CustomDouble object) throws ClassCastException
    {
      return object.toDouble();
    }

    @Override
    public CustomDouble coerceOutput(Object object) throws TemplateOutputCastException
    {
      if (!(object instanceof Double) && !(object instanceof Integer))
      {
        throw new TemplateOutputCastException("Output " + object + " is not a Double or integer, and cannot be coerced to " + CustomDouble.class
            .getName());
      }
      return new CustomDouble(((Number) object).doubleValue());
    }
  }
}
