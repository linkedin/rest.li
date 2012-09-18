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

package com.linkedin.data.avro;

/**
 * Specifies how an optional field and associated default value should be translated.
 */
public enum OptionalDefaultMode
{

  /**
   * Translate an optional field with a default value to an union with null with translated default value.
   *
   * This mode may cause problems with Avro if the field is not consistently initialized with a default value
   * or consistently not have a default value.
   *
   * This is due to a limitation in Avro that requires a union's default value always be the same as the
   * first member type of the union.
   */
  TRANSLATE_DEFAULT,

  /**
   * Translate an optional field with a default value to an union with null with the default value set null.
   */
  TRANSLATE_TO_NULL

}

