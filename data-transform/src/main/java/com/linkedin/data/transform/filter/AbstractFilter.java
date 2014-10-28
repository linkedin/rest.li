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

package com.linkedin.data.transform.filter;


import com.linkedin.data.DataComplex;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.transform.DataComplexProcessor;
import com.linkedin.data.transform.DataProcessingException;
import com.linkedin.data.transform.Escaper;

import java.util.HashMap;
import java.util.Map;

import static com.linkedin.data.transform.filter.FilterConstants.COUNT;
import static com.linkedin.data.transform.filter.FilterConstants.NEGATIVE;
import static com.linkedin.data.transform.filter.FilterConstants.POSITIVE;
import static com.linkedin.data.transform.filter.FilterConstants.START;
import static com.linkedin.data.transform.filter.FilterUtil.getIntegerWithDefaultValue;


/**
 * This class captures the generic logic of Data filtering.
 * Note that only {@link DataComplex} objects are valid input. Because it is required that the data is acyclic.
 *
 * @author Keren Jin
 */
public abstract class AbstractFilter
{
  public Object filter(Object data, DataMap opNode)
  {
    if ((data != null) && (opNode != null))
    {
      // _defaultMode specifies if field should be filtered out by default
      final NodeMode defaultMode = _dafaultNodeModeCalculator.getDefaultNodeMode(opNode);

      // get complex wildcard if it exist e.g. $*: {...}
      final DataMap complexWildCard = getComplexWildCard(opNode);

      if (data.getClass() == DataList.class)
      {
        return filterDataList(opNode, ((DataList) data));
      }
      else if (data.getClass() == DataMap.class)
      {
        return filterDataMap(opNode, ((DataMap) data), defaultMode, complexWildCard);
      }
      else
      {
        return onError(null,
                       "Data type in instruction must be DataMap or DataList, but is: %1$s",
                       data.getClass().getName());
      }
    }
    else
    {
      return onError(null, "Either data or operation is null");
    }
  }

  protected abstract Object onFilterDataList(DataList data, int start, int count, Object operation);
  protected abstract Object onFilterDataMap(DataMap data, Map<String, Object> fieldToOperation);
  protected abstract Object onError(Object field, String format, Object... args);
  protected abstract boolean isValidDataMapFieldOperation(Map<String, Object> result, String name, Object operation);

  /**
   * Returns complex wildcard mask or null if wildcard is not defined or is of simple type
   *
   */
  private static DataMap getComplexWildCard(DataMap opNode)
  {
    assert opNode != null;

    final Object o = opNode.get(FilterConstants.WILDCARD);
    if ((o != null) && (o.getClass() == DataMap.class))
    {
      return (DataMap) o;
    }
    else
    {
      return null;
    }
  }

  /**
   * Returns true if node contained $*=0, which explicitly removes all fields
   *
   */
  private static boolean areFieldsExplicitlyRemoved(NodeMode defaultMode)
  {
    return defaultMode.equals(NodeMode.HIDE_HIGH);
  }

  /**
   * Returns true if passed mask object is null or it's type is supported mask type:
   * either Integer or DataMap.
   *
   */
  private static boolean isValidMaskType(Object mask)
  {
    return mask == null || mask.getClass() == Integer.class || mask.getClass() == DataMap.class;
  }

  /**
   * Returns mask <code>{ "$*": v }</code>, where <code>v</code> is passed Integer.
   *
   */
  private static DataMap wildcard(Integer v)
  {
    final DataMap wildcardMap = new DataMap();
    wildcardMap.put(FilterConstants.WILDCARD, v);
    return wildcardMap;
  }

  /**
   * Returns a NodeMode for a child with given name only if it was explicitly specified.
   * Otherwise it returns null.
   *
   * @param opNode node containing filter
   * @param name name of the filed
   * @return NodeMode for a child with given name if it was explicitly specified or null
   *         if NodeMode was not explicitly specified
   */
  private static NodeMode getExplicitNodeMode(DataMap opNode, String name)
  {
    // preconditions:
    // mask, if exist is of correct type
    assert opNode != null;

    final Object childModeObj = opNode.get(Escaper.replaceAll(name, "$", "$$"));
    if (childModeObj != null && !(childModeObj.getClass() == DataMap.class))
    {
      if (childModeObj instanceof Integer)
        return NodeMode.fromRepresentation((Integer) childModeObj);
    }
    return null;
  }

