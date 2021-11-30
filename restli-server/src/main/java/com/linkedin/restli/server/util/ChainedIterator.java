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

package com.linkedin.restli.server.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Iterates over a list of iterators.
 *
 * @author dellamag
 */
public class ChainedIterator<T> implements Iterator<T>
{
  private final Iterator<Iterator<? extends T>> _iterators;
  private Iterator<? extends T> _currItr;

  @SafeVarargs
  public ChainedIterator(Iterator<? extends T>... iterators)
  {
    final List<Iterator<? extends T>> list = new ArrayList<>();
    for (Iterator<? extends T> itr : iterators)
    {
      list.add(itr);
    }
    _iterators = list.iterator();
  }

  @Override
  public boolean hasNext()
  {
    while (_currItr == null || !_currItr.hasNext())
    {
      if (_iterators.hasNext())
      {
        _currItr = _iterators.next();
      }
      else
      {
        return false;
      }
    }

    return _currItr.hasNext();
  }

  @Override
  public T next()
  {
    return _currItr.next();
  }

  @Override
  public void remove()
  {
    throw new UnsupportedOperationException();
  }
}
