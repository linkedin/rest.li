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

package com.linkedin.data.it;

import com.linkedin.data.element.DataElement;
import java.util.Iterator;

/**
 * A {@link DataIterator} provides {@link #next} method for iterate through
 * a acyclic graph of Data objects.
 * <p>
 *
 * A {@link DataIterator} is different from a {@link Iterator} in that
 * it's {@link #next} method returns {@code null} if there isn't a next
 * {@link DataElement}, and it does not have a {@link Iterator#hasNext} method.
 * <p>
 *
 * Unlike {@link Iterator}, it has a {@link #skipToSibling} to instruct
 * the DataIterator to skip to the next sibling.
 * <p>
 *
 * @author slim
 */
public interface DataIterator
{
  /**
   * Return a {@link DataElement} that represents the next Data object
   * in the graph.
   *
   * Unlike {@link Iterator#next} this method returns null if there isn't
   * a next Data object, and it does not throw a
   * {@link java.util.NoSuchElementException}. The {@link DataElement}
   * returned is valid until {@link #next()} is called again. In others
   * words, the next call to {@link #next()} may invalidate or change
   * the returned {@link DataElement} or its ancestors.
   *
   * @return a {@link DataElement} that represents the next Data object or
   *         return null if there isn't a next {@link DataElement}.
   */
  DataElement next();

  /**
   * Instruct the DataIterator to skip to the next sibling, i.e.
   * next invocation of {@link #next} should return a sibling of
   * the last Data object visited and returned by the last
   * invocation of {@link #next}.
   */
  void skipToSibling();
}
