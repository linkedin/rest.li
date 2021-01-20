/*
   Copyright (c) 2016 LinkedIn Corp.

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

import java.util.HashMap;


/**
 * Custom hash table when {@link DataComplex} objects are used as keys. This utilizes the custom
 * {@link DataComplex#dataComplexHashCode()} as the hash for improved performance.
 */
class DataComplexTable
{
  private final HashMap<DataComplexKey, DataComplex> _map;

  DataComplexTable()
  {
    _map = new HashMap<>();
  }

  public DataComplex get(DataComplex index)
  {
    return _map.get(new DataComplexKey(index));
  }

  public void put(DataComplex src, DataComplex clone)
  {
    _map.put(new DataComplexKey(src), clone);
  }

  private static class DataComplexKey
  {
    private final DataComplex _dataObject;
    private final int _hashCode;

    DataComplexKey(DataComplex dataObject)
    {
      _hashCode = dataObject.dataComplexHashCode();
      _dataObject = dataObject;
    }

    @Override
    public int hashCode()
    {
      return _hashCode;
    }

    @Override
    public boolean equals(Object other)
    {
      // "other" is guaranteed to be DataComplex as this class is "private" scoped within DataComplexTable, which only
      // supports DataComplex objects.
      return _dataObject == ((DataComplexKey) other)._dataObject;
    }
  }

}
