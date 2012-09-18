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
import java.util.regex.Pattern;


/**
 * Validates the {@link #toString()} representation against a regular regression.
 */
public class RegexValidator extends AbstractValidator
{
  public static final String REGEX = "regex";

  private final String _regex;
  private final Pattern _pattern;

  public RegexValidator(DataMap config)
  {
    super(config);
    _regex = config.getString(REGEX);
    if (_regex == null)
      throw new IllegalArgumentException("\"" + REGEX + "\" is required");
    _pattern = Pattern.compile(_regex);
  }

  @Override
  public void validate(ValidatorContext ctx)
  {
    DataElement element = ctx.dataElement();
    Object value = element.getValue();
    String str = String.valueOf(value);
    boolean matches = _pattern.matcher(str).matches();
    if (! matches)
    {
      ctx.addResult(new Message(element.path(), "\"%1$s\" does not match %2$s", str, _regex));
    }
  }
}
