/*
   Copyright (c) 2013 LinkedIn Corp.

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

package com.linkedin.restli.common;


import com.linkedin.data.template.Custom;
import com.linkedin.data.template.DirectCoercer;
import com.linkedin.data.template.TemplateOutputCastException;


/**
 * @author kparikh
 */
public class MyCustomString
{
  private String _string;

  static
  {
    Custom.registerCoercer(new MyCustomStringCoercer(), MyCustomString.class);
  }

  public MyCustomString(String string)
  {
    _string = string;
  }

  @Override
  public String toString()
  {
    return _string;
  }


  public static class MyCustomStringCoercer implements DirectCoercer<MyCustomString>
  {
    @Override
    public Object coerceInput(MyCustomString object)
        throws ClassCastException
    {
      return object.toString();
    }

    @Override
    public MyCustomString coerceOutput(Object object)
        throws TemplateOutputCastException
    {
      return new MyCustomString(object.toString());
    }
  }

  @Override
  public boolean equals(Object obj)
  {
    // really simple implementation of equals
    if (obj == this)
    {
      return true;
    }
    if (obj == null)
    {
      return false;
    }
    if (!(obj instanceof MyCustomString))
    {
      return false;
    }
    MyCustomString that = (MyCustomString)obj;
    return that._string.equals(_string);
  }

  @Override
  public int hashCode()
  {
    return _string.hashCode();
  }
}
