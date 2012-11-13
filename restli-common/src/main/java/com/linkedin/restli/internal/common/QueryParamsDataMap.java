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

package com.linkedin.restli.internal.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import com.linkedin.data.DataComplex;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.restli.internal.common.PathSegment.ListMap;
import com.linkedin.restli.internal.common.PathSegment.MapMap;
import com.linkedin.restli.internal.common.PathSegment.PathSegmentSyntaxException;

/**
 * A utility class for parsing query parameters map into a DataMap.
 * Complex objects are represented by dot-delimited and possibly indexed
 * parameter names.
 *
 * * @author adubman
 */

public class QueryParamsDataMap
{
  private static final Pattern SEGMENT_DELIMITER_PATTERN =
                                                             Pattern.compile(Pattern.quote(PathSegment.PATH_SEPARATOR));

  private QueryParamsDataMap()
  {
  }


  /**
   * Create a map of query string parameters (name, value) from the provided DataMap, in
   * the same manner as parseDataMapKeys() below created a DataMap from a map of query
   * parameters.
   *
   * @param dataMap a dataMap
   * @return the map of query string parameters.
   */
  public static Map<String, String> queryString(DataMap dataMap){
    Map<String, String> result = new HashMap<String, String>();
    iterate("", dataMap, result);
    return result;
  }


  private static void iterate(String keyPrefix,
                              DataComplex dataComplex,
                              Map<String, String> result)
  {
    String separator = ("".equals(keyPrefix)) ? "" : PathSegment.PATH_SEPARATOR;
    if (dataComplex instanceof DataMap)
    {
      DataMap dataMap = (DataMap) dataComplex;
      for (Entry<String, Object> entry : dataMap.entrySet())
      {
        String escapedKey = PathSegment.CODEC.encode(entry.getKey());
        handleEntry(keyPrefix + separator + escapedKey, entry.getValue(), result);
      }
    }
    else if (dataComplex instanceof DataList)
    {
      DataList dataList = (DataList) dataComplex;
      for (int i = 0; i < dataList.size(); i++)
      {
        handleEntry(keyPrefix + "[" + i + "]", dataList.get(i), result);
      }
    }
  }

  private static void handleEntry(String keyPrefix, Object object, Map<String, String> result)
  {
    if (object instanceof DataComplex)
    {
      iterate(keyPrefix, (DataComplex)object, result);
    }
    else
    {
      result.put(keyPrefix, object.toString());
    }
  }

  /**
   * Parse a multi-map representing query parameters into a DataMap, as follows.
   *
   * Multi-indexed parameter names, such as ids[0][2] or ids[0,2] are not
   * currently supported.
   *
   * For example, the following query string:
   *
   * /groupMemberships?ids[0].params.versionTag=tag1&ids[0].params.authToken=tok1&ids[0].memberID=1&ids[0].groupID=2& \
   *                    ids[1].params.versionTag=tag2&ids[1].params.authToken=tok2&ids[1].memberID=2&ids[1].groupID=2& \
   *                    q=someFinder
   *
   * is parsed into the following data map:
   *
   * {"ids" : [
   *    {
   *        "memberID"  : "1",
   *        "groupID"   : "2",
   *        "params"    : {
   *            "authToken" : "tok1",
   *            "versionTag" : "tag1"
   *        }
   *    },
   *    {   "memberID"  : "2",
   *        "groupID"   : "2",
   *        "params"    : {
   *            "authToken" : "tok2",
   *            "versionTag" : "tag2"
   *        }
   *    }
   *  ],
   *  "q" : "someFinder"
   * }
   *
   *
   * Note: at this point the data map is not typed - all names and values are
   * parsed as strings.
   *
   * Note: when parsing indexed parameter names, those will be converted to a list,
   * preserving the order of the values according to the index, but ignoring any
   * possible "holes" in the index sequence. The index values therefore only
   * serve to define order of the parameter values, rather than their actual
   * position in any collection.
   *
   * @param queryParameters the query parameters
   * @return - the DataMap represented by potentially hierarchical keys encoded
   * by the multi-part parameter names.
   *
   * @throws PathSegmentSyntaxException
   */
  public static DataMap parseDataMapKeys(Map<String, List<String>> queryParameters) throws PathSegmentSyntaxException
  {
    // The parameters are parsed into an intermediary structure comprised of
    // HashMap<String,Object> and HashMap<Integer,Object>, defined respectively
    // as MapMap and ListMap for convenience. This is done for two reasons:
    // - first, indexed keys representing lists are parsed into ListMaps keyed on
    //   index values, since the indices may come in any order in the query parameter,
    //   while we want to preserve the order.
    // - second, DataMap only accepts Data objects as values, so ListMaps cannot
    //   be stored there, so using an intermediary structure even for maps.

    MapMap dataMap = new MapMap();

    for (Map.Entry<String, List<String>> entry : queryParameters.entrySet())
    {
      // As per the notation above, we no longer support multiple occurrences of
      // a parameter (considering its full multi-part and indexed name), i.e
      // there should be only a single entry in each list. For backward compatibility
      // as well as ease of use, repeated parameters are still allowed if they
      // are "simple", i.e. they are not multi-part or indexed.
      List<String> valueList = entry.getValue();
      if (valueList.size() == 1)
      {
        List<String> key = Arrays.asList(SEGMENT_DELIMITER_PATTERN.split(entry.getKey()));
        parseParameter(key, valueList.get(0), dataMap);
      }
      else {
        String parameterName = entry.getKey();
        // In case of multiple parameters ensure they are not delimited or
        // indexed and then simulate the index for each one.
        if (parameterName.contains(PathSegment.PATH_SEPARATOR))
          throw new PathSegmentSyntaxException("Multiple values of complex query parameter are not supported");

        if (PathSegment.parse(parameterName).getIndex() != null)
          throw new PathSegmentSyntaxException("Multiple values of indexed query parameter are not supported");

        int i=0;
        Iterator<String> iterator = valueList.iterator();
        while (iterator.hasNext())
        {
          String key = parameterName + "[" + i + "]";
          i++;
          parseParameter(Arrays.asList(key), iterator.next(), dataMap);
        }
      }
    }

    return (DataMap)convertToDataCollection(dataMap);
  }

