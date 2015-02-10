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

import com.linkedin.data.message.Message;
import com.linkedin.data.message.MessageList;

/**
 * This class encapsulates context of execution
 *
 * @author jodzga
 *
 */
public class InterpreterContext
{

  //stores error messages
  private final MessageList<Message> _errorMessages = new MessageList<Message>();

  //stores informational messages
  private final MessageList<Message> _infoMessages = new MessageList<Message>();

  //current instruction is in every method that needs to add error message, because
  //instruction stores path; instead of passing instruction as parameter, it is accessible
  //on the interpreter level
  private Instruction _currentInstruction;

  //if true, processing will stop after first error
  //if false, processing will continue if possible even if error occurred
  private final boolean _fastFail;

  //holds either Integer equal to index in DataList, which is currently being processed
  //or String equal to name of field in DataMap, which is currently being processed
  private Object _currentField;

  private final InstructionScheduler _instructionScheduler;

  /**
   *
   * fastFail specifies if Interpreter should
   * work in fast-fail mode. In fast-fail mode data processing stops as soon as first error
   * occurs. If Interpreter is not in fast-fail mode, then, if possible, data processing is
   * continued, all errors are aggregated and are part of {@link DataProcessingException}
   *
   * @param fastFail if true then execution is terminated immediately after
   * first error, otherwise errors are accumulated
   * @param instructionScheduler the instruction scheduler
   */
  public InterpreterContext(boolean fastFail, InstructionScheduler instructionScheduler)
  {
    super();
    _fastFail = fastFail;
    _instructionScheduler = instructionScheduler;
  }

  public boolean isFastFail()
  {
    return _fastFail;
  }

  /**
   * Adds error message to the list of error messages and throws FastFail exception if interpreter
   * is in fast-fail mode.
   * @param format format of a message
   * @param args arguments for a message
   */
  public void addErrorMessage(final String format, final Object... args) throws FastFailException
  {
    _errorMessages.add(new Message(getPath().toArray(), format, args));
    if (isFastFail())
      throw new FastFailException();
  }

  /**
   * Adds info message to the list of info messages.
   *
   * @param format format of the message.
   * @param args arguments for the message.
   */
  public void addInfoMessage(final String format, final Object... args)
  {
    _infoMessages.add(new Message(getPath().toArray(), false, format, args));
  }

  /**
   * Returns true if interpretation of any instruction failed.
   * @return true if interpretation of any instruction failed
   */
  public boolean failed()
  {
    return !_errorMessages.isEmpty();
  }

  /**
   * Returns error messages that occurred during data processing.
   * @return list of error messages that occurred during data processing
   */
  public MessageList<Message> getErrorMessages()
  {
    return _errorMessages;
  }

  /**
   * Returns info messages that were added during data processing.
   * @return list of info messages.
   */
  public MessageList<Message> getInfoMessages()
  {
    return _infoMessages;
  }

  /**
   * @return the instruction the interpreter is currently on
   */
  public Instruction getCurrentInstruction()
  {
    return _currentInstruction;
  }

  /**
   * @param currentInstruction the instruction
   */
  public void setCurrentInstruction(Instruction currentInstruction)
  {
    _currentInstruction = currentInstruction;
  }

  /**
   * Returns path from the root to the currently processed node. It points to either DataMap or DataList.
   * @return path from the root to the currently processed node
   */
  public ImmutableList<Object> getPath()
  {
    if (_currentField == null)
      return _currentInstruction.getInstructionPath();
    else
      return _currentInstruction.getInstructionPath().append(_currentField);
  }

  /**
   * Returns current field being processed. It can be either Integer, representing index in an array
   * or a String equal to name of field in DataMap.
   * @return current field being processed
   */
  public Object getCurrentField()
  {
    return _currentField;
  }

  /**
   * @param field the field
   */
  public void setCurrentField(Object field)
  {
    _currentField = field;
  }

  /**
   * @param instruction the instruction to be scheduled
   */
  public void scheduleInstruction(Instruction instruction)
  {
    _instructionScheduler.scheduleInstruction(instruction);
  }

}
