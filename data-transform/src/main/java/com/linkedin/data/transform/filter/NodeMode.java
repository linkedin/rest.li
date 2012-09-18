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
 * $id$
 */
package com.linkedin.data.transform.filter;

/**
 * This enum represent all possible values of default mode in a filter.
 * Default mode specifies what to do with a field if it is not explicitly
 * selected or filtered out in a filter. There are four possible values for
 * default mode (in priority order, from lowest to highest):
 * <ul>
 * <li>{@link #SHOW_LOW}</li>
 * <li>{@link #HIDE_LOW}</li>
 * <li>{@link #SHOW_HIGH}</li>
 * <li>{@link #HIDE_HIGH}</li>
 * </ul>
 * Values, which starts with "hide", mean that fields are filtered out by default,
 * values, which start with "show", mean that fields are not filtered out by default.
 * The reason for having 4 values with different priorities is to support filters composition.
 * Consider the following example:<br>
 * filter1 = <code>{ a: 1 }</code> - client is interested only in field a<br>
 * filter2 = <code>{ b: 0 }</code> - security rule hides field b<br>
 * The composition of the two looks like:<br>
 * filter3 = <code>{ a: 1, b: 0 }</code><br>
 * Suppose we have a data object:<br>
 * <code>{ a: a, b: b, c: c }</code><br>
 * Applying filter1 should return object: <code>{ a: a }</code><br>
 * Applying filter2 should return object: <code>{ a: a, c: c }</code><br>
 * Applying filter3 should return object: <code>{ a: a }</code><br>
 * For filter1, default behavior for fields not mentioned in filter is to filter it out. For filter2, the default
 * behavior is to leave field in. For filter3, it is again to filter it out. This logic is achieved
 * by having 4 possible values of default mode. For filter1, the value of default mode is {@link #HIDE_LOW}, for
 * filter2 it is {@link #SHOW_LOW}. For filter3 the value is {@link #HIDE_LOW}, because it has higher priority
 * than {@link #SHOW_LOW}.
 * <p>It is also possible that default mode for node is {@link #SHOW_HIGH} and {@link #HIDE_HIGH}. The first case
 * happens when node contains wildcard, which is equivalent to 'select all' e.g. <code>{ $*=1, a=0 }</code>.
 * The second case happens when node contains wildcard equal to 0 e.g. <code>{ $*=0 }</code>.
 *
 *
 * @author jodzga
 */
public enum NodeMode
{
  SHOW_LOW(0, 3), HIDE_LOW(1, 2), SHOW_HIGH(2, 1), HIDE_HIGH(3, 0);

  private final Integer _priority;
  private final Integer _representation;

  NodeMode(int priority, Integer representation) {
    this._priority = priority;
    this._representation = representation;
  }

  /**
   * @return representation
   */
  public Integer representation()
  {
    return _representation;
  }

  /**
   * @return priority
   */
  public Integer priority()
  {
    return _priority;
  }

  /**
   * given the represenation integer, return the corresponding NodeMode.
   * Returns null if the integer is not a valid representation.
   *
   * @param r representation, and integer between 0 and 3, inclusive
   * @return the corresponding {@link NodeMode}
   */
  public static NodeMode fromRepresentation(Integer r)
  {
    switch (r)
    {
    case 0:
      return HIDE_HIGH;
    case 1:
      return SHOW_HIGH;
    case 2:
      return HIDE_LOW;
    case 3:
      return SHOW_LOW;
    default:
      return null;
    }
  }

}
