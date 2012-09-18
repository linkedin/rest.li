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

import static com.linkedin.data.transform.filter.FilterConstants.COUNT;
import static com.linkedin.data.transform.filter.FilterConstants.NEGATIVE;
import static com.linkedin.data.transform.filter.FilterConstants.POSITIVE;
import static com.linkedin.data.transform.filter.FilterConstants.START;
import static com.linkedin.data.transform.filter.FilterUtil.getIntegerWithDefaultValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.linkedin.data.DataComplex;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.transform.DataMapProcessor;
import com.linkedin.data.transform.DataProcessingException;
import com.linkedin.data.transform.Escaper;
import com.linkedin.data.transform.Instruction;
import com.linkedin.data.transform.Interpreter;
import com.linkedin.data.transform.InterpreterContext;

/**
 * Interpreter, which implements data filtering. Instruction contains data to be filtered
 * and equivalent node in filter, which specifies how data is supposed to be filtered.
 * <p>
 * Data object being filtered is modified by this Interpreter, which means that it must be
 * mutable.
 *
 * @author jodzga
 *
 */
public class Filter implements Interpreter
{

  private final DefaultNodeModeCalculator _dafaultNodeMode = new DefaultNodeModeCalculator();

  private InterpreterContext              _instrCtx;

  @Override
  public void interpret(InterpreterContext instrCtx)
  {

    _instrCtx = instrCtx;

    Instruction instruction = _instrCtx.getCurrentInstruction();

    Object dataValue = instruction.getData();
    DataMap opNode = getOperation(instruction);

    if ((dataValue != null) && (opNode != null))
    {

      // _defaultMode specifies if field should be filtered out by default
      NodeMode defaultMode = _dafaultNodeMode.getDefaultNodeMode(opNode);

      // get complex wildcard if it exist e.g. $*: {...}
      DataMap complexWildCard = getComplexWildCard(opNode);

      if (dataValue.getClass() == DataList.class)
      {
        filterDataList(opNode, ((DataList) dataValue), defaultMode, complexWildCard);

      }
      else if (dataValue.getClass() == DataMap.class)
      {
        filterDataMap(opNode, ((DataMap) dataValue), defaultMode, complexWildCard);
      }
      else
      {
        // there is instruction on complex data type - this means that filter
        // is incompatible with data, because all simple data should have been
        // filtered out by parent instruction
        _instrCtx.addErrorMessage("data type in instruction must be DataMap or DataList, but is: %1$s",
                                  dataValue.getClass().getName());
      }
    }

  }

  /**
   * Returns complex wildcard mask or null if wildcard is not defined or is of simple type
   *
   */
  private DataMap getComplexWildCard(DataMap opNode)
  {
    assert opNode != null;

    Object o = opNode.get(FilterConstants.WILDCARD);
    if ((o != null) && (o.getClass() == DataMap.class))
      return (DataMap) o;
    else
      return null;
  }

