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

package com.linkedin.restli.examples.custom.types;

import com.linkedin.data.template.Custom;

public class CustomNonNegativeLong
{
  private Long l;

  static
  {
    Custom.registerCoercer(new CustomNonNegativeLongCoercer(), CustomNonNegativeLong.class);
  }

  public CustomNonNegativeLong(Long l)
  {
    if(l < 0) throw new IllegalArgumentException("Value must be non-negative: " + l);
    this.l = l;
  }

  public Long toLong()
  {
    return l;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (obj instanceof CustomNonNegativeLong)
    {
      CustomNonNegativeLong other = (CustomNonNegativeLong)obj;
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