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

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Name[index] representation of a path segment of a dot-delimited path referencing an
 * entry in a complex data object.
 *
 * @author adubman
 *
 */
public class PathSegment
{
  public static final String      PATH_SEPARATOR = ".";
  private static final Pattern    INDEX_PATTERN  = Pattern.compile("(^.+)\\[(\\d+)\\]$");
  private static final char[]     ENCODED_CHARS  = { '[', ']', '.' };
  public static final AsciiHexEncoding CODEC          = new AsciiHexEncoding('~', ENCODED_CHARS);
  private final String            _name;
  private final Integer           _index;

  private PathSegment(String name, Integer index) throws PathSegmentSyntaxException
  {
    // Should never happen as parse() checks that name is not empty
    assert (name != null);

    try
    {
      this._name = CODEC.decode(name);
    }
    catch (AsciiHexEncoding.CannotDecodeException e1)
    {
      throw new PathSegmentSyntaxException("Cannot decode key name " + name);
    }

    this._index = index;
  }

  /**
   * Parse a (possibly) indexed path segment out of a string. Make sure no path delimiters
   * appear in the string, as this would not be an individual path segment.
   *
   * @param string a string to parse.
   * @return an PathSegment object.
   * @throws PathSegmentSyntaxException
   *           if invalid index value or key segment delimiter appears in the input
   *           string.
   */
  public static PathSegment parse(String string) throws PathSegmentSyntaxException
  {
    if (string == null || string.trim().isEmpty())
      return null;

    string = string.trim();

    if (string.contains(PATH_SEPARATOR))
      throw new PathSegmentSyntaxException("Path segment parsing error.");

    Matcher matcher = INDEX_PATTERN.matcher(string);

    if (!matcher.find())
      return new PathSegment(string, null); // Not an indexed key

    String name = matcher.group(1);
    String indexStr = matcher.group(2);

    int index;

    try
    {
      index = Integer.parseInt(indexStr);
    }
    catch (NumberFormatException e)
    {
      // Should never happen as the regex only matches integers in this group
      throw new PathSegmentSyntaxException("Only integer key indices are allowed");
    }

    return new PathSegment(name, index);
  }

  /**
   * @return the name
   */
  public String getName()
  {
    return _name;
  }

  /**
   * @return the index
   */
  public Integer getIndex()
  {
    return _index;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder(_name);

    if (_index != null)
      sb.append("[").append(_index).append("]");

    return sb.toString();
  }

  /**
   * Stores this object in the provided Map. In case of an indexed key use _name to
   * get a corresponding List, and _index for the element in the list. In case of
   * a simple key just put the value at the name key
   *
   * No overrides are allowed.
   * @param map the map to store the value in
   * @param value the value to put in the map
   * @throws PathSegmentSyntaxException if the object is unable to be placed
   */
  public void putOnDataMap(MapMap map, String value) throws PathSegmentSyntaxException
  {
    // If not an indexed key, just store the value in the provided Map
    // with the current _name as key. If already there - error
    if (_index == null)
    {
      if (map.get(_name) != null)
        throw new PathSegmentSyntaxException("Duplicate references to key: " + _name);

      map.put(_name, value);
      return;
    }

    // Otherwise, get the element at the key _name, which is assumed to be
    // a Map keyed on Integer (as opposed to String for DataMaps), and store
    // the value at the index key in that map.
    ListMap listMap;
    Object entry = map.get(_name);
    if (entry == null)
    {
      listMap = new ListMap();
      map.put(_name, listMap);
    }
    else if (entry instanceof ListMap)
    {
      listMap = (ListMap)entry;
    }
    else
    {
      throw new PathSegmentSyntaxException("Conflicting references to key: " + toString());
    }

    // Now put the value on the _index entry of the list if it's not there.
    // If it is - exception
    if (listMap.get(_index) != null)
      throw new PathSegmentSyntaxException("Duplicate references to key: " + _name);

    listMap.put(_index, value);
  }

  /**
   * Get the value referenced by this path segment in the provided DataMap
   *
   * The method assumes the current key is not a leaf segment of the key path, i.e.
   * it always references a Map value in the current Map.
   *
   * @param map the Map
   * @return a Map of Strings to Maps
   * @throws PathSegmentSyntaxException if the current key is a leaf segment
   */
  public MapMap getNextLevelMap(MapMap map) throws PathSegmentSyntaxException
  {
    Object object = map.get(_name);

    if (object == null)
    {
      MapMap nextLevelDataMap = new MapMap();
      // If not an indexed path segment, create a new Map, put it at the
      // current key and return it.
      if (_index == null)
      {
        map.put(_name, nextLevelDataMap);
      }
      // If an indexed key, create a new ListMap, put it at the current _name
      // and put next level data map at the current _index in the List Map
      else
      {
        ListMap listMap = new ListMap();
        map.put(_name, listMap);
        listMap.put(_index, nextLevelDataMap);
      }
      return nextLevelDataMap;
    }

    if (_index == null)
    {
      if (object instanceof MapMap)
        return (MapMap) object;
      throw new PathSegmentSyntaxException("Conflicting references to key " + toString());
    }

    if (object instanceof ListMap)
    {
      ListMap list = (ListMap) object;

      Object object2 = list.get(_index);

      if (object2 == null)
      {
        MapMap nextLevelMap = new MapMap();
        list.put(_index, nextLevelMap);
        return nextLevelMap;
      }
      else if (object2 instanceof MapMap)
      {
        return (MapMap) object2;
      }
      else
      {
        throw new PathSegmentSyntaxException("Conflicting references to key "
            + toString());
      }
    }

    throw new PathSegmentSyntaxException("Conflicting references to key " + toString());
  }

  public static class PathSegmentSyntaxException extends Exception
  {
    private static final long serialVersionUID = 1L;

    /**
     * Initialize a PathSegmentSyntaxException based on the given message.
     *
     * @param message the exception message
     */
    public PathSegmentSyntaxException(String message)
    {
      super(message);
    }
  }

  static class ListMap extends HashMap<Integer, Object>
  {
    private static final long serialVersionUID = 1L;
  }

  static class MapMap extends HashMap<String, Object>
  {
    private static final long serialVersionUID = 1L;
  }
}
