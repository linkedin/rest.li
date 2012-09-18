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


/**
 * A functor interface for performing a predicate test on an {@link DataElement}.
 *
 * @author slim
 */
public interface Predicate
{
  /**
   * Evaluate the predicate to test an {@link DataElement}.
   *
   * @param element provides the {@link DataElement} to evaluate.
   * @return true if the {@link DataElement} satisfies the predicate.
   */
  boolean evaluate(DataElement element);
}
