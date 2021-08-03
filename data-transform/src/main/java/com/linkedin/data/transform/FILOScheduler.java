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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * InstructionScheduler implementation, which executes instructions in FILO order. Can be
 * used to implement DFS tree traversal strategy.
 *
 * @author jodzga
 *
 */
public class FILOScheduler implements InstructionScheduler
{

  private final Deque<Instruction> _stack;

  /**
   * Initialize a new {@link FILOScheduler}.
   */
  public FILOScheduler()
  {
    _stack = new ArrayDeque<>();
  }

  public void scheduleInstructions(List<Instruction> instructions)
  {
    _stack.addAll(instructions);  /* adds to last */
  }

  public boolean hasNext()
  {
    return !_stack.isEmpty();
  }

  public Instruction next()
  {
    return _stack.removeLast();
  }

  @Override
  public void scheduleInstruction(Instruction instruction)
  {
    _stack.add(instruction);
  }

}
