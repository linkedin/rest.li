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

package com.linkedin.data.it;


import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.element.DataElement;
import com.linkedin.data.schema.PathSpec;
import java.util.ArrayList;
import java.util.List;


/**
 * Evaluate if the Data object's fully qualified path
 * complies with the specified path pattern.
 * <p>
 *
 * The path pattern is provided as input to this {@link Predicate}.
 * It is specified as a list of pattern components.
 * A pattern component may be an {@link Object} or one
 * of the predefined constants. If the pattern component is
 * not a pre-defined constant, the {@link Object} specifies
 * an equals match of a path component. The {@link Object} should be an
 * {@link Integer} for matching an index in a {@link DataList}
 * or a {@link String} for matching an key in a {@link DataMap}.
 * <p>
 *
 * If the pattern component is one of the predefined constants, it
 * specifies a wildcard match. In the examples below, '/' (slash) is
 * used to delimit path components.
 * <p>
 *
 * The predefined constants are:
 * <ul>
 * <li> {@link Wildcard#ANY_ZERO_OR_MORE} matches any zero or more
 *      path components. For example, <br>
 *      {@code (Wildcard.ANY_ZERO_OR_MORE)}
 *      matches /a, /i, /a/b, /a/i/,
 *      /a/b/c, /a/b/i, /a/b/c/d, /a/b/c/i essentially
 *      all Data objects in the object graph (except the root.) <br>
 *      {@code (Wildcard.ANY_ZERO_OR_MORE, "i")}
 *      matches /i, /a/i, /a/b/i, /a/b/c/i.
 * <li> {@link Wildcard#ANY_ZERO_OR_ONE} matches any zero or one
 *      path components. For example, <br>
 *      {@code (Wildcard.ANY_ZERO_OR_ONE)}
 *      matches /a, /i <br>
 *      {@code (Wildcard.ANY_ZERO_OR_ONE, "i")}
 *      matches /i, /a/i. <br>
 * <li> {@link Wildcard#ANY_ONE_OR_MORE} matches any one or more
 *      path components. For example, <br>
 *      {@code (Wildcard.ANY_ONE_OR_MORE)}
 *      matches all Data objects in the object graph (except the root.) <br>
 *      {@code (Wildcard.ANY_ONE_OR_MORE, "i")}
 *      matches /a/i, /a/b/i, /a/b/c/i.
 * <li> {@link Wildcard#ANY_ONE} matches any one path component.
 *      For example, <br>
 *      {@code (Wildcard.ANY_ONE)}
 *      matches /a, /i. <br>
 *      {@code (Wildcard.ANY_ONE, Wildcard.ANY_ONE)}
 *      matches /a/b, /a/i. <br>
 * </ul>
 *
 * Different constants may be specified more than once.
 * A particular constant may also be specified more than once.
 * For example,
 * {@code (Wildcard.ANY_ZERO_OR_MORE, "b", Wildcard.ANY_ZERO_OR_ONE, "i")}
 * matches /a/b/i, /a/b/c/i.
 *
 * @author slim.
 */
public class PathMatchesPatternPredicate implements Predicate
{
  /**
   * Constructor.
   *
   * @param patterns provides the pattern components.
   * @throws IllegalArgumentException if any of the pattern component is not a {@link String},
   *                                  {@link Integer} or {@link Wildcard}.
   */
  public PathMatchesPatternPredicate(Object... patterns) throws IllegalArgumentException
  {
    _patterns = patterns;
    generateComponentMatches();
  }

  public PathMatchesPatternPredicate(PathSpec pathSpec) throws IllegalArgumentException
  {
    this(pathSpecToPathMatchPattern(pathSpec));
  }

  private static Object[] pathSpecToPathMatchPattern(PathSpec pathSpec)
  {
    List<String> pathComponents = pathSpec.getPathComponents();
    Object[] results = new Object[pathComponents.size()];
    int i = 0;
    for (String pathComponent : pathComponents)
    {
      if (pathComponent == PathSpec.WILDCARD)
      {
        results[i++] = Wildcard.ANY_ONE;
      }
      else
      {
        results[i++] = pathComponent;
      }
    }
    return results;
  }

  @Override
  public boolean evaluate(DataElement element)
  {
    return pass(element, 0);
  }

