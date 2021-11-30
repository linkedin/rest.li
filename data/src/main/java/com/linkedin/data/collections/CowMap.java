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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Copy-on-Write Map.
 * <p>
 *
 * The underlying map implementation is {@link HashMap}. It delegates
 * map operations to the underlying {@link HashMap} associated
 * with this {@link CowMap}.
 * <P>
 *
 * Multiple {@link CowMap} can reference the same {@link HashMap}.
 * When a {@link CowMap} is cloned, the underlying {@link HashMap} is not cloned,
 * instead a reference count is incremented. This reference count
 * tracks the number of {@link CowMap}'s sharing the same underlying
 * {@link HashMap}. A shared underlying P@link HashMap} instance is read-only.
 * <p>
 *
 * If a method mutates the {@link CowMap} and the underlying
 * {@link HashMap} is shared, then the underlying {@link HashMap} will be
 * cloned, the clone {@link HashMap} will be exclusively "owned"
 * by this {@link CowMap}, and mutations will occur on the clone.
 * <p>
 *
 * A {@link CowMap} may be marked read-only to disable mutations,
 * and to avoid unintentional changes. It may also be invalidated
 * to release its reference and decrease the reference count on the
 * underlying {@link HashMap}.
 * <p>
 *
 * The {@link #entrySet}, {@link #keySet} and {@link #values}
 * methods return unmodifiable set and collection views.
 * This avoid the having a separate source
 * of mutations from these instances and their iterators. This is a
 * limitation of the current implementation as it leverages the underlying
 * {@link HashMap}'s implementations of these methods. (Without this
 * restriction, it would be possible to mutate the source of
 * clone and have the changes observable by the clone in the following
 * sequence {@code Set<Map.Entry<K,V>> aEntries = a.entrySet();
 * Map<K,V> b = a.clone(); aEntries.clear();}.)
 * <p>
 *
 * It allows sub-classes to provide a checker that checks the
 * keys and values being stored into the {@link CowMap}.
 * <p>
 *
 * @author slim
 */
public class CowMap<K,V> implements CommonMap<K,V>, Cloneable
{
  /**
   * Construct an empty map.
   */
  public CowMap()
  {
    _checker = null;
    _refCounted = new RefCounted<>(new HashMap<>());
  }

  /**
   * Construct a map with initial entries provided by the specified map.
   *
   * @param map provides the initial entries for the new map.
   */
  public CowMap(Map<? extends K,? extends V> map)
  {
    _checker = null;
    checkAll(map);
    _refCounted = new RefCounted<>(new HashMap<>(map));
  }

  /**
   * Construct a map with the specified initial capacity and default load factor.
   *
   * @param initialCapacity provides the initial capacity.
   *
   * @see HashMap
   */
  public CowMap(int initialCapacity)
  {
    _checker = null;
    _refCounted = new RefCounted<>(new HashMap<>(initialCapacity));
  }

  /**
   * Construct a map with the specified initial capacity and load factor.
   *
   * @param initialCapacity provides the initial capacity.
   * @param loadFactor provides the load factor.
   *
   * @see HashMap
   */
  public CowMap(int initialCapacity, float loadFactor)
  {
    _checker = null;
    _refCounted = new RefCounted<>(new HashMap<>(initialCapacity, loadFactor));
  }

  /**
   * Construct an empty map with the specified {@link MapChecker}.
   *
   * @param checker provides the {@link MapChecker}.
   */
  public CowMap(MapChecker<K,V> checker)
  {
    _checker = checker;
    _refCounted = new RefCounted<>(new HashMap<>());
  }

  /**
   * Construct a map with the initial entries provided by the specified map
   * and specified {@link MapChecker}.
   *
   * @param map provides the initial entries of the new map.
   * @param checker provides the {@link MapChecker}.
   *
   * @see HashMap
   */
  public CowMap(Map<? extends K,? extends V> map, MapChecker<K,V> checker)
  {
    _checker = checker;
    checkAll(map);
    _refCounted = new RefCounted<>(new HashMap<>(map));
  }

  /**
   * Construct a map with the specified initial capacity and {@link MapChecker},
   * and default load factor.
   *
   * @param initialCapacity provides the initial capacity.
   * @param checker provides the {@link MapChecker}.
   *
   * @see HashMap
   */
  public CowMap(int initialCapacity, MapChecker<K,V> checker)
  {
    _checker = checker;
    _refCounted = new RefCounted<>(new HashMap<>(initialCapacity));
  }

