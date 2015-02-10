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
package com.linkedin.data.transform.patch;

import static com.linkedin.data.transform.patch.PatchConstants.COMMAND_PREFIX;
import static com.linkedin.data.transform.patch.PatchConstants.DELETE_COMMAND;
import static com.linkedin.data.transform.patch.PatchConstants.SET_COMMAND;

import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.transform.Instruction;
import com.linkedin.data.transform.Interpreter;
import com.linkedin.data.transform.InterpreterContext;

public class Patch implements Interpreter
{

  //constant used internally by the Patch to mark that field has been used
  //for further processing
  private static final String CHILD_PROCESS_PSEUDOCOMMAND = "$child-process";

  // list of operations that do data manipulation other than just removing data
  private static final List<String>         NON_DELETE_OPERATIONS =
                                                                      Arrays.asList(new String[] { SET_COMMAND });

  // used for memoization of types of operations down the tree, contains true if down the
  // operations tree are only $delete operations the reason for using IdentityHashMap is
  // that we know that each node is distinct object, they never repeat in a tree and we
  // want to avoid expensive hash calculations on maps and lists
  private IdentityHashMap<DataMap, Boolean> _hasDeletesOnly       =
                                                                      new IdentityHashMap<DataMap, Boolean>();

  // On $delete operations, log the path as an info message in the interpreter context
  private final boolean _logDeletes;

  public Patch()
  {
    this(false);
  }

  public Patch(boolean logDeletes)
  {
    _logDeletes = logDeletes;
  }

  /**
   * Interpret and execute the current instruction based on the
   * interpreter context.
   *
   * @param instrCtx the current interpreter context
   */
  public void interpret(final InterpreterContext instrCtx)
  {
    Instruction instruction = instrCtx.getCurrentInstruction();

    // preconditions:
    // operation's node is always DataMap
    assert instruction.getOperation().getClass() == DataMap.class;
    // data's node is always DataMap
    assert instruction.getData().getClass() == DataMap.class;

    //_usedFields variable is used to keep track of fields, which were already used
    //at this nodes. The reason for it is that if field should not be used in more than
    //one operation e.g. $set and $delete, because such patch becomes ambiguous.
    //Each operation, upon being executed updates this variable.
    final Map<String, String> usedFields = new HashMap<String, String>();

    DataMap opNode = (DataMap) instruction.getOperation();
    DataMap dataNode = (DataMap) instruction.getData();

    /**
     * Apply all supported operations here. _usedFields is used to keep track of fields
     * that operations were applied to.
     */
    executeSetCommand(opNode.get(SET_COMMAND), dataNode, usedFields, instrCtx);
    executeDeleteCommand(opNode.get(DELETE_COMMAND), dataNode, usedFields, instrCtx);

    // iterate over children
    for (Entry<String, Object> entry : opNode.entrySet())
      processChild(dataNode, entry.getKey(), entry.getValue(), usedFields, instrCtx);

  }

  private boolean processChild(DataMap dataNode,
                            String name,
                            Object opChild,
                            Map<String, String> usedFields,
                            final InterpreterContext instrCtx)
  {
    instrCtx.setCurrentField(name);
    // do not process reserved words
    if (!name.startsWith(COMMAND_PREFIX))
    {

      if (usedFields.containsKey(name))
      {
        instrCtx.addErrorMessage("field %1$s can not be used in both %2$s operation and " +
            "be a branch in Patch at the same time", name, usedFields.get(name));
        return false;
      }
      else if (opChild.getClass() == DataMap.class)
      {
        usedFields.put(name, CHILD_PROCESS_PSEUDOCOMMAND);
        DataMap opChildDataMap = (DataMap) opChild;
        Object dataChild = dataNode.get(name);
        if (dataChild == null)
        {

          // this is an optimization: if respective object does not exist in data
          // and if patch's branch contains only deletes operations, then it is
          // not necessary to create nodes on the data object and process that branch,
          // unless we have to log all delete operations
          if (!hasDeletesOnly(opChildDataMap) || _logDeletes)
          {

            // if patch does data manipulations other than deletes, then we need to
            // create respective branch in data object and continue processing patch
            // on that branch
            dataChild = new DataMap();
            dataNode.put(name, dataChild);
            instrCtx.scheduleInstruction(new Instruction(opChild, (DataMap)dataChild, instrCtx.getPath()));
          }
        }
        else
        {
          // equivalent object exists in data tree
          if (dataChild.getClass() == DataMap.class)
            // if it's of proper type, then create new instruction
            instrCtx.scheduleInstruction(new Instruction(opChild, (DataMap)dataChild, instrCtx.getPath()));
          else
            // incorrect type in data object - it means that patch is
            // incompatible with data
          {
            instrCtx.addErrorMessage("patch incopatible with data object, expected %1$s"
                + " field to be of type DataMap, but found: %2$s", name, dataChild.getClass().getName());
            return false;
          }
        }
      }
      else
      {
        instrCtx.addErrorMessage("incorrect wire format of patch, simple type values are "
            + "allowed only as children of commands; node name: %1$s, value: %2$s", name, opChild);
        return false;
      }
    }
    return true;
  }

