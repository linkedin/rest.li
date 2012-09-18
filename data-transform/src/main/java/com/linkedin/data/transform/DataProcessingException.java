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

import com.linkedin.data.message.MessageList;

/**
 * Data processing exception containing list of errors that occurred during processing. If
 * Interpreter was created with fast-fail flag, then data processing stops after first
 * error occurred.
 *
 * @author jodzga
 * @see Interpreter
 */
public class DataProcessingException extends Exception
{

  private static final long serialVersionUID = 1;
  private final MessageList _messages;
  private final String      _mainMessage;

  /**
   * Initialize a {@link DataProcessingException}.
   *
   * @param mainMessage main exception message
   * @param messages Array of {@link com.linkedin.data.message.Message Message}s
   *                 encountered during data processing
   */
  public DataProcessingException(String mainMessage, MessageList messages)
  {
    super();
    _mainMessage = mainMessage;
    _messages = messages;
  }

  @Override
  public String getMessage()
  {
    return _mainMessage + "\n" + _messages.toString();
  }

  /**
   * Returns list of error messages which occurred during data processing.
   *
   * @return list of error messages which occurred during data processing
   */
  public MessageList getMessages()
  {
    return _messages;
  }

}
