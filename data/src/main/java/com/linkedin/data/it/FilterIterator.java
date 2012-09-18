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
 * A filtering {@link DataIterator} that filters {@link DataElement}'s
 * returned by a source {@link DataIterator} by whether the returned
 * {@link DataElement}'s satisfies the provided {@link Predicate}.
 *
 * @author slim
 */
public class FilterIterator implements DataIterator
{
  /**
   * Constructor.
   *
   * @param it provides the source {@link DataIterator}.
   * @param predicate provides the {@link Predicate} to evaluate on
   *                  each {@link DataElement} returned by the source
   *                  {@link DataIterator}.
   */
  protected FilterIterator(DataIterator it, Predicate predicate)
  {
    _it = it;
    _predicate = predicate;
  }

  @Override
  public DataElement next()
  {
    DataElement element;
    while ((element = _it.next()) != null)
    {
      if (_predicate.evaluate(element))
      {
        return element;
      }
    }
    return null;
  }

  @Override
  public void skipToSibling()
  {
    _it.skipToSibling();
  }

  /**
   * The source {@link DataIterator}.
   */
  private final DataIterator _it;
  /**
   * The predicate to evaluate on each {@link com.linkedin.data.element.DataElement}.
   */
  private final Predicate _predicate;
}
