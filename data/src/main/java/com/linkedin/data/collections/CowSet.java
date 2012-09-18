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

package com.linkedin.data.collections;

import java.util.AbstractSet;
import java.util.Iterator;


/**
 * A copy-on-write Set.
 * <p>
 *
 * Multiple {@link CowSet} can have a reference to the same underlying data structure. The
 * underlying data structure is only copied when one of the {@link CowSet} instances is modified.
 * <p>
 *
 * A {@link CowSet} can be set as read-only to make it immutable. This, combined with the lazy
 * copy described above, makes {@link CowSet} ideal for use cases where a Set needs to be
 * iteratively constructed and ultimately frozen in an immutable value object.
 * <p>
 *
 * Views of this {@link CowSet}, i.e. Iterator, provide immutable access to this set, even if the
 * set is not read-only.
 *
 * @author Chris Pettitt
 * @version $Revision$
 */
public class CowSet<K> extends AbstractSet<K> implements CommonSet<K>
{
  private static final Object PRESENT = new Object();

  private final CowMap<K, Object> _map;

  /**
   * Construct an empty set.
   */
  public CowSet()
  {
    _map = new CowMap<K, Object>();
  }

  private CowSet(CowMap<K, Object> map)
  {
    _map = map;
  }

  @Override
  public Object clone() throws CloneNotSupportedException
  {
    return new CowSet<K>(_map.clone());
  }

  @Override
  public void setReadOnly()
  {
    _map.setReadOnly();
  }

  @Override
  public boolean isReadOnly()
  {
    return _map.isReadOnly();
  }

  @Override
  public void invalidate()
  {
    _map.invalidate();
  }

  @Override
  public Iterator<K> iterator()
  {
    return _map.keySet().iterator();
  }

  @Override
  public int size()
  {
    return _map.size();
  }

  @Override
  public boolean contains(Object o)
  {
    return _map.containsKey(o);
  }

  @Override
  public boolean add(K k)
  {
    return _map.put(k, PRESENT) == null;
  }

  @Override
  public boolean remove(Object o)
  {
    return _map.remove(o) == PRESENT;
  }

  @Override
  public void clear()
  {
    _map.clear();
  }
}
