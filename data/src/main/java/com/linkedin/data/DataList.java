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

import com.linkedin.data.collections.CheckedList;
import com.linkedin.data.collections.CommonList;
import com.linkedin.data.collections.ListChecker;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * A DataList is ordered list of Data objects.
 * <p>
 *
 * When an value is being added or replaced on the list, the list
 * will verify that the value is a Data object. If the value
 * is not a Data object, then the list will throw an {@link IllegalArgumentException}.
 * <p>
 *
 * Cloning via the {@link #clone()} method will shallow copy the {@link DataList}
 * and will not deep copy contained complex objects. Copying via the {@link #copy()}
 * method will deep  copy the {@link DataList}, which includes deep copying the
 * contained complex objects.
 * <p>
 *
 * Instrumentation if enabled is only enabled for the {@link DataList} and not
 * for the underlying {@link CheckedList}. Furthermore, if a {@link DataList} is cloned,
 * the clone will not have instrumentation enabled and the clone's instrumented
 * data will be cleared.
 * <p>
 *
 * Since {@link DataList} extends {@link CheckedList}, copying of the {@link DataList}
 * is lazy and may be delayed until the {@link DataList} is about to be modified.
 * <p>
 *
 * @author slim
 */
public final class DataList extends CheckedList<Object> implements DataComplex
{
  /**
   * Construct an empty {@link DataList}.
   */
  public DataList()
  {
    super(_checker);
  }

  /**
   * Construct a {@link DataList} with Data objects provided by the input list.
   *
   * @param list provides the initial Data objects in the constructed list.
   */
  public DataList(List<? extends Object> list)
  {
    super(list, _checker);
  }

  /**
   * Construct a {@link DataList} with the specified initial capacity.
   *
   * @param initialCapacity provides the initial capacity of the {@link DataList}.
   */
  public DataList(int initialCapacity)
  {
    super(initialCapacity, _checker);
  }

  @Override
  public Object get(int index)
  {
    instrumentAccess(index);
    return super.get(index);
  }

  @Override
  public DataList clone() throws CloneNotSupportedException
  {
    DataList o = (DataList) super.clone();
    o._madeReadOnly = false;
    o._instrumented = false;
    o._accessList = null;
    o._dataComplexHashCode = 0;
    o._isTraversing = new ThreadLocal<>();

    return o;
  }

  @Override
  public DataList copy() throws CloneNotSupportedException
  {
    return Data.copy(this, new DataComplexTable());
  }

  /**
   * Deep copy this object and the complex Data objects referenced by this object.
   *
   * @param alreadyCopied provides the objects already copied, and their copies.
   * @throws CloneNotSupportedException if the referenced object cannot be copied.
   */
  public void copyReferencedObjects(DataComplexTable alreadyCopied) throws CloneNotSupportedException
  {
    int count = size();
    for (int i = 0; i < count; ++i)
    {
      Object value = get(i);
      Object valueCopy = Data.copy(value, alreadyCopied);
      if (value != valueCopy)
      {
        setWithoutChecking(i, valueCopy);
      }
    }
  }

  @Override
  public Collection<Object> values()
  {
    return this;
  }

  @Override
  public void makeReadOnly()
  {
    for (Object o : this)
    {
      Data.makeReadOnly(o);
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
   * Returns the element at the specified position cast to a {@link DataList}.
   *
   * @param index of the element to return.
   * @return the element at the specified position cast to a {@link DataList}.
   */
  public DataList getDataList(int index)
  {
    return (DataList) get(index);
  }

  /**
   * Returns the element at the specified position cast to a {@link DataMap}.
   *
   * @param index of the element to return.
   * @return the element at the specified position cast to a {@link DataMap}.
   */
  public DataMap getDataMap(int index)
  {
    return (DataMap) get(index);
  }

  @Override
  public void startInstrumentingAccess()
  {
    Data.startInstrumentingAccess(values());
    _instrumented = true;
    if (_accessList == null)
    {
      _accessList = new ArrayList<Integer>(size());
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
    if (_accessList != null)
    {
      _accessList.clear();
    }
  }

  @Override
  public void collectInstrumentedData(StringBuilder keyPrefix, Map<String, Map<String, Object>> instrumentedData, boolean collectAllData)
  {
    for (int i = 0; i < size(); ++i)
    {
      int preLength = keyPrefix.length();

      keyPrefix.append('[');
      keyPrefix.append(i);
      keyPrefix.append(']');

      Object object = super.get(i);
      Integer timesAccessed = _accessList == null ? 0 : (i < _accessList.size() ? _accessList.get(i) : 0);

      Data.collectInstrumentedData(keyPrefix, object, timesAccessed, instrumentedData, collectAllData);

      keyPrefix.setLength(preLength);
    }
  }

  @Override
  public int dataComplexHashCode()
  {
    if (_dataComplexHashCode != 0)
    {
      return _dataComplexHashCode;
    }

    synchronized (this)
    {
      if (_dataComplexHashCode == 0)
      {
        _dataComplexHashCode = DataComplexHashCode.nextHashCode();
      }
    }

    return _dataComplexHashCode;
  }

  // Unit test use only
  void disableChecker()
  {
    super._checker = null;
  }

  // Unit test use only
  List<Object> getUnderlying()
  {
    return getObject();
  }

  private void instrumentAccess(int index)
  {
    if (_instrumented)
    {
      if (index >= _accessList.size())
      {
        _accessList.ensureCapacity(size());
        for (int i = _accessList.size(); i < index; ++i)
        {
          _accessList.add(0);
        }
        _accessList.add(1);
      }
      else
      {
        _accessList.set(index, _accessList.get(index) + 1);
      }
    }
  }

  private final static ListChecker<Object> _checker = (list, e) -> Data.checkAllowed((DataComplex) list, e);

  /**
   * Indicates if this {@link DataList} is currently being traversed by a {@link Data.TraverseCallback} if this value is
   * not null, or not if this value is null. This is internally marked package private, used for cycle detection and
   * not meant for use by external callers. This is maintained as a {@link ThreadLocal} to allow for concurrent
   * traversals of the same {@link DataList} from multiple threads.
   */
  ThreadLocal<Object> _isTraversing = new ThreadLocal<>();

  private boolean _madeReadOnly = false;
  private boolean _instrumented = false;
  private ArrayList<Integer> _accessList;
  private int _dataComplexHashCode = 0;
}
