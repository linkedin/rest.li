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
import com.linkedin.data.DataMap;
import com.linkedin.data.message.Message;
import com.linkedin.data.message.MessageList;

/**
 * DataMapProcessor abstracts DataMap processing when it can be described as an
 * object with layout similar to data object it operates on with additional information
 * how data should be modified. Examples of data processing that fit this description are:
 * patch, projections.
 *
 * @author jodzga
 *
 */
public class DataComplexProcessor
{
  public static final String DATA_PROCESSING_FAILED = "data processing failed";

  private final InstructionScheduler         _instructionScheduler;
  private final Interpreter                  _interpreter;
  private final DataMap                      _program;
  private final DataComplex                  _data;
  private static final ImmutableList<Object> _rootPath              = ImmutableList.empty();

  /**
   * Creates new DataMapProcessor.
   */
  public DataComplexProcessor(final InstructionScheduler instructionScheduler, final Interpreter interpreter, final DataMap program, final DataComplex data)
  {
    this._instructionScheduler = instructionScheduler;
    this._interpreter = interpreter;
    this._program = program;
    this._data = data;
  }

  /**
   * Constructor which uses default (FILO) InstructionScheduler.
   *
   * @param interpreter the interpreter
   * @param program the program
   * @param data the data
   */
  public DataComplexProcessor(final Interpreter interpreter, final DataMap program, final DataComplex data)
  {
    this(new FILOScheduler(), interpreter, program, data);
  }

  /**
   * Runs data processing and throws {@link DataProcessingException} if there were any errors.
   * <p>If Interpreter was created with fast-fail flag, then processing is stopped
   * immediately after occurrence of first error. Otherwise, if possible, data processing is continued
   * and all errors are gathered and accessible through DataProcessingException thrown
   * by this method.
   *
   * @param fastFail if true, stop immediately after the first error,
   *                 otherwise gather as many errors as possible.
   * @throws DataProcessingException
   * @return information messages from the interpreter context.
   */
  public MessageList<Message> runDataProcessing(boolean fastFail) throws DataProcessingException
  {
    if (_program != null)
    {

      InterpreterContext ic = new InterpreterContext(fastFail, _instructionScheduler);

      // create first instruction and schedule it for execution
      final Instruction firstInstruction = new Instruction(_program, _data, _rootPath);
      _instructionScheduler.scheduleInstruction(firstInstruction);

      // main loop
      while (_instructionScheduler.hasNext())
      {
        final Instruction next = _instructionScheduler.next();
        try
        {
          ic.setCurrentInstruction(next);
          _interpreter.interpret(ic);
        }
        catch (FastFailException ff)
        {
          //in case of fast fail exception, the behavior is to immediately stop processing,
          //don't generate child instructions, exception containing details is thrown below
          throw new DataProcessingException(DATA_PROCESSING_FAILED,
                                            ic.getErrorMessages());
        }
      }

      //check for errors and throw exception if processing failed
      if (ic.failed())
        throw new DataProcessingException(DATA_PROCESSING_FAILED,
                                          ic.getErrorMessages());

      return ic.getInfoMessages();
    }
    return null;
  }

  /**
   * Same as {@link #runDataProcessing(boolean)}, but returns void.
   */
  public void run(boolean fastFail) throws DataProcessingException
  {
    runDataProcessing(fastFail);
  }
}
