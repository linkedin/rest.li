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

package com.linkedin.restli.tools.twitter;


import com.linkedin.data.template.InvalidAlternativeKeyException;
import com.linkedin.data.template.KeyCoercer;


/**
 * @author Moira Tagle
 */
public class StringLongCoercer implements KeyCoercer<String, Long>
{
  @Override
  public Long coerceToKey(String object) throws InvalidAlternativeKeyException
  {
    return Long.parseLong(object.substring(3));
  }

  @Override
  public String coerceFromKey(Long object)
  {
    return "Alt" + object;
  }
}
