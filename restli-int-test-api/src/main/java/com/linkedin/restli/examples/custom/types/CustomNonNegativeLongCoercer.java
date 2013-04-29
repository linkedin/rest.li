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

import com.linkedin.data.template.DirectCoercer;
import com.linkedin.data.template.TemplateOutputCastException;

public class CustomNonNegativeLongCoercer implements DirectCoercer<CustomNonNegativeLong>
{
  @Override
  public Object coerceInput(CustomNonNegativeLong object)
          throws ClassCastException
  {
    return object.toLong();
  }

  @Override
  public CustomNonNegativeLong coerceOutput(Object object)
          throws TemplateOutputCastException
  {
    if (! (object instanceof Long))
    {
      throw new TemplateOutputCastException("Output " + object + " is not a long, and cannot be coerced to " + CustomNonNegativeLong.class.getName());
    }
    return new CustomNonNegativeLong((Long) object);
  }
}