  /**
   * Filter DataList type consists of two steps. First, range filter is applied if it
   * exists. Second step is application of wildcard filter to all remaining elements.
   *
   */
  private void filterDataList(DataMap opNode,
                                           DataList valueDataList,
                                           NodeMode defaultMode,
                                           DataMap _complexWildCard)
  {
    assert opNode != null;

    filterByRange(opNode, valueDataList);

    filterByWildcard(opNode, valueDataList);
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
  private void filterByWildcard(DataMap opNode, DataList valueDataList)
  {
    final Object wildcard = opNode.get(FilterConstants.WILDCARD);
    _instrCtx.setCurrentField(FilterConstants.WILDCARD);
    if (wildcard != null)
    {
      if (wildcard.equals(NEGATIVE))
        valueDataList.clear();
      else if (wildcard.getClass() == DataMap.class)
      {
        int index = 0;
        for (Object elem : valueDataList)
        {
          _instrCtx.setCurrentField(Integer.valueOf(index));
          // if it is not complex, then it is an error, because for simple types filter
          // can be only 0 or 1
          // and at this stage we know that filter is complex
          if (!(elem instanceof DataComplex))
            _instrCtx.addErrorMessage("complex filter defined for array element, which is not an object nor an array, "
                                          + "but it is of type: %1$s, with value: %2$s",
                                      elem.getClass().getName(),
                                      elem.toString());
          else
          {
            Instruction childInstruction =
                createInstruction((DataComplex) elem, wildcard);
            if (childInstruction != null)
              _instrCtx.scheduleInstruction(childInstruction);
          }
          index++;
        }

      }
      else if (!wildcard.equals(POSITIVE)) // _wildcard can be either 0, DataMap or 1,
                                           // otherwise it is incorrect
        _instrCtx.addErrorMessage("wildcard can be either 0, 1 or DataMap instance, but it is of type: %1$s, equal to: %2$s",
                                  wildcard.getClass().getName(),
                                  wildcard.toString());
      // if _wildcard is 1, then there is no filtering
    }
    // else, if wildcard == null, it means there is no filtering and array should not be
    // changed
  }

  /**
   * Filters content of an array using $start and $count if they are specified in the
   * mask.
   *
   */
  private void filterByRange(final DataMap op, final DataList valueDataList)
  {
    final Integer start = getIntegerWithDefaultValue(op, START, 0);
    if (start == null || start < 0)
      _instrCtx.addErrorMessage("value of %1$s must be positive integer but is equal to %2$d",
                                START,
                                start);
    final Integer count = getIntegerWithDefaultValue(op, COUNT, Integer.MAX_VALUE);
    if (count == null || count < 0)
      _instrCtx.addErrorMessage("value of %1$s must be positive integer but is equal to %2$d",
                                COUNT,
                                count);
    if (start != null && start >= 0 && count != null && count >= 0)
    {
      // trimming beginning of array can change indexes, so first the beginning of array
      // needs to be trimmed
      trimEndOfArray(valueDataList, start, count);
      trimBeginOfArray(valueDataList, start);
    }
  }

  private void trimEndOfArray(DataList valueDataList, Integer start, Integer count)
  {
    if (start + count < valueDataList.size())
      valueDataList.removeRange(start + count, valueDataList.size());

  }

  private void trimBeginOfArray(DataList valueDataList, Integer start)
  {
    if (start > 0)
      if (start >= valueDataList.size())
        valueDataList.clear();
      else
        valueDataList.removeRange(0, start);
  }

  private void filterDataMap(DataMap opNode,
                                          DataMap valueDataMap,
                                          NodeMode defaultMode,
                                          DataMap complexWildCard)
  {
    assert opNode != null;

    List<String> toBeRemoved = new ArrayList<String>();

    for (Entry<String, Object> entry : valueDataMap.entrySet())
    {
      String name = entry.getKey();
      _instrCtx.setCurrentField(name);
      Object childValue = entry.getValue();

      // make sure that mask is of correct type if it is defined
      if (!isValidMaskType(opNode.get(Escaper.replaceAll(name, "$", "$$"))))
      {
        _instrCtx.addErrorMessage("mask value for field %2$s should be of type Integer or DataMap, instead it is of type: %1$s, ",
                                  opNode.get(Escaper.replaceAll(name, "$", "$$")),
                                  name);
        // in not fast-fail mode just skip this entry
        continue;
      }

      // _explicitFieldMode can only have value of high priority: either
      // show_high or hide_high
      NodeMode explicitFieldMode = getExplicitNodeMode(opNode, name);

      if (explicitFieldMode != null)
      {
        if (areFieldsExplicitlyRemoved(explicitFieldMode))
        {
          // if item was explicitly hidden, filter it out with all descendants
          toBeRemoved.add(name);
        }
        else if (complexWildCard != null)
        // apply complex wildcard if it was specified
        // there is no need for further evaluation of mask, because this field
        // was explicitly masked with positive mask
        {
          if (childValue instanceof DataComplex)
          {
            DataMap composed = compose(complexWildCard, wildcard(1));
            if (composed != null)
            {
              Instruction childInstruction =
                  createInstruction((DataComplex) childValue, composed);
              if (childInstruction != null)
                _instrCtx.scheduleInstruction(childInstruction);
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

        Object opChild = opNode.get(Escaper.replaceAll(name, "$", "$$"));

        // if item was not explicitly excluded nor included

        if (opChild == null)
        {
          // 1. there was no filter for this item - in this case apply default filter
          // and $* if it was defined and field was not filtered out
          if (areFieldsExplicitlyRemoved(defaultMode)
              || areFieldsImplicitlyRemoved(defaultMode, complexWildCard))
          {
            toBeRemoved.add(name);
          }
          else if (complexWildCard != null)
          {
            if (childValue instanceof DataComplex)
            {
              Instruction childInstruction =
                  createInstruction((DataComplex) childValue, complexWildCard);
              if (childInstruction != null)
                _instrCtx.scheduleInstruction(childInstruction);
            }
            else
            {
              if (needsRemoving(defaultMode, complexWildCard))
                toBeRemoved.add(name);
            }
          }
        }
        else
        {
          // precondition:
          assert opChild != null && (opChild.getClass() == DataMap.class) : opChild;

          Object rawWildcard = opNode.get(FilterConstants.WILDCARD);
          DataMap effectiveComplexWildcard =
              ((rawWildcard != null && rawWildcard.equals(POSITIVE)) ? wildcard(POSITIVE)
                  : (DataMap) rawWildcard);
          // effectiveMask contains complex mask composed with wildcard if wildcard is
          // defined
          DataMap effectiveMask =
              ((effectiveComplexWildcard == null) ? (DataMap) opChild
                  : compose((DataMap) opChild, effectiveComplexWildcard));

          // 2. filter was complex
          if (needsRemoving(defaultMode, effectiveMask))
            toBeRemoved.add(name);
          else
          {
            if (childValue instanceof DataComplex)
            {
              Instruction childInstruction =
                  createInstruction((DataComplex) childValue, effectiveMask);
              if (childInstruction != null)
                _instrCtx.scheduleInstruction(childInstruction);
            }
            else
              _instrCtx.addErrorMessage("data is of primitve value: %1$s, but filter: %2$s is complex",
                                        childValue.toString(),
                                        opChild.toString());
          }
        }
      }

    }

    // remove fields that need to be filtered out
    for (String _beingRemoved : toBeRemoved)
      valueDataMap.remove(_beingRemoved);

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
        || (defaultMode.equals(NodeMode.HIDE_LOW) && (_dafaultNodeMode.getDefaultNodeMode(effectiveMask)
                                                                      .equals(NodeMode.SHOW_LOW) || _dafaultNodeMode.getDefaultNodeMode(effectiveMask)
                                                                                                                    .equals(NodeMode.HIDE_HIGH)));
  }

  /**
   * Returns true if node contained $*=0, which explicitly removes all fields
   *
   */
  private boolean areFieldsExplicitlyRemoved(NodeMode defaultMode)
  {
    return defaultMode.equals(NodeMode.HIDE_HIGH);
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
        && (complexWildCard == null || !_dafaultNodeMode.getDefaultNodeMode(complexWildCard)
                                                        .equals(NodeMode.HIDE_LOW));
  }

  /**
   * Returns true if passed mask object is null or it's type is supported mask type:
   * either Integer or DataMap.
   *
   */
  private boolean isValidMaskType(Object mask)
  {
    if (mask != null && !(mask instanceof Integer) && !(mask.getClass() == DataMap.class))
      return false;
    return true;
  }

  /**
   * Returns mask <code>{ "$*": v }</code>, where <code>v</code> is passed Integer.
   *
   */
  private DataMap wildcard(Integer v)
  {
    DataMap c1 = new DataMap();
    c1.put(FilterConstants.WILDCARD, v);
    return c1;
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
  private DataMap compose(DataMap mask1, DataMap mask2)
  {
    // precondition:
    assert mask2 != null;
    assert mask1 != null;

    try
    {
      final DataMap clone = mask1.copy();
      new DataMapProcessor(new MaskComposition(), mask2, clone).run(_instrCtx.isFastFail());
      return clone;
    }
    catch (CloneNotSupportedException e)
    {
      _instrCtx.addErrorMessage("could not clone mask: %1$s, exception: %2$s", mask1, e);
    }
    catch (DataProcessingException e)
    {
      _instrCtx.addErrorMessage("error composing mask %1$s with %2$s, exception: %3$s",
                                mask1,
                                mask2,
                                e);
    }
    return null;
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
  private NodeMode getExplicitNodeMode(DataMap opNode, String name)
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
   * Returns operation from an instruction. If operation in instruction is of incorrect
   * type or is null, the null is returned.
   *
   * @param instruction instruction to get operation from
   * @return operation or null if instruction is corrupted
   */
  private DataMap getOperation(Instruction instruction)
  {
    Object opObj = instruction.getOperation();

    if (opObj != null)
    {
      if (opObj.getClass() == DataMap.class)
        return (DataMap) opObj;
      else
        _instrCtx.addErrorMessage("Instruction's operation must be instance of DataMap, "
            + "but it is instance of %1$s", opObj.getClass().getName());
    }
    else
      _instrCtx.addErrorMessage("Instruction's operation must not be null");

    // return null if there was an error
    return null;
  }

  /**
   * Creates and returns new Instruction for provided data and makes sure they are of
   * proper type. If provided data is not correct, then null is returned.
   *
   */
  private Instruction createInstruction(DataComplex childData, Object childOperation)
  {
    if (!(childOperation.getClass() == DataMap.class))
      _instrCtx.addErrorMessage("specified mask was not number 0, nor number 1, nor nested object {...}, it was: %1$s",
                                childOperation);
    else if (childData instanceof DataComplex)
      return new Instruction(childOperation, childData, _instrCtx.getPath());
    else
      _instrCtx.addErrorMessage("specified filter for complex type, but value in an object is of simple type: %1$s",
                                childData.getClass().getName());

    return null;
  }

}
