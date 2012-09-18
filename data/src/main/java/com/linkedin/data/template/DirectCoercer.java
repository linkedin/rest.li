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
 * Coerce input and output values to the specified type.
 * <p>
 * It will be used only if the class of an input or output value
 * is not the same as the expected type.
 * Input values are values that will be stored in complex Data objects.
 * Output values are values that will be returned by {@link DataTemplate}'s.
 * <p>
 *
 * @see DataTemplateUtil#coerceInput(Object, Class, Class)
 * @see DataTemplateUtil#coerceOutput(Object, Class)
 *
 * @param <T> the desired type.
 */
public interface DirectCoercer<T>
{
  /**
   * Check and coerce input value to the specified type so that
   * it can be stored in a complex Data object.
   *
   * @param object provides the input value.
   * @return the value that can be stored in a complex Data object.
   * @throws ClassCastException if the input value cannot be coerced.
   */
  Object coerceInput(T object) throws ClassCastException;

  /**
   * Check and coerce output value to the specified type so that
   * the returned value is of the type expected to be returned
   * by a {@link DataTemplate} accessor method.
   *
   * @param object provides the output value that is not the specified type.
   * @return the value that is coerced from the output value to the specified type.
   * @throws TemplateOutputCastException if the output value cannot be coerced.
   */
  T coerceOutput(Object object) throws TemplateOutputCastException;
}
