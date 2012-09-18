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
package com.linkedin.data.transform;

import com.linkedin.data.DataComplex;



/**
 * Encapsulates one step in data processing. Contains references to respective nodes in
 * operations tree and data tree.
 * <p>This class is not thread safe. DataMapProcessor makes sure that access to instances of
 * this class is coordinated.
 *
 * @author jodzga
 *
 */
public class Instruction
{
  private final Object                _operation;
  private final DataComplex           _data;
  private final ImmutableList<Object> _path;

  /**
   * Intialize an instruction.
   * <p>
   * The operation is a tree of relevant operations
   * to be performed on the data.  These operations may be things like masking
   * or patching, each applying to the equivalent node in data.
   *
   * @param operation a tree of operations to be performed on the data
   * @param data a JSON object in tree format
   * @param path the list of paths to be taken to complete the instruction
   *             (only needed for logging purposes)
   */
  public Instruction(Object operation, DataComplex data, ImmutableList<Object> path)
  {
    super();
    this._operation = operation;
    this._data = data;
    this._path = path;
  }

  /**
   * @return the operation
   */
  public Object getOperation()
  {
    return _operation;
  }

  /**
   * @return the data
   */
  public DataComplex getData()
  {
    return _data;
  }

  @Override
  public String toString()
  {
    return "Instruction [_operation=" + _operation + ", _data=" + _data + ", _path="
        + _path + "]";
  }

  /**
   * @return the instruction path
   */
  public ImmutableList<Object> getInstructionPath()
  {
    return _path;
  }

}
