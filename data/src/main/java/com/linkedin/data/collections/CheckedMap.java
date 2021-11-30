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

import com.linkedin.data.Data;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;


/**
 * Checked Map.
 * <p>
 *
 * It enables a {@link MapChecker} to be added to check
 * keys and values being stored into the {@link CheckedMap}.
 * <p>
 *
 * The underlying map implementation is {@link HashMap}. It delegates
 * map operations to the underlying {@link HashMap} associated
 * with this {@link CheckedMap}.
 * <P>
 *
 * A {@link CheckedMap} may be marked read-only to disable mutations,
 * and to avoid unintentional changes. It may also be invalidated
 * to release its reference the underlying {@link HashMap}.
 * <p>
 *
 * The {@link #entrySet}, {@link #keySet} and {@link #values}
 * methods return unmodifiable set and collection views.
 *
 * @author slim
 */
public class CheckedMap<K,V> implements CommonMap<K,V>, Cloneable
{
  /**
   * Construct an empty map.
   */
  public CheckedMap()
  {
    _checker = null;
    _map = new HashMap<>();
  }

  /**
   * Construct a map with initial entries provided by the specified map.
   *
   * @param map provides the initial entries for the new map.
   */
  public CheckedMap(Map<? extends K,? extends V> map)
  {
    _checker = null;
    checkAll(map);
    _map = new HashMap<>(map);
  }

  /**
   * Construct a map with the specified initial capacity and default load factor.
   *
   * @param initialCapacity provides the initial capacity.
   *
   * @see HashMap
   */
  public CheckedMap(int initialCapacity)
  {
    _checker = null;
    _map = new HashMap<>(initialCapacity);
  }

  /**
   * Construct a map with the specified initial capacity and load factor.
   *
   * @param initialCapacity provides the initial capacity.
   * @param loadFactor provides the load factor.
   *
   * @see HashMap
   */
  public CheckedMap(int initialCapacity, float loadFactor)
  {
    _checker = null;
    _map = new HashMap<>(initialCapacity, loadFactor);
  }