  /**
   * Filter DataList type consists of two steps. First, range filter is applied if it
   * exists. Second step is application of wildcard filter to all remaining elements.
   *
   */
  private Object filterDataList(DataMap opNode, DataList valueDataList)
  {
    assert opNode != null;

    final Integer start = getIntegerWithDefaultValue(opNode, START, 0);
    if (start == null || start < 0)
    {
      onError(null, "value of %1$s must be positive integer but is equal to %2$d", START, start);
    }

    final Integer count = getIntegerWithDefaultValue(opNode, COUNT, Integer.MAX_VALUE);
    if (count == null || count < 0)
    {
      onError(null, "value of %1$s must be positive integer but is equal to %2$d", COUNT, count);
    }

    if (start != null && start >= 0 && count != null && count >= 0)
    {
      final Object operation = filterByWildcard(opNode, valueDataList);
      return onFilterDataList(valueDataList, start, count, operation);
    }

    return null;
  }

  /**
   * The following cases are possible:
   * <ul>
   * <li>wildcard is null or is equal to 1, in such case there is nothing to do</li>
   * <li>wildcard is equal to 0, in this case all element are removed from an array</li>
   * <li>wildcard mask is a complex object; in such case new instruction is created for
   * every element in the array</li>
   * <li>filter is incorrect in other any case</li>
   * </ul>
   *
   */
  private Object filterByWildcard(DataMap opNode, DataList valueDataList)
  {
    final Object wildcard = opNode.get(FilterConstants.WILDCARD);
    if (wildcard != null)
    {
      if (wildcard.equals(NEGATIVE))
      {
        return FilterConstants.NEGATIVE;
      }
      else if (wildcard.getClass() == DataMap.class)
      {
        for (int i = 0; i < valueDataList.size(); ++i)
        {
          final Object elem = valueDataList.get(i);

          // if it is not complex, then it is an error, because for simple types filter
          // can be only 0 or 1
          // and at this stage we know that filter is complex
          if (!(elem instanceof DataComplex))
          {
            onError(i,
                    "complex filter defined for array element, which is not an object nor an array, " +
                        "but it is of type: %1$s, with value: %2$s",
                    elem.getClass().getName(),
                    elem);
          }
        }
        return wildcard;
      }
      else if (!wildcard.equals(POSITIVE))
      {
        // _wildcard can be either 0, DataMap or 1,
        // otherwise it is incorrect
        onError(null,
                "wildcard can be either 0, 1 or DataMap instance, but it is of type: %1$s, equal to: %2$s",
                wildcard.getClass().getName(),
                wildcard);
      }
      // if _wildcard is 1, then there is no filtering
    }
    // else, if wildcard == null, it means there is no filtering and array should not be
    // changed
    return FilterConstants.POSITIVE;
  }

