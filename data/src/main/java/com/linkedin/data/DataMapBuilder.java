/*
   Copyright (c) 2019 LinkedIn Corp.

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * This class exists for one purpose: to create a DataMap with correct (small) capacity
 * when the number of elements in it is small. With Jackson codec the size of DataMap
 * is not communicated upfront, so we cannot create a proper DataMap upfront. We also
 * don't want to create every DataMap with initial small capacity, since filling out
 * bigger DataMaps will result in repeated resizing/rehashing operations, which is
 * expensive. Thus we use the class below to temporarily hold the contents of a DataMap
 * currently being processed, and only up to the point when the resulting DataMap's
 * capacity is smaller than the default value of 16.
 */
public class DataMapBuilder implements DataComplex {

  private List<Object> _dataMapContents = new ArrayList<>(20);
  private boolean _inUse;

  public void addKVPair(String field, Object value)
  {
    assert _inUse;
    _dataMapContents.add(field);
    _dataMapContents.add(value);
  }

  /**
   * Returns true when the accumulated contents size reaches the point (6 key-value pairs),
   * after which the resulting DataMap, assuming the default load factor of 0.75, will be
   * created with the standard capacity of 16.
   */
  public boolean smallHashMapThresholdReached() { return _dataMapContents.size() >= 12; }

  public DataMap convertToDataMap()
  {
    DataMap dataMap = new DataMap(optimumCapacityFromSize());
    for (int i = 0; i < _dataMapContents.size(); i += 2)
    {
      dataMap.put((String) _dataMapContents.get(i), _dataMapContents.get(i+1));
    }
    _dataMapContents.clear();
    _inUse = false;
    return dataMap;
  }

  public boolean inUse() { return _inUse; }

  public void setInUse(boolean v) { _inUse = v; }

  private int optimumCapacityFromSize() {
    // Pass in size / 2 since we calculate size based on num pairs
    // Should be a clean division since we add to the list in pairs
    return getOptimumHashMapCapacityFromSize(_dataMapContents.size() / 2);
  }

  /**
   * If the proposed hash map size is such that there is a capacity that fits it exactly, for example
   * size 6 and capacity 8, performs an exact int calculation and returns the capacity. Otherwise,
   * uses an approximate formula with the float load factor, which usually returns a higher number.
   * Assumes the default load factor of 0.75.
   */
  public static int getOptimumHashMapCapacityFromSize(int size) {
    return (size % 3 == 0) ? size * 4 / 3 : ((int) (size / 0.75) + 1);
  }

  // The methods below are present only to implement DataComplex interface. They should not be used.
  @Override
  public void makeReadOnly() { throw new UnsupportedOperationException(); }

  @Override
  public boolean isMadeReadOnly() { throw new UnsupportedOperationException(); }

  @Override
  public Collection<Object> values() { throw new UnsupportedOperationException(); }

  @Override
  public DataComplex clone() throws CloneNotSupportedException { throw new UnsupportedOperationException(); }

  @Override
  public void setReadOnly() { throw new UnsupportedOperationException(); }

  @Override
  public boolean isReadOnly() { throw new UnsupportedOperationException(); }

  @Override
  public void invalidate() { throw new UnsupportedOperationException(); }

  @Override
  public DataComplex copy() throws CloneNotSupportedException { throw new UnsupportedOperationException(); }

  @Override
  public int dataComplexHashCode() { throw new UnsupportedOperationException(); }

  @Override
  public void startInstrumentingAccess() { throw new UnsupportedOperationException(); }

  @Override
  public void stopInstrumentingAccess() { throw new UnsupportedOperationException(); }

  @Override
  public void clearInstrumentedData() { throw new UnsupportedOperationException(); }

  @Override
  public void collectInstrumentedData(StringBuilder keyPrefix, Map<String, Map<String, Object>> instrumentedData,
      boolean collectAllData) { throw new UnsupportedOperationException(); }
}
