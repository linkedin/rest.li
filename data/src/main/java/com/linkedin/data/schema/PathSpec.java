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

/**
 * $Id: $
 */

package com.linkedin.data.schema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;


/**
 * A PathSpec represents a path within a complex data object.  PathSpecs may be obtained from
 * generated RecordTemplate subclasses using the .fields() method.  Because the primary use cases
 * for PathSpecs are to generate uniform references in hierarchical data maps
 * (used in the data-transform) component, the path segments are all represented as String,
 * including the representation for wildcards and integer array indices.
 *
 * @author Josh Walker
 * @version $Revision: $
 */

public class PathSpec
{
  //use this specific instance to differentiate a true wildcard from a "*" key
  public static final String WILDCARD = new String("*");

  public static final String ATTR_ARRAY_START = "start";
  public static final String ATTR_ARRAY_COUNT = "count";

  /**
   * Construct a new {@link PathSpec} from a list of parent segments and a current
   * segment.
   *
   * @param parentPath provides the parent path.
   * @param segment provides the current path segment.
   */
  public PathSpec(List<String> parentPath, String segment)
  {
    _path = new ArrayList<String>(parentPath.size()+1);
    _path.addAll(parentPath);
    _path.add(segment);
  }

  /**
   * Construct a new {@link PathSpec} from just one segment.
   *
   * @param segment provides the only segment of the path.
   */
  public PathSpec(String segment)
  {
    _path = new ArrayList<String>(1);
    _path.add(segment);
  }

  /**
   * Construct a new {@link PathSpec} from a list of segments.
   *
   * @param segments provides the list of path segments.
   */
  public PathSpec(String... segments)
  {
    _path = new ArrayList<String>(Arrays.asList(segments));
  }

  /**
   * Construct a new {@link PathSpec} from {@link java.util.Collection} type
   *
   * @param pathSpecCollection the collection that contains path segments.
   */
  public PathSpec(Collection<String> pathSpecCollection)
  {
    _path = new ArrayList<>(pathSpecCollection.size());
    _path.addAll(pathSpecCollection);
  }

  /**
   * Construct a new empty {@link PathSpec} that has no segments.
   */
  public PathSpec()
  {
    _path = Collections.emptyList();
  }

  public void setAttribute(String name, Object value)
  {
    _attributes.put(name, value);
  }

  /**
   * Return an empty {@link PathSpec} that has no segments.
   *
   * @return an empty {@link PathSpec} that has no segments.
   */
  public static PathSpec emptyPath()
  {
    return EMPTY_PATH_SPEC;
  }

  public List<String> getPathComponents()
  {
    return Collections.unmodifiableList(_path);
  }

  /**
   * Specifies whether this PathSpec has no segment
   * @return <code>true</code> if this pathSpec has no segment, <code>false</code> otherwise
   */
  public boolean isEmptyPath()
  {
    return _path.isEmpty();
  }

  public Map<String, Object> getPathAttributes()
  {
    return Collections.unmodifiableMap(_attributes);
  }

  /**
   * Returns a new PathSpec using the same path as this PathSpec but truncated of its last element.
   * The parent of an empty PathSpec is itself.
   */
  public PathSpec getParent()
  {
    if (_path.size() <= 1)
    {
      return emptyPath();
    }
    else
    {
      return new PathSpec(_path.subList(0, _path.size() - 1));
    }
  }

  @Override
  public String toString()
  {
    StringBuilder rep = new StringBuilder();
    for (String s: getPathComponents())
    {
      rep.append(SEPARATOR);
      rep.append(s);
    }

    boolean beforeAttributes = true;
    if (!_attributes.isEmpty())
    {
      for (Map.Entry<String, Object> attribute: _attributes.entrySet())
      {
        rep.append(beforeAttributes ? PATH_ATTR_SEPARATOR : ATTR_SEPARATOR);
        rep.append(attribute.getKey());
        rep.append(ATTR_KEY_VALUE_SEPARATOR);
        rep.append(attribute.getValue());

        beforeAttributes = false;
      }
    }

    return rep.toString();
  }

  /**
   * Test whether a string match the syntax pattern of {@link PathSpec#toString()}
   *
   * @param pathSpecStr string under validation
   * @return whether it is valid
   */
  public static boolean validatePathSpecString(String pathSpecStr)
  {
    return PATHSPEC_PATTERN.matcher(pathSpecStr).matches();
  }

  @Override
  public boolean equals(Object obj)
  {
    if (obj instanceof PathSpec)
    {
      PathSpec other = (PathSpec)obj;
      if (other._path.size() != _path.size())
      {
        return false;
      }
      for (int ii=0; ii<_path.size(); ++ii)
      {
        //Identity comparisons
        if ((_path.get(ii) == WILDCARD) != (other._path.get(ii) == WILDCARD))
        {
          return false;
        }
        if (! _path.get(ii).equals(other._path.get(ii)))
        {
          return false;
        }
      }

      return Objects.equals(_attributes, other._attributes);
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(_path, _attributes);
  }

  private final List<String> _path;
  private final Map<String, Object> _attributes = new HashMap<>();
  private static final PathSpec EMPTY_PATH_SPEC = new PathSpec();
  public static final char SEPARATOR = '/';
  private static final char ATTR_SEPARATOR = '&';
  private static final char PATH_ATTR_SEPARATOR = '?';
  private static final char ATTR_KEY_VALUE_SEPARATOR = '=';
  private static final Pattern PATHSPEC_PATTERN = Pattern.compile(String.format("^(%s[^%<s\\s]+)+", SEPARATOR));
}
