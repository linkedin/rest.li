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

import java.util.List;

/**
 * InstructionScheduler is responsible for organizing the order of execution of instructions. It allows
 * implementation of certain types of tree traversals e.g. DFS, BFS etc.
 *
 * @author jodzga
 *
 */
public interface InstructionScheduler
{

  /**
   * Adds instructions for further processing. Order, in which instructions will be interpreted depends
   * on the concrete implementation of InstructionScheduler.
   * @param instructions list of instructions to be scheduled
   */
  public void scheduleInstructions(List<Instruction> instructions);

  /**
   * Adds instruction for further processing. Order, in which instructions will be interpreted depends
   * on the concrete implementation of InstructionScheduler.
   * @param instruction instruction to be scheduled for execution
   */
  public void scheduleInstruction(Instruction instruction);

  /**
   * Returns true if there are more instructions to be processed.
   * @return true if there are any instructions scheduled for execution
   */
  public boolean hasNext();

  /**
   * Returns next instruction for processing.
   * @return next instruction to be processed
   */
  public Instruction next();

}
