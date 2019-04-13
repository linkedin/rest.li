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
import com.linkedin.restli.client.ProjectionDataMapSerializer;
import com.linkedin.restli.client.RestLiProjectionDataMapSerializer;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.URIParamUtils;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * @author kparikh
 */
public class QueryParamsUtil
{
  public static DataMap convertToDataMap(Map<String, Object> queryParams)
  {
    return convertToDataMap(queryParams, Collections.<String, Class<?>>emptyMap(),
        AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
        RestLiProjectionDataMapSerializer.DEFAULT_SERIALIZER);
  }

  /**
   * Converts a String -> Object based representation of query params into a {@link DataMap}
   */
  public static DataMap convertToDataMap(Map<String, Object> queryParams, Map<String, Class<?>> queryParamClasses,
      ProtocolVersion version)
  {
    return convertToDataMap(queryParams, queryParamClasses, version, RestLiProjectionDataMapSerializer.DEFAULT_SERIALIZER);
  }

  public static DataMap convertToDataMap(Map<String, Object> queryParams, Map<String, Class<?>> queryParamClasses,
      ProtocolVersion version, ProjectionDataMapSerializer projectionDataMapSerializer)
  {
    DataMap result = new DataMap(queryParams.size());
    for (Map.Entry<String, Object> entry: queryParams.entrySet())
    {
      String key = entry.getKey();
      Object value = entry.getValue();

      if (RestConstants.PROJECTION_PARAMETERS.contains(key))
      {
        @SuppressWarnings("unchecked")
        Set<PathSpec> pathSpecs = (Set<PathSpec>)value;
        result.put(key, projectionDataMapSerializer.toDataMap(key, pathSpecs));
      }
      else
      {
        Object objValue = paramToDataObject(value, queryParamClasses.get(key), version);
        // If the value object is of type DataComplex, mark that as read only as the parameter value can be from a user
        // constructed DataTemplate and we don't want this to be modified in any way.
        if (objValue instanceof DataComplex)
        {
          ((DataComplex) objValue).makeReadOnly();
        }

        result.put(key, objValue);
      }
    }

    return result;
  }

  @SuppressWarnings("unchecked")
  private static Object paramToDataObject(Object param, Class<?> paramClass, ProtocolVersion version)
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
    else if (param instanceof List || param instanceof Set)
    {
      return coerceCollection((Collection<?>) param, paramClass, version);
    }
    else if (param instanceof Map)
    {
      return coerceMap((Map<?, ?>) param, paramClass, version);
    }
    else
    {
      return DataTemplateUtil.stringify(param, paramClass);
    }
  }

  /**
   * Given a collection of objects returns the objects either in a DataList, or, if
   * they are PathSpecs (projections), encode them and return a String.
   */
  private static Object coerceCollection(Collection<?> values, Class<?> elementClass, ProtocolVersion version)
  {
    assert values != null;
    DataList dataList = new DataList();
    for (Object value : values)
    {
      if (value != null)
      {
        dataList.add(paramToDataObject(value, elementClass, version));
      }
    }
    return dataList;
  }

  /**
   * Given a map of objects returns the objects in a DataMap.  All key values must be strings.
   */
  private static DataMap coerceMap(Map<?, ?> inputMap, Class<?> elementClass, ProtocolVersion version)
  {
    assert inputMap != null;

    return inputMap.entrySet()
        .stream()
        .collect(Collectors.<Map.Entry<?, ?>, String, Object, DataMap>toMap(
            entry ->
            {
              try
              {
                return (String) entry.getKey();
              }
              catch (ClassCastException e)
              {
                throw new IllegalArgumentException(String.format("Map key '%s' is not of type String",  entry.getKey().toString()));
              }
            },
            entry -> paramToDataObject(entry.getValue(), elementClass, version),
            (older, newer) ->
            {
              throw new IllegalStateException("Multiple mappings for the same key");
            },
            DataMap::new));
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
