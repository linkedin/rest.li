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

package com.linkedin.data.schema.validation;


/**
 * Enum that provides how required fields should be handled during validation.
 */
public enum RequiredMode
{
  /**
   * Ignore if required fields are not present.
   */
  IGNORE,
  /**
   * If a required field is absent, then validation fails.
   *
   * Validation fails even if the field has a default value.
   */
  MUST_BE_PRESENT,
  /**
   * If a required field is absent and it does not have a default value, then validation fails.
   *
   * Validation will not attempt to modify the field to provide it with the default value.
   */
  CAN_BE_ABSENT_IF_HAS_DEFAULT,
  /**
   * If a required field is absent and it cannot be fixed-up with a default value, then validation fails.
   *
   * This mode will attempt to modify an absent field to provide it with the field's default value.
   * If the field does not have a default value, validation fails.
   * If the field has a default value, validation will attempt to set the field's value to the default value.
   * This attempt may fail if the field cannot be modified.
   * The provided default value will be read-only.
   */
  FIXUP_ABSENT_WITH_DEFAULT
}
