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

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import com.linkedin.data.DataMap;
import static com.linkedin.data.transform.filter.FilterUtil.*;

/**
 * Filter can be considered as a tree. For example filter:
 * <code>{ a: { b: 1 } c: { d: 0 } }</code><br>
 * can be considered as a tree with root having two children: <code>a</code> and
 * <code>c</code>, where child <code>a</code> is equal to <code>{ b: 1}</code>
 * and child <code>c</code> is equal to <code>{ d: 0 }</code>.<br>
 * Every node in this tree describes how to filter data in respective node in data object.
 * Filter can explicitly specify if a field should be selected (if it's value in filter is 1) or filtered out
 * (if it's value in filtered is 0). If a field does not have value 0 or 1 in filter, then
 * default mode decides if it should be filtered out or not. This class is responsible for
 * calculating default mode for a node in filter tree. The value of default mode depends on values
 * of children in the node. The intuitive rule is the following: if some of fields in the node or it's
 * children were selected using positive filter (1), it means that all other fields in this node
 * should be filtered out. This behavior is necessary to preserve natural meaning of negative and positive filter.
 * For example if I declare negative filter which removes field 'password' from an object I don't have to
 * explicitly specify that all other fields need to be preserved in an object. Similarly, if I define
 * positive filter, which selects only 'name' field from the object I don't need to explicitly specify that all
 * other fields should be removed.
 * <p>The precise set rules for calculation default mode is the following:
 * <ul>
 * <li>if node contains a wildcard which specifies simple value (0 or 1) for all fields, then it is used
 * as a default mode for this node</li>
 * <li>if any child of a node has a value 1, then default mode is {@link NodeMode#HIDE_LOW} </li>
 * <li>if node contains array's range specification, then default mode is {@link NodeMode#HIDE_LOW}</li>
 * <li>if any child of a node is a complex filter with default mode equal to {@link NodeMode#HIDE_LOW} or
 * {@link NodeMode#SHOW_HIGH}, then default mode is {@link NodeMode#HIDE_LOW}</li>
 * <li>finally, if none of above conditions is true, then default node mode is {@link NodeMode#SHOW_LOW}</li>
 * </ul>
 *
 * <p>This class uses memoization to decrease amortized time of calculation per node to O(1).
 *
 * @author jodzga
 * @see NodeMode
 */
public class DefaultNodeModeCalculator
{

  // used for memoization of default node modes
  private IdentityHashMap<DataMap, NodeMode> _defaultNodeModes = new IdentityHashMap<>();
  /**
   * Reruns default NodeMode for given filter node.
   * @param opNode DataMap containing filter, for which default mode needs to be determined
   * @return default NodeMode for given filter
   */
  public NodeMode getDefaultNodeMode(DataMap opNode)
  {

    assert opNode != null;

    NodeMode defaulNodeMode = _defaultNodeModes.get(opNode);
    if (defaulNodeMode == null)
    {

      //wildcard symbol can directly define default mode if it is of simple type
      Object wldcrd = opNode.get(FilterConstants.WILDCARD);
      if (wldcrd != null)
      {
        if (wldcrd.equals(FilterConstants.NEGATIVE))
          defaulNodeMode = NodeMode.HIDE_HIGH;
        else
          if (isMarkedAsMergedWith1(opNode))
            defaulNodeMode = NodeMode.SHOW_HIGH;
      }

      // recursive definition of value for defaultNodeMode:
      // if there exist child with positive mask, then default mode is hide_low,
      // otherwise it is show_low
      // array ranges are treated as a positive mask

      Iterator<Entry<String, Object>> it = opNode.entrySet().iterator();
      while (it.hasNext() && defaulNodeMode == null)
      {
        Entry<String, Object> entry = it.next();
        Object o = entry.getValue();

        if (entry.getKey().equals(FilterConstants.START) || entry.getKey().equals(FilterConstants.COUNT))
          //array range is treated as a positive mask expression
          defaulNodeMode = NodeMode.HIDE_LOW;
        else if ((o instanceof Integer) && ((Integer) o) == 1)
          defaulNodeMode = NodeMode.HIDE_LOW;
        else if (o.getClass() == DataMap.class)
        {
          NodeMode childNodeMode = getDefaultNodeMode((DataMap) o);
          if (childNodeMode.equals(NodeMode.HIDE_LOW)
              || childNodeMode.equals(NodeMode.SHOW_HIGH))
            defaulNodeMode = NodeMode.HIDE_LOW;
        }
      }
      if (defaulNodeMode == null)
        defaulNodeMode = NodeMode.SHOW_LOW;
    }
    _defaultNodeModes.put(opNode, defaulNodeMode);

    return defaulNodeMode;
  }

}
