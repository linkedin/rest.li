/*
   Copyright (c) 2019 LinkedIn Corp.

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

import java.util.Map;

/**
 * Abstract class for translation options when translating values from Pegasus and Avro format and vice versa.
 */
abstract class DataTranslationOptions
{
  private Map<String, String> _avroToDataSchemaNamespaceMapping;

  /**
   * Set Avro-to-Pegasus namespaces mapping for TranslationOptions.
   *
   * The Key is the Avro schema namespace.
   * The Value is the corresponding Pegasus scheme namespace.
   * This Map is required when the namespace for one of these schemas is overridden,
   * which will result in mismatching namespace.
   *
   * @return the {@link DataTranslationOptions}
   */
  public DataTranslationOptions setAvroToDataSchemaNamespaceMapping(Map<String, String> avroToDataSchemaNamespaceMapping)
  {
    _avroToDataSchemaNamespaceMapping = avroToDataSchemaNamespaceMapping;
    return this;
  }

  /**
   * Get Avro-to-Pegasus namespaces mapping from TranslationOptions.
   *
   * @return the {@link Map} of namespaces override mapping.
   */
  public Map<String, String> getAvroToDataSchemaNamespaceMapping()
  {
    return _avroToDataSchemaNamespaceMapping;
  }
}
