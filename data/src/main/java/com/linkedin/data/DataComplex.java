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

package com.linkedin.data;


import com.linkedin.data.collections.Common;
import java.util.Collection;

/**
 * Marker interface for complex Data objects.
 * <p>
 *
 * All complex objects implement this interface. The only complex types
 * are {@link DataList} and {@link DataMap}.
 *
 * @author slim
 */
public interface DataComplex extends Common, Instrumentable
{
  /**
   * Make read-only this object and recursively contained complex objects.
   *
   * This is irreversible, once a complex object becomes read-only,
   * it cannot be changed back to read-write.
   *
   * Note: Contained primitive types are always immutable.
   */
  void makeReadOnly();

  /**
   * Whether this object and recursively contained complex objects are read-only.
   *
   * Note: Contained primitive types are always immutable.
   *
   * @return whether this object and recursively contained complex objects are read-only.
   */
  boolean isMadeReadOnly();

  /**
   * Return the collection of values in this complex object.
   *
   * This is not recursive. It only returns the values directly contained
   * in this object.
   *
   * @return the collection of values in this complex object.
   */
  Collection<Object> values();

  /**
   * Shallow copy.
   *
   * The cloned object will be read-write enabled.
   *
   * @return a shallow copy.
   * @throws CloneNotSupportedException if the object cannot be shallow copied.
   */
  @Override
  DataComplex clone() throws CloneNotSupportedException;

  /**
   * Deep copy.
   *
   * Clones this object, deep copies complex Data objects referenced by this object, and
   * update internal references to point to the deep copies.
   *
   * @throws CloneNotSupportedException if the object cannot be deep copied.
   */
  DataComplex copy() throws CloneNotSupportedException;

  /**
   * Returns the data complex hash code of this data complex object.
   * @return the data complex hash code.
   */
  int dataComplexHashCode();
}
