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


/**
 * Filter that creates filtered data object by copying non-negative masked items from original to new.
 * Different algorithm is used according to how sparse the projection is.
 *
 * @author Keren Jin
 */
public class CopyFilter extends AbstractFilter
{
  @Override
  protected Object onFilterDataList(DataList data, int start, int count, Object operation)
  {
    if (operation == FilterConstants.NEGATIVE)
    {
      return EMPTY_DATALIST;
    }

    final int end = Math.min(data.size(), start + count);
    final int size = Math.max(end - start, 0);
    final DataList resultList =  new DataList(size);

    for (int i = start; i < end; ++i)
    {
      final Class<?> operationClass = operation.getClass();
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
    final DataMap resultMap = new DataMap((int)(fieldToOperation.size() / 0.75f + 1));

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
