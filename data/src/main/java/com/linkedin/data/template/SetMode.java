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

public enum SetMode
{
  /**
   * If the provided value is null, then do nothing.
   * <p>
   *
   * If the provided value is null, then do nothing,
   * i.e. the value of the field is not changed.
   * The field may or may be present.
   */
  IGNORE_NULL,
  /**
   * If the provided value is null, then remove the field.
   * <p>
   *
   * If the provided value is null, then remove the field.
   * This occurs regardless of whether the field is optional.
   */
  REMOVE_IF_NULL,
  /**
   * If the provided value is null and the field is
   * an optional field, then remove the field.
   * <p>
   *
   * If the provided value is null and the field is
   * an optional field, then remove the field.
   * If the provided value is null and the field is
   * a mandatory field, then throw
   * {@link IllegalArgumentException}.
   */
  REMOVE_OPTIONAL_IF_NULL,
  /**
   * The provided value cannot be null.
   * <p>
   *
   * If the provided value is null, then throw {@link NullPointerException}.
   */
  DISALLOW_NULL
}
