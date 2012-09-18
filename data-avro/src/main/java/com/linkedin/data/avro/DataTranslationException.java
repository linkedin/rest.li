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

package com.linkedin.data.avro;


import com.linkedin.data.message.Message;
import java.io.IOException;
import java.util.Collections;
import java.util.List;


/**
 * Exception thrown if there is an error translating data values from
 * one representation to another, such as Pegasus {@link com.linkedin.data.DataMap}
 * to Avro {@link org.apache.avro.generic.GenericRecord}.
 */
public class DataTranslationException extends IOException
{
  private static final long serialVersionUID = 1L;

  /**
   * Initialize a {@link DataTranslationException}.
   *
   * @param message provides a message.
   */
  public DataTranslationException(String message)
  {
    super(message);
    _messageList = Collections.emptyList();
  }

  /**
   * Initialize a {@link DataTranslationException}.
   *
   * @param message provides a message.
   * @param e provides the cause.
   */
  public DataTranslationException(String message, Throwable e)
  {
    super(message, e);
    _messageList = Collections.emptyList();
  }

  /**
   * Initialize a {@link DataTranslationException}.
   *
   * @param message provides a message.
   * @param messageList provides a {@link List} of {@link Message}s
   */
  public DataTranslationException(String message, List<Message> messageList)
  {
    super(message);
    _messageList = Collections.unmodifiableList(messageList);
  }

  /**
   * Initialize a {@link DataTranslationException}.
   *
   * @param message provides a message.
   * @param messageList provides a {@link List} of {@link Message}s
   * @param e provides the  cause
   */
  public DataTranslationException(String message, List<Message> messageList, Throwable e)
  {
    super(message, e);
    _messageList = Collections.unmodifiableList(messageList);
  }

  /**
   * Return the content of both message and message list (if the list is not empty.)
   *
   * Override of this method enables the messages in message list to be
   * emitted when this exception's stack trace is printed using {@link #printStackTrace()}.
   *
   * @return the content of message and message list (if the list is not empty.)
   */
  @Override
  public String getMessage()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(super.getMessage());
    if (! _messageList.isEmpty())
    {
      sb.append("\n");
      String messages = _messageList.toString();
      int len = messages.length();
      if (messages.charAt(len - 1) == '\n')
      {
        len--;
      }
      sb.append(messages, 0, len);
    }
    return sb.toString();
  }

  /**
   * Return the {@link List} of {@link Message}s.
   *
   * @return the list of {@link Message}s.
   */
  public List<Message> getMessageList()
  {
    return _messageList;
  }

  private final List<Message> _messageList;
}
