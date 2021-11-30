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

import com.linkedin.data.template.DataTemplateUtil;


/**
 * Specifies whether and how primitive types will be coerced from
 * one value type to a value type that conforms to the Java type
 * expected for the schema type.
 */
public enum CoercionMode
{
  /**
   * No type coercion will be performed.
   */
  OFF,

  /**
   * Coerces numeric values to the schema's numeric type and
   * coerces Avro string encoded binary to {@link com.linkedin.data.ByteString}.
   *
   * This coercion mode performs the following type coercions:
   *
   * <table border="1">
   *   <tr>
   *     <th>Schema Type</th>
   *     <th>Un-coerced Java Type</th>
   *     <th>Coerced Java Type</th>
   *     <th>Coercion method</th>
   *   </tr>
   *   <tr>
   *     <td>int</td>
   *     <td>Number</td>
   *     <td>Integer</td>
   *     <td>{@link DataTemplateUtil#coerceIntOutput(Object)}</td>
   *   </tr>
   *   <tr>
   *     <td>long</td>
   *     <td>Number</td>
   *     <td>Long</td>
   *     <td>{@link DataTemplateUtil#coerceLongOutput(Object)}</td>
   *   </tr>
   *   <tr>
   *     <td>float</td>
   *     <td>Number or String*</td>
   *     <td>Float</td>
   *     <td>{@link DataTemplateUtil#coerceFloatOutput(Object)}</td>
   *   </tr>
   *   <tr>
   *     <td>double</td>
   *     <td>Number or String*</td>
   *     <td>Double</td>
   *     <td>{@link DataTemplateUtil#coerceDoubleOutput(Object)}</td>
   *   </tr>
   *   <tr>
   *     <td>bytes</td>
   *     <td>String (Avro encoded)</td>
   *     <td>{@link com.linkedin.data.ByteString}</td>
   *     <td>{@link com.linkedin.data.ByteString#copyAvroString(String, boolean)}</td>
   *   </tr>
   *   <tr>
   *     <td>fixed</td>
   *     <td>String (Avro encoded)</td>
   *     <td>{@link com.linkedin.data.ByteString}</td>
   *     <td>{@link com.linkedin.data.ByteString#copyAvroString(String, boolean)}</td>
   *   </tr>
   * </table>
   * <i>
   *   *String values can be coerced to Float and Double only for non-numeric values
   *   {@code "NaN"}, {@code "Infinity"}, {@code "-Infinity"}.
   *  </i>
   */
  NORMAL,

  /**
   * Coerces numeric values and strings containing numeric values to the schema's numeric type and
   * coerces Avro string encoded binary to {@link com.linkedin.data.ByteString}.
   *
   * The coercion mode performs the following coercion in addition to coercions performed by {@link CoercionMode#NORMAL}:
   *
   * <table border="1">
   *   <tr>
   *     <th>Schema Type</th>
   *     <th>Un-coerced Java Type</th>
   *     <th>Coerced Java Type</th>
   *     <th>Coercion method</th>
   *   </tr>
   *   <tr>
   *     <td>int</td>
   *     <td>String</td>
   *     <td>Integer</td>
   *     <td>parsing the string with {@link java.math.BigDecimal} and calling {@link Number#intValue()}</td>
   *   </tr>
   *   <tr>
   *     <td>long</td>
   *     <td>Number</td>
   *     <td>Long</td>
   *     <td>parsing the string with {@link java.math.BigDecimal} and calling {@link Number#longValue()}</td>
   *   </tr>
   *   <tr>
   *     <td>float</td>
   *     <td>String</td>
   *     <td>Float</td>
   *     <td>{@link Float#valueOf(String)}</td>
   *   </tr>
   *   <tr>
   *     <td>double</td>
   *     <td>String</td>
   *     <td>Double</td>
   *     <td>{@link Double#valueOf(String)}</td>
   *   </tr>
   *   <tr>
   *     <td>boolean</td>
   *     <td>String</td>
   *     <td>Boolean</td>
   *     <td>strict case-insensitive match against "true" or "false"</td>
   *   </tr>
   * </table>
   */
  STRING_TO_PRIMITIVE
}
