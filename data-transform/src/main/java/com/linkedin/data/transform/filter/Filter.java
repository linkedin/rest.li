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


import com.linkedin.data.DataComplex;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.transform.Instruction;
import com.linkedin.data.transform.Interpreter;
import com.linkedin.data.transform.InterpreterContext;

import java.util.Map;

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
public class Filter extends AbstractFilter implements Interpreter
{
  private InterpreterContext _instrCtx;

  @Override
  public void interpret(InterpreterContext instrCtx)
  {
    _instrCtx = instrCtx;

    final Instruction instruction = _instrCtx.getCurrentInstruction();

    final Object dataValue = instruction.getData();
    final DataMap opNode = getOperation(instruction);

    filter(dataValue, opNode);
  }

  @Override
  protected Object onFilterDataList(DataList data, int start, int count, Object operation)
  {
    if (operation == FilterConstants.NEGATIVE)
    {
      data.clear();
    }
    else
    {
      if (start + count < data.size())
      {
        data.removeRange(start + count, data.size());
      }

      if (start > 0)
      {
        if (start >= data.size())
        {
          data.clear();
        }
        else
        {
          data.removeRange(0, start);
        }
      }

      if (operation.getClass() == DataMap.class)
      {
        for (int i = 0; i < data.size(); ++i)
        {
          final Object value = data.get(i);
          if (value instanceof DataComplex)
          {
            _instrCtx.setCurrentField(start + i);
            scheduleInstruction((DataMap) operation, (DataComplex) value);
          }
        }
      }
    }

    return data;
  }

  @Override
  protected Object onFilterDataMap(DataMap data, Map<String, Object> fieldToOperation)
  {
    for (Map.Entry<String, Object> operation : fieldToOperation.entrySet())
    {
      if (operation.getValue() == FilterConstants.NEGATIVE)
      {
        data.remove(operation.getKey());
      }
    }

    for (Map.Entry<String, Object> entry : data.entrySet())
    {
      final Object operation = fieldToOperation.get(entry.getKey());
      if (operation != null && operation.getClass() == DataMap.class)
      {
        _instrCtx.setCurrentField(entry.getKey());
        scheduleInstruction((DataMap) operation, (DataComplex) entry.getValue());
      }
    }

    return data;
  }

  @Override
  protected boolean isValidDataMapFieldOperation(Map<String, Object> result, String name, Object operation)
  {
    return operation != FilterConstants.POSITIVE;
  }

  protected Object onError(Object field, String format, Object... args)
  {
    if (field != null)
    {
      _instrCtx.setCurrentField(field);
    }

    _instrCtx.addErrorMessage(format, args);
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
   * Creates and schedules new Instruction for provided data and makes sure they are of
   * proper type. If provided data is not correct, error is added.
   */
  private void scheduleInstruction(DataMap childOperation, DataComplex childData)
  {
    _instrCtx.scheduleInstruction(new Instruction(childOperation, childData, _instrCtx.getPath()));
  }
}
