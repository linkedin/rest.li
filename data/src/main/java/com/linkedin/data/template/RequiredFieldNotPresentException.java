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

package com.linkedin.data.template;


/**
 * Exception to indicate that a required field is not present.
 * <p>
 *
 * A required field is a field that does not have default value.
 * This exception will be thrown only if the get mode is STRICT,
 * the field is not present, and the field does not have a
 * default value.
 */
@SuppressWarnings("serial")
public class RequiredFieldNotPresentException extends TemplateRuntimeException
{
  public RequiredFieldNotPresentException(String fieldName)
  {
    super("Field \"" + fieldName + "\" is required but it is not present");
  }
}