  /**
   * Construct an empty map with the specified {@link MapChecker}.
   *
   * @param checker provides the {@link MapChecker}.
   */
  public CheckedMap(MapChecker<K,V> checker)
  {
    _checker = checker;
    _map = new HashMap<>();
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
  public CheckedMap(Map<? extends K,? extends V> map, MapChecker<K,V> checker)
  {
    _checker = checker;
    checkAll(map);
    _map = new HashMap<>(map);
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
  public CheckedMap(int initialCapacity, MapChecker<K,V> checker)
  {
    _checker = checker;
    _map = new HashMap<>(initialCapacity);
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
  public CheckedMap(int initialCapacity, float loadFactor, MapChecker<K,V> checker)
  {
    _checker = checker;
    _map = new HashMap<>(initialCapacity, loadFactor);
  }

  @Override
  public void clear()
  {
    checkMutability();
    Set<K> keys = null;
    if (_changeListenerHead != null)
    {
      keys = new HashSet<>(keySet());
    }
    _map.clear();
    if (keys != null)
    {
      notifyChangeListenersOnClear(keys);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public CheckedMap<K,V> clone() throws CloneNotSupportedException
  {
    CheckedMap<K,V> o = (CheckedMap<K,V>) super.clone();
    o._map = (HashMap<K,V>) _map.clone();
    o._readOnly = false;
    o._changeListenerHead = null;
    o._changeListenerReferenceQueue = null;
    return o;
  }

  @Override
  public boolean containsKey(Object key)
  {
    return _map.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value)
  {
    return _map.containsValue(value);
  }

  /**
   * Return unmodifiable set view of the entries contained in this map.
   *
   * @return unmodifiable set view of the entries contained in this map.
   */
  @Override
  public Set<java.util.Map.Entry<K, V>> entrySet()
  {
    return Collections.unmodifiableMap(_map).entrySet();
  }

  @Override
  public boolean equals(Object object)
  {
    return _map.equals(object);
  }

  @Override
  public V get(Object key)
  {
    return _map.get(key);
  }

  @Override
  public int hashCode()
  {
    return _map.hashCode();
  }

  @Override
  public boolean isEmpty()
  {
    return _map.isEmpty();
  }

  /**
   * Return unmodifiable set view of the keys contained in this map.
   *
   * @return unmodifiable set view of the keys contained in this map.
   */
  @Override
  public Set<K> keySet()
  {
    return Collections.unmodifiableSet(_map.keySet());
  }

  @Override
  public V put(K key, V value)
  {
    checkKeyValue(key, value);
    checkMutability();
    V oldValue = _map.put(key, value);
    notifyChangeListenersOnPut(key, value);
    return oldValue;
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m)
  {
    checkAll(m);
    checkMutability();
    _map.putAll(m);
    notifyChangeListenersOnPutAll(m);
  }

  @Override
  @SuppressWarnings("unchecked")
  public V remove(Object key)
  {
    checkMutability();
    V oldValue = _map.remove(key);

    if (!(oldValue == null || oldValue == Data.NULL))
    {
      notifyChangeListenersOnPut((K) key, null);
    }

    return oldValue;
  }

  @Override
  public String toString()
  {
    return _map.toString();
  }

  @Override
  public void forEach(BiConsumer<? super K, ? super V> action)
  {
    _map.forEach(action);
  }

  /**
   * Removes all of the entries of this map that satisfy the given predicate.
   *
   * @param filter a predicate which returns {@code true} for elements to be
   *        removed
   * @return {@code true} if any elements were removed
   */
  public boolean removeIf(Predicate<? super Entry<K, V>> filter)
  {
    checkMutability();
    return _map.entrySet().removeIf(filter);
  }

  @Override
  public int size()
  {
    return _map.size();
  }

  /**
   * Return unmodifiable collection view of the values contained in this map.
   *
   * @return unmodifiable collection view of the values contained in this map.
   */
  @Override
  public Collection<V> values()
  {
    return Collections.unmodifiableCollection(_map.values());
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
    _map = null;
  }

  private final void checkMutability()
  {
    if (_readOnly)
    {
      throw new UnsupportedOperationException("Cannot mutate a read-only map");
    }
  }

  /**
   * Add a change listener to be notified of changes to the underlying map.
   *
   * <p>This class internally maintains weak references to the listeners to avoid leaking them.</p>
   *
   * @param listener The listener to register.
   */
  public final void addChangeListener(ChangeListener<K, V> listener)
  {
    //
    // Read only maps cannot be mutated, so they don't need change listeners.
    //
    if (_readOnly)
    {
      return;
    }

    if (_changeListenerReferenceQueue == null)
    {
      _changeListenerReferenceQueue = new ReferenceQueue<>();
    }
    else
    {
      purgeStaleChangeListeners();
    }
    // Maintain a weak reference to to the listener to avoid leaking the wrapper beyond its
    // lifetime.
    _changeListenerHead = new  WeakListNode<>(
        new WeakReference<>(listener, _changeListenerReferenceQueue), _changeListenerHead);
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
    checkMutability();
    V oldValue = _map.put(key, value);
    notifyChangeListenersOnPut(key, value);
    return oldValue;
  }

  V putWithAssertedChecking(K key, V value)
  {
    assert(assertCheckKeyValue(key, value)) : "Check is failed";
    return putWithoutChecking(key, value);
  }

  /**
   * Put that does not invoke checker or change notifications but does check for read-only, use with extreme caution.
   *
   * This method skips all checks.
   *
   * @param key key with which the specified value is to be associated.
   * @param value to be associated with the specified key.
   * @return the previous value associated with key, or null if there was no mapping for key.
   *         A null return can also indicate that the map previously associated null with key.
   * @throws UnsupportedOperationException if the map is read-only.
   */
  V putWithoutCheckingOrChangeNotification(K key, V value)
  {
    checkMutability();
    assert(assertCheckKeyValue(key, value)) : "Check is failed";
    return _map.put(key, value);
  }

  /**
   * Copies the content of another checkedMap without further checking, use with caution.
   *
   * @param src source map that should be copied to this map.
   */
  protected void putAllWithoutChecking(Map<? extends K, ? extends V> src)
  {
    checkMutability();
    _map.putAll(src);
    notifyChangeListenersOnPutAll(src);
  }

  void putAllWithAssertedChecking(Map<? extends K, ? extends V> src)
  {
    assert(assertCheckMap(src)) : "Check is failed";
    putAllWithoutChecking(src);
  }

  /**
   * Unit test use only.
   *
   * @return underlying map.
   */
  protected final Map<K,V> getObject()
  {
    return _map;
  }

  private boolean assertCheckKeyValue(K key, V value)
  {
    try
    {
      checkKeyValue(key, value);
      return true;
    }
    catch (IllegalArgumentException e)
    {
      return false;
    }
  }

  private boolean assertCheckMap(Map<? extends K, ? extends V> map)
  {
    try
    {
      checkAll(map);
      return true;
    }
    catch (IllegalArgumentException e)
    {
      return false;
    }
  }

  private void notifyChangeListenersOnPut(K key, V value)
  {
    purgeStaleChangeListeners();
    if (_changeListenerHead == null)
    {
      return;
    }
    forEachChangeListener(listener -> listener.onUnderlyingMapChanged(key, value));
  }

  private void notifyChangeListenersOnPutAll(Map<? extends K, ? extends V> map)
  {
    purgeStaleChangeListeners();
    if (_changeListenerHead == null)
    {
      return;
    }
    forEachChangeListener(listener -> map.forEach(listener::onUnderlyingMapChanged));
  }

  private void notifyChangeListenersOnClear(Set<? extends K> keys)
  {
    purgeStaleChangeListeners();
    if (_changeListenerHead == null)
    {
      return;
    }

    forEachChangeListener(listener -> keys.forEach(key -> listener.onUnderlyingMapChanged(key, null)));
  }

  /**
   * Change listener interface invoked when the underlying map changes.
   */
  public interface ChangeListener<K, V>
  {
    /**
     * Listener method called whenever an entry in the underlying map is updated or removed.
     *
     * @param key Key being updated.
     * @param value Updated value, can be null when entries are removed.
     */
    void onUnderlyingMapChanged(K key, V value);
  }

  private void purgeStaleChangeListeners()
  {
    if (_changeListenerReferenceQueue != null && _changeListenerReferenceQueue.poll() != null)
    {
      // Clear finalized weak references.
      while (_changeListenerReferenceQueue.poll() != null)
      {
        // Do nothing, as we are just clearing the reference queue.
      }
      // Now iterate over change listeners and purge stale references.
      WeakListNode<ChangeListener<K, V>> node = _changeListenerHead,  prev = null;
      while (node != null)
      {
        if (node._object.get() == null)
        {
          if (prev == null)
          {
            _changeListenerHead = node._next;
          }
          else
          {
            prev._next = node._next;
          }
        }
        else
        {
          prev = node;
        }
        node = node._next;
      }
    }
  }

  private void forEachChangeListener(Consumer<ChangeListener<K, V>> listenerConsumer)
  {
    WeakListNode<ChangeListener<K, V>> node = _changeListenerHead;
    while (node != null)
    {
      WeakReference<ChangeListener<K, V>> listenerRef = node._object;
      ChangeListener<K, V> listener = listenerRef.get();
      if (listener != null)
      {
        listenerConsumer.accept(listener);
      }
      node = node._next;
    }
  }

  private boolean _readOnly = false;
  protected MapChecker<K,V> _checker;
  // Change listeners are mostly used by map wrappers, and we always iterate through them
  // linearly, so use a linked list.
  protected WeakListNode<ChangeListener<K, V>> _changeListenerHead;
  // Reference queue holds any change listener weak references finalized by GC. It being non-empty is a signal
  // to purge change listeners of stale entries.
  private ReferenceQueue<ChangeListener<K, V>> _changeListenerReferenceQueue;
  private HashMap<K,V> _map;

  /**
   * A singly-linked list node that holds weak references to objects.
   */
  static class WeakListNode<T> {
    final WeakReference<T> _object;
    WeakListNode<T> _next;

    WeakListNode(WeakReference<T> object, WeakListNode<T> next)
    {
      this._object = object;
      this._next = next;
    }
  }
}
