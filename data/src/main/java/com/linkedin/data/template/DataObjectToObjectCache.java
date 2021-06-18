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

package com.linkedin.data.template;


import com.linkedin.data.DataComplex;
import java.util.HashMap;


/**
* {@link DataObjectToObjectCache} is a cache implementation for storing mappings between
 * data objects and other objects.
 *
 * This class is used for caching {@link DataTemplate} typed field values in {@link DataTemplate} objects.
 * This class basically replaces the functionality of IdentityHashMap. IdentityHashMap is retired because of
 * mixed performance results in multi-threaded scenarios. System.identityHashCode method which is being called
 * by the IdentityHashMap is using a suboptimal algorithm to assign hash code values to objects initially.
 * The algorithm causes multiple threads to synchronize at hash code assignment. This cache implementation
 * scales much better than IdentityHashMap and also has better performance in single threaded scenarios.
 * see {@link com.linkedin.data.DataComplexHashCode} to read about the details of the hash code generation that
 * this class depends on.
 */
class DataObjectToObjectCache<V> implements Cloneable
{
  private HashMap<DataObjectKey, V> _cache;

  DataObjectToObjectCache()
  {
    _cache = new HashMap<>();
  }

  DataObjectToObjectCache(int initialCapacity)
  {
    _cache = new HashMap<>(initialCapacity);
  }

  @SuppressWarnings("unchecked")
  public DataObjectToObjectCache<V> clone() throws CloneNotSupportedException
  {
    DataObjectToObjectCache<V> cloned = (DataObjectToObjectCache<V>) super.clone();
    cloned._cache = (HashMap<DataObjectKey, V>) _cache.clone();
    return cloned;
  }

  void put(Object dataObject, V value)
  {
    _cache.put(new DataObjectKey(dataObject), value);
  }

  V get(Object dataObject)
  {
    return _cache.get(new DataObjectKey(dataObject));
  }

  private class DataObjectKey
  {
    private Object _dataObject;
    private int _hashCode;

    DataObjectKey(Object dataObject)
    {
      if(dataObject instanceof DataComplex)
      {
        _hashCode = ((DataComplex) dataObject).dataComplexHashCode();
      }
      else
      {
        _hashCode = dataObject.hashCode();
      }

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
      //We know that the parameter values to this method will always be DataObjectKey objects as
      //this class is private scoped within DataObjectToObjectCache class.
      return _dataObject == ((DataObjectToObjectCache.DataObjectKey)other)._dataObject;
    }
  }
}
