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

import com.linkedin.data.schema.DataSchema;


/**
 * A {@link DataTemplate} is a proxy for an underlying Data object that
 * has an associated {@link DataSchema} and provides methods for
 * type-checked access to the contents of the Data object.
 *
 * @param <E> is the type of underlying Data object.
 */
public interface DataTemplate<E> extends Cloneable
{
  /**
   * {@link DataSchema} of this object.
   *
   * @return the {@link DataSchema} of this object.
   */
  DataSchema schema();

  /**
   * The underlying Data object proxied by this object.
   *
   * @return the underlying Data object proxied by this object.
   */
  E data();

  /**
   * Return a clone of the {@link DataTemplate}.
   *
   * This method should clone the underlying Data object
   * (unless the underlying Data object is immutable.)
   * The cloned {@link DataTemplate} will proxy the new cloned
   * Data object.
   *
   * Since cloning an underlying Data object performs
   * a shallow copy, this method has
   * the semantics of a shallow copy.
   *
   * @return a shallow copy clone of the DataTemplate.
   * @throws CloneNotSupportedException if the {@link DataTemplate} or
   *                                    its underlying Data object
   *                                    cannot be cloned.
   */
  DataTemplate<E> clone() throws CloneNotSupportedException;

  /*
   * TODO: add deep copy.
   */
}
