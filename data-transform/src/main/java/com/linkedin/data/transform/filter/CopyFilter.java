/*
 * Copyright (c) 2012 LinkedIn Corp.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.linkedin.data.transform.filter;


import com.linkedin.data.DataComplex;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.collections.CheckedUtil;

import java.util.Map;
import java.util.Set;


/**
 * Filter that creates filtered data object by copying non-negative masked items from original to new.
 * Different algorithm is used according to how sparse the projection is.
 *
 * @author Keren Jin
 */
public class CopyFilter extends AbstractFilter
{
  public CopyFilter()
  {
  }

  /**
   * Create a filter with set of fields that are always included.
   * @param alwaysIncludedFields Fields to include in the filtered data, these fields override the operation specified
   *                             by the filter data.
   */
  public CopyFilter(Set<String> alwaysIncludedFields)
  {
    super(alwaysIncludedFields);
  }

  @Override
  protected Object onFilterDataList(DataList data, int start, int count, Object operation)
  {
    if (operation == FilterConstants.NEGATIVE || start >= data.size() || count <= 0)
    {
      return EMPTY_DATALIST;
    }

    count = Math.min(count, data.size() - start);
    final DataList resultList =  new DataList(count);
    final Class<?> operationClass = operation.getClass();

    for (int i = start; i < start + count; ++i)
    {
      final Object original = data.get(i);
      final Object value;

      if (operationClass == Integer.class)
      {
        value = original;
      }
      else
      {
        assert(operationClass == DataMap.class);
        assert(original instanceof DataComplex);
        value = filter(original, (DataMap) operation);
      }

      CheckedUtil.addWithoutChecking(resultList, value);
    }

    return resultList;
  }

  @Override
  protected Object onFilterDataMap(DataMap data, Map<String, Object> fieldToOperation)
  {
    // This is not using the common initial capacity calculation in Pegasus-Common
    // in order not to depend on an extra jar.
    final DataMap resultMap = new DataMap((int)(fieldToOperation.size() / 0.75f) + 1);

    for (Map.Entry<String, Object> entry : fieldToOperation.entrySet())
    {
      final Object operation = entry.getValue();
      final Object value;

      if (operation == FilterConstants.POSITIVE)
      {
        value = data.get(entry.getKey());
      }
      else
      {
        assert(operation.getClass() == DataMap.class);
        final Object original = data.get(entry.getKey());
        assert(original instanceof DataComplex);
        value = filter(original, (DataMap) operation);
      }

      CheckedUtil.putWithoutChecking(resultMap, entry.getKey(), value);
    }

    return resultMap;
  }

  @Override
  protected Object onError(Object field, String format, Object... args)
  {
    throw new RuntimeException(String.format(format, args));
  }

  @Override
  protected boolean isValidDataMapFieldOperation(Map<String, Object> result, String name, Object operation)
  {
    return operation != FilterConstants.NEGATIVE;
  }

  private static final DataList EMPTY_DATALIST = new DataList();
  static
  {
    EMPTY_DATALIST.makeReadOnly();
  }
}