  private boolean executeDeleteCommand(Object deleteCommand, Object data, Map<String, String> usedFields, final InterpreterContext instrCtx)
  {
    instrCtx.setCurrentField(DELETE_COMMAND);
    if (deleteCommand != null)
    {
      // preconditions:
      // deleteCommand value is of type DataList
      assert deleteCommand.getClass() == DataList.class;
      // data is of type DataMap
      assert data.getClass() == DataMap.class;

      DataList delDataList = (DataList) deleteCommand;
      DataMap dataDataMap = (DataMap) data;

      for (Object key : delDataList)
      {
        if (usedFields.containsKey(key))
        {
          instrCtx.addErrorMessage("field %1$s can not be used in both %2$s operation and " +
          		DELETE_COMMAND + " operation at the same time", key, usedFields.get(key));
          return false;
        }
        else
        {
          usedFields.put(key.toString(), DELETE_COMMAND);
          dataDataMap.remove(key);
          if (_logDeletes)
          {
            instrCtx.addInfoMessage(key.toString());
          }
        }
      }
    }
    return true;
  }

  /**
   * Executes $set command and returns true is it was successful and false otherwise.
   */
  private boolean executeSetCommand(Object setCommand, Object data, Map<String, String> usedFields, final InterpreterContext instrCtx)
  {
    instrCtx.setCurrentField(SET_COMMAND);
    if (setCommand != null)
    {
      // input invariants
      // deleteCommand value is of DataMap type
      assert setCommand.getClass() == DataMap.class : setCommand.getClass();
      // data is of DataMap type
      assert data.getClass() == DataMap.class : data.getClass();

      DataMap setDataMap = (DataMap) setCommand;
      DataMap dataDataMap = (DataMap) data;

      for (Entry<String, Object> entry : setDataMap.entrySet())
      {
        String key = entry.getKey();
        if (usedFields.containsKey(key))
        {
          instrCtx.addErrorMessage("field %1$s can not be used in both %2$s operation and " +
              SET_COMMAND + " operation at the same time", key, usedFields.get(key));
          return false;
        }
        else
        {
          usedFields.put(key.toString(), SET_COMMAND);
          dataDataMap.put(key, entry.getValue());
        }
      }
    }
    return true;
  }

  /**
   * Checks whether patch rooted in given node contains data manipulations other
   * than deletes. It is used for optimization e.g. if patch contains only deletes and
   * respective branch does not exist in the data object, then creation of that branch in
   * data object and further processing can be skipped. This method uses memoization to
   * achieve amortized constant time per operation.
   *
   * @param opNode
   *          node, whose contents will be inspected to check if they contain only delete
   *          operations
   * @return true if node (and it's subnodes) contain only delete operations
   */
  private boolean hasDeletesOnly(DataMap opNode)
  {
    Boolean hdo = _hasDeletesOnly.get(opNode);
    if (hdo == null)
    {
      // if value has not been computed yet
      // patch has deletes only unless:
      // - current node contains non delete operation
      Iterator<String> it = NON_DELETE_OPERATIONS.iterator();
      while (hdo == null && it.hasNext())
      {
        if (opNode.containsKey(it.next()))
          hdo = false;
      }
      // - one of children contains non delete operation
      Iterator<Object> nodeIt = opNode.values().iterator();
      while (hdo == null && nodeIt.hasNext())
      {
        Object child = nodeIt.next();
        if (child.getClass() == DataMap.class)
          if (!hasDeletesOnly((DataMap) child))
            hdo = false;
      }
      // if neither of previous conditions holds, then
      // patch contains only delete operations
      if (hdo == null)
        hdo = true;
      // memorize this value
      _hasDeletesOnly.put(opNode, hdo);
    }
    return hdo;
  }

}
