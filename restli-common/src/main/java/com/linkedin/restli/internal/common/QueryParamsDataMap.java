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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import com.linkedin.data.DataComplex;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.jersey.api.uri.UriBuilder;
import com.linkedin.jersey.api.uri.UriComponent;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.PathSegment.ListMap;
import com.linkedin.restli.internal.common.PathSegment.MapMap;
import com.linkedin.restli.internal.common.PathSegment.PathSegmentSyntaxException;
import com.linkedin.restli.internal.common.URLEscaper.Escaping;

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
   * Helper method to convert a DataMap into a string by concatenating key-value pairs with "&",
   * sorted by both key and value.
   *
   * @return a String
   */
  public static String dataMapToQueryString(DataMap dataMap, Escaping escaping)
  {
    if (dataMap == null || dataMap.isEmpty())
    {
      return "";
    }

    Map<String, List<String>> queryStringParamsMap = queryString(dataMap);

    StringBuilder sb = new StringBuilder();
    List<String> keys = new ArrayList<String>(queryStringParamsMap.keySet());
    Collections.sort(keys);

    for (String key : keys)
    {
      List<String> values = new ArrayList<String>(queryStringParamsMap.get(key));
      Collections.sort(values);
      for (String value : values)
      {
        sb.append(key)
          .append(RestConstants.KEY_VALUE_DELIMITER)
          .append(URLEscaper.escape(value, escaping))
          .append(RestConstants.SIMPLE_KEY_DELIMITER);
      }
    }

    if (sb.length() > 0)
    {
      sb.deleteCharAt(sb.length() - 1);
    }

    return sb.toString();
  }


  /**
   * Create a map of query string parameters (name, value) from the provided DataMap, in
   * the same manner as parseDataMapKeys() below created a DataMap from a map of query
   * parameters.
   *
   * @param dataMap a dataMap
   * @return the map of query string parameters.
   */
  public static Map<String, List<String>> queryString(DataMap dataMap){
    Map<String, List<String>> result = new HashMap<String, List<String>>();
    iterate("", dataMap, result);
    return result;
  }


  private static void iterate(String keyPrefix,
                              DataComplex dataComplex,
                              Map<String, List<String>> result)
  {
    String separator = ("".equals(keyPrefix)) ? "" : PathSegment.PATH_SEPARATOR;
    if (dataComplex instanceof DataMap)
    {
      DataMap dataMap = (DataMap) dataComplex;
      for (Entry<String, Object> entry : dataMap.entrySet())
      {
        String escapedKeyPrefix = keyPrefix + separator + PathSegment.CODEC.encode(entry.getKey());
        Object object = entry.getValue();
        if (object instanceof DataComplex)
        {
          iterate(escapedKeyPrefix, (DataComplex)object, result);
        }
        else
        {
          // If the current key designates a location in a datamap - the key should be unique - create a new
          // list containing the value and put it in the map.
          result.put(escapedKeyPrefix, Collections.singletonList(object.toString()));
        }
      }
    }
    else if (dataComplex instanceof DataList)
    {
      DataList dataList = (DataList) dataComplex;
      for (int i = 0; i < dataList.size(); i++)
      {
        Object object = dataList.get(i);
        if (object instanceof DataComplex)
        {
          iterate(keyPrefix + "[" + i + "]", (DataComplex)object, result);
        }
        else
        {
          addListValue(keyPrefix, i, object, result);
        }
      }
    }
  }

  /**
   * For backwards compatibility must support multiple query parameter values, but only in case of
   * primitive values, and only at the root level (the parameter name doesn't contain path separators ('.'))
   * It follows, that for any complex values, the list keyed on the parameter name will only contain one element.
   * This method, depending on the path in the datamap, represented by keyPrefix, will either put the provided
   * (primitive) object in the location specified by keyPrefix, wrapped in a newly created singleton list, or
   * add the object to the list at the specified location.
   */
  private static void addListValue(String keyPrefix,
                               int listIndex,
                               Object value,
                               Map<String, List<String>> result)
  {
    assert (!(value instanceof DataComplex));
    // If the current key designates a location in a datamap - the key should be unique - create a new
    // list containing the value and put it in the map.
    if (keyPrefix.contains(PathSegment.PATH_SEPARATOR))
    {
      result.put(keyPrefix + "[" + listIndex + "]", Collections.singletonList(value.toString()));
    }
    else
    {
      // Otherwise - just a simple query parameter, we intentionally leave the listIndex out of the key in this case since the value will go into a list
      if (result.containsKey(keyPrefix))
      {
        result.get(keyPrefix).add(value.toString());
      }
      else
      {
        result.put(keyPrefix, new ArrayList<String>(Collections.singletonList(value.toString())));
      }
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
        String[] key = SEGMENT_DELIMITER_PATTERN.split(entry.getKey());
        parseParameter(key, valueList.get(0), dataMap);
      }
      else
      {
        String parameterName = entry.getKey();
        // In case of multiple parameters ensure they are not delimited or
        // indexed and then simulate the index for each one.
        if(parameterName.indexOf('.') != -1)
          throw new PathSegmentSyntaxException("Multiple values of complex query parameter are not supported");

        if(parameterName.charAt(parameterName.length()-1) == ']')
          throw new PathSegmentSyntaxException("Multiple values of indexed query parameter are not supported");

        if(dataMap.containsKey(parameterName))
          throw new PathSegmentSyntaxException("Conflicting references to key " + parameterName + "[0]");

        else
        {
          dataMap.put(parameterName, new DataList(valueList));
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
   *          - an array of key segments resulting from splitting query parameter name with
   *          '.'
   * @param value the value to be associated with the key
   * @param dataMap the MapMap to place the key-value pairs into
   * @throws PathSegmentSyntaxException
   */
  private static void parseParameter(String key[], String value, MapMap dataMap) throws PathSegmentSyntaxException
  {
    if (key == null || key.length == 0)
      throw new IllegalArgumentException("Error parsing query parameters: query parameter name cannot be empty");

    MapMap currentMap = dataMap;
    for(int index = 0; index < key.length - 1; index++)
    {
      // get the DataMap referenced by the current path segment
      // and parse the rest of the path recursively
      PathSegment pathSegment = PathSegment.parse(key[index]);
      currentMap = pathSegment.getNextLevelMap(currentMap);
    }
    // For the leaf, store the value
    PathSegment pathSegment = PathSegment.parse(key[key.length - 1]);
    pathSegment.putOnDataMap(currentMap, value);
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
  private static DataComplex convertToDataCollection(Map<?, ?> map)
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

  /**
   * Helper method to add the provided query params multimap to the provided UriBuilder, sorting both
   * keys and values within the list for each key.
   *
   * @param uriBuilder
   * @param params
   */
  public static void addSortedParams(UriBuilder uriBuilder,
                                     Map<String, List<String>> params)
  {
    List<String> keysList = new ArrayList<String>(params.keySet());
    Collections.sort(keysList);

    for (String key : keysList)
    {
      // Create a new list to make sure it's modifiable and can be sorted.
      List<String> values = new ArrayList<String>(params.get(key));
      Collections.sort(values);
      for (String value : values)
      {
        // force full encoding as UriBuilder.queryParam(..) won't encode percent signs
        // followed by hex digits
        uriBuilder.queryParam(UriComponent.encode(key, UriComponent.Type.QUERY_PARAM),
                              UriComponent.encode(value, UriComponent.Type.QUERY_PARAM));
      }
    }
  }

  /**
   * Same as above, but taking a DataMap representation of query parameters
   *
   * @param uriBuilder
   * @param params
   */
  public static void addSortedParams(UriBuilder uriBuilder, DataMap params)
  {
    addSortedParams(uriBuilder, queryString(params));
  }
}
