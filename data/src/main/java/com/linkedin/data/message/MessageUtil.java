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

package com.linkedin.data.message;

import java.io.IOException;
import java.util.Collection;
import java.util.Formatter;

/**
 * Utility functions that works with {@link Message}s.
 */
public class MessageUtil
{
  public static StringBuilder appendMessages(StringBuilder sb, Collection<Message> messages)
  {
    Formatter formatter = new Formatter(sb);
    Appendable appendable = formatter.out();
    for (Message message : messages)
    {
      try
      {
        message.format(formatter, Message.MESSAGE_FIELD_SEPARATOR);
        appendable.append('\n');
      }
      catch (IOException e)
      {
        throw new IllegalStateException(e);
      }
    }
    formatter.flush();
    formatter.close();
    return sb;
  }

  public static String messagesToString(Collection<Message> messages)
  {
    StringBuilder sb = new StringBuilder();
    return appendMessages(sb, messages).toString();
  }
}
