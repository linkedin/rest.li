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

import com.linkedin.data.collections.CheckedMap;
import com.linkedin.data.collections.CommonMap;
import com.linkedin.data.collections.MapChecker;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;


/**
 * An {@link DataMap} maps strings to Data objects.
 * <p>
 *
 * The key of this map is always a string. When a key is being added or replaced,
 * the map will verify that key is a string. If the key is not a string,
 * the map will throw an {@link IllegalArgumentException}.
 * <p>
 *
 * When an value is being added or replaced on the map, the map
 * will verify that the value is a Data object. If the value
 * is not a Data object, then the map will throw an {@link IllegalArgumentException}.
 * <p>
 *
 * Cloning via the {@link #clone()} method will shallow copy the {@link DataMap}
 * and will not deep copy contained complex objects. Copying via the {@link #copy()}
 * method will deep copy the {@link DataMap}, which includes deep copying the
 * contained complex objects. Keys are not deep copied because the keys are
 * immutable strings.
 * <p>
 *
 * Instrumentation if enabled is only enabled for the {@link DataMap} and not
 * for the underlying {@link CheckedMap}. Furthermore, if a {@link DataMap} is cloned,
 * the clone will not have instrumentation enabled and the clone's instrumented
 * data will be cleared.
 * <p>
 *
 * Since {@link DataMap} extends {@link CheckedMap}, copying of the {@link DataMap} is lazy and may be
 * delayed until the {@link DataMap} is about to be modified.
 *
 * @author slim
 */
public final class DataMap extends CheckedMap<String,Object> implements DataComplex
{
  public static final String RESERVED_CONSTANT_PREFIX = "**";
  public static final String RESERVED_CONSTANT_SUFFIX = "**";
  public static String reservedConstant(String name)
  {
    return RESERVED_CONSTANT_PREFIX + name + RESERVED_CONSTANT_SUFFIX;
  }

  public static final String ERROR_KEY = reservedConstant("ERROR");

  /**
   * Constructs an empty {@link DataMap}.
   */
  public DataMap()
  {
    super(_checker);
  }

  /**
   * Constructs a {@link DataMap} with the entries provided by the input map.
   *
   * @param map provides the entries of the new {@link DataMap}.
   */
  public DataMap(Map<? extends String, ? extends Object> map)
  {
    super(map, _checker);
  }

  /**
   * Constructs a {@link DataMap} with the specified initial capacity and
   * default load factor.
   *
   * @param initialCapacity provides the initial capacity of the {@link DataMap}.
   *
   * @see HashMap
   */
  public DataMap(int initialCapacity)
  {
    super(initialCapacity, _checker);
  }

  /**
   * Constructs a {@link DataMap} with the specified initial capacity and
   * load factor.
   *
   * @param initialCapacity provides the initial capacity of the {@link DataMap}.
   * @param loadFactor provides the load factor of the {@link DataMap}.
   *
   * @see HashMap
   */
  public DataMap(int initialCapacity, float loadFactor)
  {
    super(initialCapacity, loadFactor, _checker);
  }

  @Override
  public DataMap clone() throws CloneNotSupportedException
  {
    DataMap o = (DataMap) super.clone();
    o._madeReadOnly = false;
    o._instrumented = false;
    o._accessMap = null;
    return o;
  }

  @Override
  public Object get(Object key)
  {
    instrumentAccess(key);
    return super.get(key);
  }

  @Override
  public boolean containsKey(Object key)
  {
    instrumentAccess(key);
    return super.containsKey(key);
  }

  @Override
  public DataMap copy() throws CloneNotSupportedException
  {
    return Data.copy(this, new IdentityHashMap<DataComplex, DataComplex>());
  }

  @Override
  public void copyReferencedObjects(IdentityHashMap<DataComplex, DataComplex> alreadyCopied) throws CloneNotSupportedException
  {
    for (Map.Entry<String,?> e : entrySet())
    {
      Object value = e.getValue();
      Object valueCopy = Data.copy(value, alreadyCopied);
      if (value != valueCopy)
      {
        putWithoutChecking(e.getKey(), valueCopy);
      }
    }
  }

  @Override
  public void makeReadOnly()
  {
    for (Map.Entry<String,?> e : entrySet())
    {
      Data.makeReadOnly(e.getValue());
    }
    setReadOnly();
    _madeReadOnly = true;
  }

  @Override
  public boolean isMadeReadOnly()
  {
    return _madeReadOnly;
  }

  /**
   * Returns the value to which the specified key is mapped and cast to {@link Boolean},
   * or {@code null} if this map contains no mapping for the key.
   *
   * @param key provides the key whose associated value is to be returned.
   * @return the value to which the specified key is mapped and cast to {@link Boolean},
   *         or {@code null} if this map contains no mapping for the key.
   */
  public Boolean getBoolean(String key)
  {
    return (Boolean) get(key);
  }

  /**
   * Returns the value to which the specified key is mapped and cast to {@link Integer},
   * or {@code null} if this map contains no mapping for the key.
   *
   * @param key provides the key whose associated value is to be returned.
   * @return the value to which the specified key is mapped and cast to {@link Integer},
   *         or {@code null} if this map contains no mapping for the key.
   */
  public Integer getInteger(String key)
  {
    return (Integer) get(key);
  }

  /**
   * Returns the value to which the specified key is mapped and cast to {@link Long},
   * or {@code null} if this map contains no mapping for the key.
   *
   * @param key provides the key whose associated value is to be returned.
   * @return the value to which the specified key is mapped and cast to {@link Long},
   *         or {@code null} if this map contains no mapping for the key.
   */
  public Long getLong(String key)
  {
    return (Long) get(key);
  }

