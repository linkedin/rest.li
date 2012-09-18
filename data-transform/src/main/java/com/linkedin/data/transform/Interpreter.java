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


/**
 * Interpreter is responsible for interpreting instructions, executing
 * appropriate actions and generating new instructions if necessary.
 * <p>
 * By default interpreter is initialized with fastFail set to true.
 *
 * @author jodzga
 * @see Instruction
 */
public interface Interpreter
{

  /**
   * Interpret instruction. Interpreter can not depend on order of execution of
   * returned instructions. New instructions can be scheduled using
   * {@link InterpreterContext#scheduleInstruction(Instruction)} method.
   *
   * @param ic InterpreterContext passed by DataMapProcessor
   * @throws FastFailException exception thrown and caught internally by framework to implement
   * fast-fail functionality: if fast-fail option is true, then on first error the FastFailException
   * is thrown
   */
  public void interpret(InterpreterContext ic) throws FastFailException;

}