  private boolean pass(DataElement element, int i)
  {
    boolean pass = true;
    DataElement currentElement = element;
    for (; i < _matches.size(); ++i)
    {
      Match match = _matches.get(i);
      if (match.name == null)
      {
        int d = 0;
        while (d < match.minDistance)
        {
          if (currentElement.getParent() == null)
          {
            pass = false;
            break;
          }
          currentElement = currentElement.getParent();
          ++d;
        }
        if (pass == false)
        {
          break;
        }
        if (i == (_matches.size() - 1))
        {
          while (d < match.maxDistance && currentElement.getParent() != null)
          {
            currentElement = currentElement.getParent();
            ++d;
          }
        }
        else
        {
          Match nextMatch = _matches.get(i + 1);
          int searchDistance = (match.maxDistance == Integer.MAX_VALUE ?
                                Integer.MAX_VALUE :
                                match.maxDistance - match.minDistance + 1);
          boolean matched = false;
          while (searchDistance > 0 && currentElement.getParent() != null)
          {
            if (matchName(nextMatch, currentElement.getName()))
            {
              boolean subPass = pass(currentElement.getParent(), i + 2);
              if (subPass)
              {
                matched = true;
                i = _matches.size();
                while (currentElement.getParent() != null)
                {
                  currentElement = currentElement.getParent();
                }
                break;
              }
            }
            searchDistance--;
            currentElement = currentElement.getParent();
          }
          if (matched == false)
          {
            pass = false;
            break;
          }
          break;
        }
      }
      else
      {
        if (currentElement.getParent() == null || matchName(match, currentElement.getName()) == false)
        {
          pass = false;
          break;
        }
        currentElement = currentElement.getParent();
      }
    }
    if (pass)
    {
      assert(i == _matches.size());
      pass = (currentElement.getParent() == null);
    }

    return pass;
  }

  private boolean matchName(Match match, Object name)
  {
    return match.name.equals(name);
  }

  /**
   * Compute the matches that have to be performed.
   *
   * @throws IllegalArgumentException if the input pattern is not an {@link Integer},
   *                                  {@link String}, or {@link Wildcard}.
   */
  private void generateComponentMatches() throws IllegalArgumentException
  {
    for (int i = _patterns.length - 1; i >= 0; --i)
    {
      Match match = new Match();
      Object component = _patterns[i];
      Class<?> componentClass = component.getClass();
      if (componentClass == Wildcard.class)
      {
        match.name = null;
        match.minDistance = 0;
        match.maxDistance = 0;
        while (i >= 0 &&  (component = _patterns[i]).getClass() == Wildcard.class)
        {
          Wildcard w = (Wildcard) component;
          switch (w)
          {
            case ANY_ZERO_OR_MORE:
              match.maxDistance = Integer.MAX_VALUE;
              break;
            case ANY_ZERO_OR_ONE:
              if (match.maxDistance != Integer.MAX_VALUE)
              {
                match.maxDistance++;
              }
              break;
            case ANY_ONE_OR_MORE:
              if (match.minDistance != Integer.MAX_VALUE)
              {
                match.minDistance++;
              }
              match.maxDistance = Integer.MAX_VALUE;
              break;
            case ANY_ONE:
              match.minDistance++;
              if (match.maxDistance != Integer.MAX_VALUE)
              {
                match.maxDistance++;
              }
              break;
          }
          --i;
        }
        ++i;
        assert(match.minDistance <= match.maxDistance);
      }
      else if (componentClass == String.class || componentClass == Integer.class)
      {
        match.name = component;
      }
      else
      {
        throw new IllegalArgumentException("Component is not a String, Integer, or Wildcard: " + component);
      }
      _matches.add(match);
    }
    // dump(out);
  }

  // For debugging use only
  /*
  private void dump(PrintStream out)
  {
    out.println("Pattern: ");
    boolean first = true;
    for (Object o : _patterns)
    {
      if (first)
      {
        first = false;
      }
      else
      {
        out.print(", ");
      }
      out.print(o.toString());
    }
    if (first == false)
    {
      out.println();
    }
    out.println("Match:");
    for (Match m : _matches)
    {
      out.println("name=" + m.name + ", min=" + m.minDistance + ", max=" + m.maxDistance);
    }
  }
  */

  private final Object[] _patterns;
  private final List<Match> _matches = new ArrayList<>();

  /**
   * A {@link Match} holds either a name that represents an exact match of a path component or
   * a number of path components that have been wildcarded.
   * <p>
   *
   * If the name is not null, it indicates an exact match.
   * <p>
   *
   * If the name is null, than {@code minDistance} provides the minimum number of wildcarded
   * path components, and {@code maxDistance} provides the maximum number of wildcarded
   * path components. These distances are computed as follows:
   * <ul>
   * <li>
   * </ul>
   */
  private static final class Match
  {
    Object name;
    int minDistance;
    int maxDistance;
  }

  // private static final PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out));
}
