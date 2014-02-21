/*
 Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.data.schema.generator;


import com.linkedin.data.schema.DataSchema;


/**
 * Generates data that conforms to a given schema.
 *
 * @author jbetz@linkedin.com
 */
public interface DataGenerator
{
  /**
   *
   * @param fieldName provides the name of the field for the data being generated.  May be null.
   *             The name may be provided by the caller to indicate the name of the field or named reference
   *             that identifies the contents of the generated data.  Generators are encouraged to use this where
   *             it adds clarity.  E.g. map keys might be generated with the field name included,
   *             e.g. { "<nameHint>Key1": value1, "<nameHint>Key2": value2 }.
   *             Generators MAY choose to ignore the field name.
   * @param dataSchema The data schema to use to generate the data.
   * @return generated data.
   */
  Object buildData(String fieldName, DataSchema dataSchema);
}
