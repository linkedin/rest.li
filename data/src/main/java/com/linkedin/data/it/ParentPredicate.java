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
 * Evaluate the supplied {@link Predicate} on the parent Data object.
 *
 * <p>
 * If the Data object does not have a parent, then evaluate to false,
 * else evaluate the supplied predicate on the parent Data object.
 */
public class ParentPredicate implements Predicate
{
  public ParentPredicate(Predicate predicate)
  {
    _predicate = predicate;
  }

  @Override
  public boolean evaluate(DataElement element)
  {
    DataElement parentElement = element.getParent();
    if (parentElement == null)
      return false;
    else
      return _predicate.evaluate(parentElement);
  }

  final Predicate _predicate;
}