  /**
   * Returns the value to which the specified key is mapped and cast to {@link Float},
   * or {@code null} if this map contains no mapping for the key.
   *
   * @param key provides the key whose associated value is to be returned.
   * @return the value to which the specified key is mapped and cast to {@link Float},
   *         or {@code null} if this map contains no mapping for the key.
   */
  public Float getFloat(String key)
  {
    return (Float) get(key);
  }

  /**
   * Returns the value to which the specified key is mapped and cast to {@link Double},
   * or {@code null} if this map contains no mapping for the key.
   *
   * @param key provides the key whose associated value is to be returned.
   * @return the value to which the specified key is mapped and cast to {@link Double},
   *         or {@code null} if this map contains no mapping for the key.
   */
  public Double getDouble(String key)
  {
    return (Double) get(key);
  }

  /**
   * Returns the value to which the specified key is mapped and cast to {@link String},
   * or {@code null} if this map contains no mapping for the key.
   *
   * @param key provides the key whose associated value is to be returned.
   * @return the value to which the specified key is mapped and cast to {@link String},
   *         or {@code null} if this map contains no mapping for the key.
   */
  public String getString(String key)
  {
    return (String) get(key);
  }

  /**
   * Returns the value to which the specified key is mapped and cast to {@link ByteString},
   * or {@code null} if this map contains no mapping for the key.
   *
   * @param key provides the key whose associated value is to be returned.
   * @return the value to which the specified key is mapped and cast to {@link ByteString},
   *         or {@code null} if this map contains no mapping for the key.
   */
  public ByteString getByteString(String key)
  {
    return (ByteString) get(key);
  }

  /**
   * Returns the value to which the specified key is mapped and cast to {@link DataMap},
   * or {@code null} if this map contains no mapping for the key.
   *
   * @param key provides the key whose associated value is to be returned.
   * @return the value to which the specified key is mapped and cast to {@link DataMap},
   *         or {@code null} if this map contains no mapping for the key.
   */
  public DataMap getDataMap(String key)
  {
    return (DataMap) get(key);
  }

  /**
   * Returns the value to which the specified key is mapped and cast to {@link DataList},
   * or {@code null} if this map contains no mapping for the key.
   *
   * @param key provides the key whose associated value is to be returned.
   * @return the value to which the specified key is mapped and cast to {@link DataList},
   *         or {@code null} if this map contains no mapping for the key.
   */
  public DataList getDataList(String key)
  {
    return (DataList) get(key);
  }

  /**
   * Returns the value to which {@link #ERROR_KEY} is mapped and cast to {@link String},
   * or {@code null} if this map contains no mapping for the key.
   *
   * @return the value to which the specified key is mapped and cast to {@link String},
   *         or {@code null} if this map contains no mapping for the key.
   */
  public String getError()
  {
    return (String) get(ERROR_KEY);
  }

  /**
   * Adds an error message to the {@link DataMap}.
   *
   * If a value is not mapped to {@link #ERROR_KEY}, bind the specified error message
   * to {@link #ERROR_KEY}. Otherwise, replace the value of {@link #ERROR_KEY} with
   * a new {@link String} constructed by appending the specified error message to
   * the previous value of {@link #ERROR_KEY}.
   *
   * @param msg provides the error message to add.
   * @return the new value of {@link #ERROR_KEY}.
   */
  public String addError(String msg)
  {
    String error = getError();
    String res;
    if (error != null)
    {
      res = error + msg;
    }
    else
    {
      res = msg;
    }
    put(ERROR_KEY, res);
    return res;
  }

  @Override
  public void startInstrumentingAccess()
  {
    Data.startInstrumentingAccess(values());
    _instrumented = true;
    if (_accessMap == null)
    {
      _accessMap = new HashMap<String, Integer>();
    }
  }

  @Override
  public void stopInstrumentingAccess()
  {
    _instrumented = false;
    Data.stopInstrumentingAccess(values());
  }

  @Override
  public void clearInstrumentedData()
  {
    if (_accessMap != null)
    {
      _accessMap.clear();
    }
  }

  @Override
  public void collectInstrumentedData(StringBuilder keyPrefix, Map<String, Map<String, Object>> instrumentedData, boolean collectAllData)
  {
    for (Map.Entry<String, Object> entry : entrySet())
    {
      String key = entry.getKey();
      Integer timesAccessed = _accessMap == null ? null : _accessMap.get(key);
      if (timesAccessed == null)
      {
        timesAccessed = 0;
      }

      int preLength = keyPrefix.length();

      keyPrefix.append('.');
      keyPrefix.append(key);

      Data.collectInstrumentedData(keyPrefix, entry.getValue(), timesAccessed, instrumentedData, collectAllData);

      keyPrefix.setLength(preLength);
    }
  }

  // Unit test use only
  void disableChecker()
  {
    super._checker = null;
  }

  // Unit test use only
  Map<String, Object> getUnderlying()
  {
    return getObject();
  }

  private void instrumentAccess(Object key)
  {
    if (_instrumented)
    {
      Integer i = _accessMap.get(key);
      _accessMap.put(key.toString(), (i == null ? 1 : i + 1));
    }
  }

  private final static MapChecker<String,Object> _checker = new MapChecker<String,Object>()
  {
    @Override
    public void checkKeyValue(CommonMap<String, Object> map, String key, Object value)
    {
      if (key.getClass() != String.class)
      {
        throw new IllegalArgumentException("Key must be a string");
      }
      Data.checkAllowed((DataComplex) map, value);
    }
  };

  private boolean _madeReadOnly = false;
  private boolean _instrumented = false;
  private Map<String, Integer> _accessMap;
}