  private Object filterDataMap(DataMap opNode,
                               DataMap valueDataMap,
                               NodeMode defaultMode,
                               DataMap complexWildCard)
  {
    assert opNode != null;

    final Map<String, Object> result = new HashMap<String, Object>();
    for (Map.Entry<String, Object> entry : valueDataMap.entrySet())
    {
      final String name = entry.getKey();
      final Object childValue = entry.getValue();

      // make sure that mask is of correct type if it is defined
      if (!isValidMaskType(opNode.get(Escaper.replaceAll(name, "$", "$$"))))
      {
        onError(name,
                "mask value for field %2$s should be of type Integer or DataMap, instead it is of type: %1$s, ",
                opNode.get(Escaper.replaceAll(name, "$", "$$")),
                name);
        // in not fast-fail mode just skip this entry
        continue;
      }

      Object operation = FilterConstants.POSITIVE;

      // _explicitFieldMode can only have value of high priority: either
      // show_high or hide_high
      final NodeMode explicitFieldMode = getExplicitNodeMode(opNode, name);
      if (explicitFieldMode != null)
      {
        if (areFieldsExplicitlyRemoved(explicitFieldMode))
        {
          // if item was explicitly hidden, filter it out with all descendants
          operation = FilterConstants.NEGATIVE;
        }
        else if (complexWildCard != null)
        // apply complex wildcard if it was specified
        // there is no need for further evaluation of mask, because this field
        // was explicitly masked with positive mask
        {
          if (childValue instanceof DataComplex)
          {
            final DataMap composed = compose(name, complexWildCard, wildcard(1));
            if (composed != null)
            {
              operation = composed;
            }
          }
          // else
          // data is of primitive type, and is selected with mask = 1, but there also
          // exist
          // a wildcard mask, in this case we don't report it as an error
        }
      }
      else
      {
        // field was not explicitly masked

        final Object opChild = opNode.get(Escaper.replaceAll(name, "$", "$$"));

        // if item was not explicitly excluded nor included

        if (opChild == null)
        {
          // 1. there was no filter for this item - in this case apply default filter
          // and $* if it was defined and field was not filtered out
          if (areFieldsExplicitlyRemoved(defaultMode)
              || areFieldsImplicitlyRemoved(defaultMode, complexWildCard))
          {
            operation = FilterConstants.NEGATIVE;
          }
          else if (complexWildCard != null)
          {
            if (childValue instanceof DataComplex)
            {
              operation = complexWildCard;
            }
            else if (needsRemoving(defaultMode, complexWildCard))
            {
              operation = FilterConstants.NEGATIVE;
            }
          }
        }
        else
        {
          // precondition:
          assert (opChild.getClass() == DataMap.class) : opChild;

          final Object rawWildcard = opNode.get(FilterConstants.WILDCARD);
          final DataMap effectiveComplexWildcard =
              ((rawWildcard != null && rawWildcard.equals(POSITIVE)) ? wildcard(POSITIVE)
                  : (DataMap) rawWildcard);
          // effectiveMask contains complex mask composed with wildcard if wildcard is
          // defined
          final DataMap effectiveMask =
              ((effectiveComplexWildcard == null) ? (DataMap) opChild
                  : compose(name, (DataMap) opChild, effectiveComplexWildcard));

          // 2. filter was complex
          if (needsRemoving(defaultMode, effectiveMask))
          {
            operation = FilterConstants.NEGATIVE;
          }
          else
          {
            if (childValue instanceof DataComplex)
            {
              operation = effectiveMask;
            }
            else
            {
              onError(name, "data is of primitve value: %1$s, but filter: %2$s is complex",
                      childValue,
                      opChild);
            }
          }
        }
      }

      if (isValidDataMapFieldOperation(result, name, operation))
      {
        result.put(name, operation);
      }
    }

    return onFilterDataMap(valueDataMap, result);
  }

  /**
   * Field needs to be removed if it was explicitly filtered out with 0 or if default mode
   * is hide_low and _effectiveMask does not contain any positive mask, which can only
   * happen if it's default mode is show_low or hide_high
   *
   */
  private boolean needsRemoving(NodeMode defaultMode, DataMap effectiveMask)
  {
    return areFieldsExplicitlyRemoved(defaultMode)
        || (defaultMode.equals(NodeMode.HIDE_LOW) && (_dafaultNodeModeCalculator.getDefaultNodeMode(effectiveMask)
        .equals(NodeMode.SHOW_LOW) || _dafaultNodeModeCalculator.getDefaultNodeMode(effectiveMask)
        .equals(NodeMode.HIDE_HIGH)));
  }

  /**
   * Returns true if default mode for fields in the node is hide_low and it is not
   * overwritten by wildcard mask. If wildcard mask is not only negative, then fields can
   * not be removed by default.
   *
   */
  private boolean areFieldsImplicitlyRemoved(NodeMode defaultMode, DataMap complexWildCard)
  {
    return defaultMode.equals(NodeMode.HIDE_LOW)
        && (complexWildCard == null || !_dafaultNodeModeCalculator.getDefaultNodeMode(complexWildCard)
        .equals(NodeMode.HIDE_LOW));
  }

  /**
   * Returns composition of two masks. This method does not modify any of the parameters
   * and returned mask is new object. Composition is achieved by invoking separate data
   * processing. If there was an error during mask composition, then null is returned.
   *
   * @param mask1 first mask
   * @param mask2 second mask
   * @return composed masks or null if there was an error during composition fast-fail
   *         mode
   */
  private DataMap compose(String fieldName, DataMap mask1, DataMap mask2)
  {
    // precondition:
    assert mask2 != null;
    assert mask1 != null;

    try
    {
      final DataMap clone = mask1.copy();
      new DataComplexProcessor(new MaskComposition(), mask2, clone).run(true);
      return clone;
    }
    catch (CloneNotSupportedException e)
    {
      onError(fieldName, "could not clone mask: %1$s, exception: %2$s", mask1, e);
    }
    catch (DataProcessingException e)
    {
      onError(fieldName, "error composing mask %1$s with %2$s, exception: %3$s", mask1, mask2, e);
    }
    return null;
  }

  private final DefaultNodeModeCalculator _dafaultNodeModeCalculator = new DefaultNodeModeCalculator();
}
