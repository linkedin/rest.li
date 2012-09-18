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

public enum GetMode
{
  /**
   * If the field is not present, the method returns null.
   * <p>
   *
   * If the field is present, then return value of the field.
   * If the field is not present, then return null
   * (even if there is a default value.)
   */
  NULL,
  /**
   * If the field is not present, the method returns the default value
   * if the field has a default value, else return null.
   * <p>
   *
   * If the field is present, then return value of the field.
   * If the field is not present and there is a default value,
   * then return the the default value.
   * If the field is not present and there is no default value,
   * then return null.
   */
  DEFAULT,
  /**
   * If the field is not present and the field has a default value,
   * the method returns the default value,
   * else if the field is not present and the field is not optional,
   * throw {@link RequiredFieldNotPresentException},
   * else if the field is not present and the field is optional,
   * then return null.
   * <p>
   *
   * If the field is present, then return value of the field.
   * If the field is not present and there is a default value,
   * then return the the default value.
   * If the field is not present and the field is not optional,
   * then throw {@link RequiredFieldNotPresentException}.
   * If the field is not present and the field is optional,
   * then return null.
   */
  STRICT
}
