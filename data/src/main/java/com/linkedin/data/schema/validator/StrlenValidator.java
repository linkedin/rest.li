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

package com.linkedin.data.schema.validator;

import com.linkedin.data.DataMap;
import com.linkedin.data.element.DataElement;
import com.linkedin.data.message.Message;


/**
 * Validates length of string representation
 */
public class StrlenValidator extends AbstractValidator
{
  private static final String MIN = "min";
  private static final String MAX = "max";

  private final int _min;
  private final int _max;

  public StrlenValidator(DataMap config)
  {
    super(config);
    Integer min = config.getInteger(MIN);
    Integer max = config.getInteger(MAX);
    _min = (min == null ? 0 : min);
    _max = (max == null ? Integer.MAX_VALUE : max);
  }

  @Override
  public void validate(ValidatorContext ctx)
  {
    DataElement element = ctx.dataElement();
    Object value = element.getValue();
    String str = String.valueOf(value);
    int strlen = str.length();
    if ((strlen < _min) || (strlen > _max))
    {
      ctx.addResult(new Message(element.path(), "length of \"%1$s\" is out of range %2$d...%3$d", str, _min, _max));
    }
  }
}