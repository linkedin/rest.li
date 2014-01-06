package com.linkedin.restli.internal.client;


import com.linkedin.data.DataComplex;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.template.DataTemplate;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * @author kparikh
 */
public class QueryParamsUtil
{
  public static DataMap convertToDataMap(Map<String, Object> queryParams)
  {
    return convertToDataMap(queryParams, RestConstants.DEFAULT_PROTOCOL_VERSION);
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
      result.put(key, paramToDataObject(value));
    }
    result.makeReadOnly();
    return result;
  }

  private static Object paramToDataObject(Object param)
  {
    if (param == null)
    {
      return null;
    }
    else if (param instanceof ComplexResourceKey)
    {
      return ((ComplexResourceKey) param).toDataMap();
    }
    else if (param instanceof Object[])
    {
      return new DataList(coerceIterable(Arrays.asList((Object[]) param)));
    }
    else if (param.getClass().isArray())
    {
      // not an array of objects but still an array - must be an array of primitives
      return new DataList(stringifyArray(param));
    }
    else if (param instanceof DataTemplate)
    {
      @SuppressWarnings("rawtypes")
      final DataTemplate dataTemplate = (DataTemplate)param;
      return dataTemplate.data();
    }
    else if (param instanceof Iterable)
    {
      return new DataList(coerceIterable((Iterable<?>) param));
    }
    else
    {
      return DataTemplateUtil.stringify(param);
    }
  }

  /**
   * given an iterable of objects returns a list of (non-null) Objects,
   * which can be Strings or DataMap
   *
   */
  private static List<Object> coerceIterable(Iterable<?> values)
  {
    assert values != null;
    List<Object> objects =
        new ArrayList<Object>(values instanceof Collection ? ((Collection<?>) values).size() : 10);
    for (Object value : values)
    {
      if (value != null)
      {
        objects.add(paramToDataObject(value));
      }
    }
    return objects;
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
