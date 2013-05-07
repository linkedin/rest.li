/*
   Copyright (c) 2013 LinkedIn Corp.

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
 * Mode to use when translation from {@link com.linkedin.data.schema.DataSchema} to Avro schema.
 */
public enum AvroToDataSchemaTranslationMode
{
  /**
   * Always perform translation.
   * <p>
   * If embedded schema is present, it is ignored.
   */
  TRANSLATE,
  /**
   * If embedded schema is available, return the embedded schema without verification.
   */
  RETURN_EMBEDDED_SCHEMA,
  /**
   * If embedded schema is available, return the embedded schema after verifying that it translates to the
   * provided Avro schema. If verification fails, then throw {@link IllegalArgumentException}.
   */
  VERIFY_EMBEDDED_SCHEMA
}
