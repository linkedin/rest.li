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

package com.linkedin.restli.internal.client;


import com.linkedin.data.DataComplex;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.template.DataTemplate;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.transform.filter.request.MaskCreator;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.URIParamUtils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * @author kparikh
 */
public class QueryParamsUtil
{
  public static DataMap convertToDataMap(Map<String, Object> queryParams)
  {
    return convertToDataMap(queryParams, AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion());
  }

  /**
   * Converts a String -> Object based representation of query params into a {@link DataMap}
   * @param queryParams
   * @param version
   * @return
   */
  public static DataMap convertToDataMap(Map<String, Object> queryParams, ProtocolVersion version)
  {
    DataMap result = new DataMap(queryParams.size());
    for (Map.Entry<String, Object> entry: queryParams.entrySet())
    {
      String key = entry.getKey();
      Object value = entry.getValue();

      if (RestConstants.PROJECTION_PARAMETERS.contains(key))
      {
        @SuppressWarnings("unchecked")
        List<PathSpec> pathSpecs = (List<PathSpec>)value;
        result.put(key, MaskCreator.createPositiveMask(pathSpecs).getDataMap());
      }
      else
      {
        result.put(key, paramToDataObject(value, version));
      }
    }
    result.makeReadOnly();
    return result;
  }

  private static Object paramToDataObject(Object param, ProtocolVersion version)
  {
    if (param == null)
    {
      return null;
    }
    else if (param instanceof ComplexResourceKey)
    {
      return ((ComplexResourceKey) param).toDataMap();
    }
    else if (version.compareTo(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion()) >= 0
             && param instanceof CompoundKey)
    {
      return URIParamUtils.compoundKeyToDataMap((CompoundKey) param);
    }
    else if (param instanceof DataTemplate)
    {
      @SuppressWarnings("rawtypes")
      final DataTemplate dataTemplate = (DataTemplate)param;
      return dataTemplate.data();
    }
    else if (param instanceof DataComplex)
    {
      return param;
    }
    else if (param instanceof List)
    {
      return coerceList((List) param, version);
    }
    else
    {
      return DataTemplateUtil.stringify(param);
    }
  }

  /**
   * given a list of objects returns the objects either in a DataList, or, if
   * they are PathSpecs (projections), encode them and return a String.
   */
  private static Object coerceList(List<?> values, ProtocolVersion version)
  {
    assert values != null;
    DataList dataList = new DataList();
    for (Object value : values)
    {
      if (value != null)
      {
        dataList.add(paramToDataObject(value, version));
      }
    }
    return dataList;
  }

  /**
   * given an array of primitives returns a collection of strings
   *
   * @param array the array to stringify
   */
  private static List<String> stringifyArray(Object array)
  {
    assert array != null && array.getClass().isArray();
    int len = Array.getLength(array);
    List<String> strings = new ArrayList<String>(len);
    for (int i = 0; i < len; ++i)
    {
      Object value = Array.get(array, i);
      if (value != null)
      {
        strings.add(value.toString());
      }
    }
    return strings;
  }
}
