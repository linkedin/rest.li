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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


/**
 * Evaluate if all the {@link Predicate}'s are true.
 * <p>
 *
 * If there are no {@link Predicate}'s to evaluate, then
 * this predicate returns true. Furthermore, the
 * predicates are evaluated in the order they are provided,
 * once a predicate evaluates to false, the remaining
 * predicates are not evaluated, i.e. the semantics
 * is similar to {@code &&}.
 */
public class AndPredicate implements Predicate
{
  /**
   * Constructor.
   *
   * @param predicates to evaluate, it should not be modified
   *                   once passed to the constructor.
   */
  public AndPredicate(Predicate... predicates)
  {
    _predicates = predicates;
  }

  /**
   * Constructor.
   *
   * @param predicates to evaluate, it should not be modified
   *                   once passed to the constructor.
   */
  public AndPredicate(Collection<? extends Predicate> predicates)
  {
    _predicates = predicates.toArray(new Predicate[0]);
  }

  @Override
  public boolean evaluate(DataElement element)
  {
    for (Predicate predicate : _predicates)
    {
      if (predicate.evaluate(element) == false)
      {
        return false;
      }
    }
    return true;
  }

  public List<Predicate> getChildPredicates()
  {
    return Collections.unmodifiableList(Arrays.asList(_predicates));
  }

  Predicate[] _predicates;
}