  /**
   * Construct a map with the specified initial capacity, load factor and {@link MapChecker}.
   *
   * @param initialCapacity provides the initial capacity.
   * @param loadFactor provides the load factor.
   * @param checker provides the {@link MapChecker}.
   *
   * @see HashMap
   */
  public CowMap(int initialCapacity, float loadFactor, MapChecker<K,V> checker)
  {
    _checker = checker;
    _refCounted = new RefCounted<>(new HashMap<>(initialCapacity, loadFactor));
  }

  @Override
  public void clear()
  {
    getMutable().clear();
  }

  @Override
  public CowMap<K,V> clone() throws CloneNotSupportedException
  {
    @SuppressWarnings("unchecked")
    CowMap<K,V> o = (CowMap<K,V>) super.clone();
    o._refCounted = _refCounted.acquire();
    o._readOnly = false;
    return o;
  }

  @Override
  public boolean containsKey(Object key)
  {
    return getObject().containsKey(key);
  }

  @Override
  public boolean containsValue(Object value)
  {
    return getObject().containsValue(value);
  }

  /**
   * Return unmodifiable set view of the entries contained in this map.
   *
   * @return unmodifiable set view of the entries contained in this map.
   */
  @Override
  public Set<java.util.Map.Entry<K, V>> entrySet()
  {
     return Collections.unmodifiableMap(getObject()).entrySet();
  }

  public boolean equals(Object object)
  {
    return getObject().equals(object);
  }

  @Override
  public V get(Object key)
  {
    return getObject().get(key);
  }

  public int hashCode()
  {
    return getObject().hashCode();
  }

  @Override
  public boolean isEmpty()
  {
    return getObject().isEmpty();
  }

  /**
   * Return unmodifiable set view of the keys contained in this map.
   *
   * @return unmodifiable set view of the keys contained in this map.
   */
  @Override
  public Set<K> keySet()
  {
    return Collections.unmodifiableSet(getObject().keySet());
  }

  @Override
  public V put(K key, V value)
  {
    checkKeyValue(key, value);
    return getMutable().put(key, value);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m)
  {
    checkAll(m);
    getMutable().putAll(m);
  }

  @Override
  public V remove(Object key)
  {
    return getMutable().remove(key);
  }

  @Override
  public String toString()
  {
    return getObject().toString();
  }

  @Override
  public int size()
  {
    return getObject().size();
  }

  /**
   * Return unmodifiable collection view of the values contained in this map.
   *
   * @return unmodifiable collection view of the values contained in this map.
   */
  @Override
  public Collection<V> values()
  {
    return Collections.unmodifiableCollection(getObject().values());
  }

  @Override
  public boolean isReadOnly()
  {
    return _readOnly;
  }

  @Override
  public void setReadOnly()
  {
    _readOnly = true;
  }

  @Override
  public void invalidate()
  {
    try
    {
      if (_refCounted != null)
      {
        _refCounted.release();
      }
    }
    finally
    {
      _refCounted = null;
    }
  }

  final private void checkKeyValue(K key, V value)
  {
    if (_checker != null)
    {
      _checker.checkKeyValue(this, key, value);
    }
  }

  final private void checkAll(Map<? extends K, ? extends V> m)
  {
    if (_checker != null)
    {
      for (Map.Entry<? extends K, ? extends V> e : m.entrySet())
      {
        _checker.checkKeyValue(this, e.getKey(), e.getValue());
      }
    }
  }

  /**
   * Put that does not invoke checker but does check for read-only, use with caution.
   *
   * This method skips all checks.
   *
   * @param key key with which the specified value is to be associated.
   * @param value to be associated with the specified key.
   * @return the previous value associated with key, or null if there was no mapping for key.
   *         A null return can also indicate that the map previously associated null with key.
   * @throws UnsupportedOperationException if the map is read-only.
   */
  protected V putWithoutChecking(K key, V value)
  {
    return getMutable().put(key, value);
  }

  /* Avoid use of finalize, causes GC problems
  protected void finalize() throws Throwable
  {
    try
    {
      invalidate();
    }
    finally
    {
      super.finalize();
    }
  }
  */

  /**
   * For debugging use only, package scope.
   *
   * @return underlying {@link RefCounted}.
   */
  RefCounted<HashMap<K, V>> getRefCounted()
  {
    return _refCounted;
  }

  private final Map<K,V> getMutable()
  {
    if (_readOnly)
    {
      throw new UnsupportedOperationException("Cannot mutate a read-only map");
    }
    _refCounted = _refCounted.getMutable();
    return _refCounted.getObject();
  }

  protected final Map<K,V> getObject()
  {
    return _refCounted.getObject();
  }

  protected MapChecker<K,V> _checker;

  private boolean _readOnly = false;
  private RefCounted<HashMap<K,V>> _refCounted;
}
