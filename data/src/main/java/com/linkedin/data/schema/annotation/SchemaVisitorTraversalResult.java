/*
   Copyright (c) 2019 LinkedIn Corp.

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
package com.linkedin.data.schema.annotation;

import com.linkedin.data.message.Message;
import com.linkedin.data.message.MessageList;
import com.linkedin.data.message.MessageUtil;
import com.linkedin.data.schema.DataSchema;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;


/**
 * The traversal result stores states of the traversal result for each visitor.
 * It should tell whether the traversal is successful and stores error messages if not
 *
 * There are two kinds of error messages
 * (1) An error message with {@link Message} type, it will be collected to the {@link Message} list and formatted and
 * outputted by the string builder.
 * (2) User can also directly add string literal messages and output them using the string builder.
 *
 * @see Message
 */
public class SchemaVisitorTraversalResult
{

  boolean _isTraversalSuccessful = true;
  MessageList<Message> _messages = new MessageList<>();
  StringBuilder _messageBuilder = new StringBuilder();
  /**
   * The {@link SchemaVisitor} should not mutate the original {@link DataSchema} that {@link DataSchemaRichContextTraverser} is traversing,
   * instead it needs to construct a new one if it needs to update the original schema.
   * This is useful if the new updated {@link DataSchema} is needed for later reuse.
   * If no update on the original schema is needed, this variable should remain null.
   */
  DataSchema _constructedSchema = null;

  public DataSchema getConstructedSchema()
  {
    return _constructedSchema;
  }

  public void setConstructedSchema(DataSchema constructedSchema)
  {
    _constructedSchema = constructedSchema;
  }

  /**
   * Return whether there are errors detected during the traversal.
   * @return boolean to tell whether the traversal is successful or not
   */
  public boolean isTraversalSuccessful()
  {
    return _isTraversalSuccessful;
  }

  /**
   * private method for setting whether the traversal is successful.
   *
   * @param traversalSuccessful the boolean value to represent whether the traversal is successful
   *
   * @see #isTraversalSuccessful()
   */
  private void setTraversalSuccessful(boolean traversalSuccessful)
  {
    _isTraversalSuccessful = traversalSuccessful;
  }

  /**
   * Getter for messages lists
   * @return collection of messages gather during traversal
   */
  public Collection<Message> getMessages()
  {
    return _messages;
  }

  /**
   * Setter for message lists
   * @param messages
   */
  public void setMessages(MessageList<Message> messages)
  {
    _messages = messages;
    if (messages != null && messages.size() > 0)
    {
      setTraversalSuccessful(false);
    }
  }

  /**
   * Add a message to the message list and the string builder
   * @param message
   */
  public void addMessage(Message message)
  {
    _messages.add(message);
    MessageUtil.appendMessages(getMessageBuilder(), Arrays.asList(message));
    setTraversalSuccessful(false);
  }

  /**
   * Add a {@link Message} to the message list using constructor of the {@link Message}
   * and also add to the string builder
   *
   * @param path path to show in the message
   * @param format format of the message to show
   * @param args args for the format string
   *
   * @see Message
   */
  public void addMessage(ArrayDeque<String> path, String format, Object... args)
  {
    Message msg = new Message(path.toArray(), format, args);
    addMessage(msg);
  }

  /**
   * Add multiple {@link Message}s to the message list and the string builder
   * These message added shows same path
   *
   * @param path path of the location where the messages are added
   * @param messages the message to add to the message list
   *
   * @see Message
   */
  public void addMessages(ArrayDeque<String> path, Collection<? extends Message> messages)
  {
    List<Message> msgs = messages.stream()
                                 .map(msg -> new Message(path.toArray(), ((Message) msg).toString()))
                                 .collect(Collectors.toList());
    _messages.addAll(msgs);
    MessageUtil.appendMessages(getMessageBuilder(), msgs);
    setTraversalSuccessful(false);
  }

  public StringBuilder getMessageBuilder()
  {
    return _messageBuilder;
  }

  /**
   * Output the string builder content as a string
   *
   * @return a string output by the string builder
   */
  public String formatToErrorMessage()
  {
    return getMessageBuilder().toString();
  }
}