  /**
   * Place a specified key-value pair into the supplied datamap. Treat the dot-delimited
   * key name as the path within the datamap to put the value to, as defined above.
   *
   * @param key
   *          - a list of key segments resulting from splitting query parameter name with
   *          '.'
   * @param value the value to be associated with the key
   * @param dataMap the MapMap to place the key-value pairs into
   * @throws PathSegmentSyntaxException
   */
  private static void parseParameter(List<String> key, String value, MapMap dataMap) throws PathSegmentSyntaxException
  {
    if (dataMap == null)
      throw new IllegalArgumentException("Query parameters target data map is null");

    if (key == null || key.size() == 0)
      throw new IllegalArgumentException("Error parsing query parameters: query parameter name cannot be empty");

    PathSegment pathSegment = PathSegment.parse(key.get(0));

    // If a leaf, store the value
    if (key.size() == 1)
    {
      pathSegment.putOnDataMap(dataMap, value);
    }
    // Otherwise, get the DataMap referenced by the current path segment
    // and parse the rest of the path recursively
    else
    {
      MapMap nextLevelDataMap = pathSegment.getNextLevelMap(dataMap);
      parseParameter(key.subList(1, key.size()), value, nextLevelDataMap);
    }
  }

  /**
   * The method recursively traverses the input Map and transforms it as follows:
   * - wherever encounters instances of ListMap (Map<Integer,Object>), converts
   *   those to DataList, preserving key order but ignoring any "holes" in key sequences.
   * - wherever encounters instances of MapMap (Map<String,Object>) converts them
   *   into DataMap.
   *
   * This is done since while parsing out indexed query parameters it's convenient to
   * parse them into a map due to arbitrary order in which they may appear, while if they
   * are defined in the schema as a list, a DataList is expected during validation.
   *
   * @param map the Map to transform
   * @return DataMap or DataList, depending on the type of the input Map
   */
  private static Object convertToDataCollection(Map<?, ?> map)
  {
    // If this map is not an instance of ListMap, just call this method
    // recursively on every value that is itself a map
    if (map instanceof MapMap)
    {
      DataMap result = new DataMap();
      MapMap mapMap = (MapMap) map;
      for (Entry<String, Object> entry : mapMap.entrySet())
      {
        Object value = entry.getValue();
        if (value instanceof Map<?, ?>)
          value = convertToDataCollection((Map<?, ?>) value);
        result.put(entry.getKey(), value);
      }
      return result;
    }

    // If an instance of a list, call recursively on any Map entry, and also
    // convert this map into a list preserving key order.
    if (map instanceof ListMap)
    {
      DataList result = new DataList();
      ListMap listMap = (ListMap)map;

      List<Integer> sortedKeys = new ArrayList<Integer>(listMap.keySet());
      Collections.sort(sortedKeys);

      for (Integer key : sortedKeys)
      {
        Object object = map.get(key);
        if (object instanceof Map<?, ?>)
          object = convertToDataCollection((Map<?, ?>) object);
        result.add(object);
      }
      return result;
    }

    throw new IllegalArgumentException("Only MapMap or ListMap input argument types are allowed");
  }
}
