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

import java.util.ArrayList;

/**
 * Extends {@code java.util.ArrayList<Message>} by overriding the {@link #toString()} method.
 */
public class MessageList extends ArrayList<Message>
{
  private static final long serialVersionUID = 1L;

  public StringBuilder appendTo(StringBuilder stringBuilder)
  {
    return MessageUtil.appendMessages(stringBuilder, this);
  }

  @Override
  public String toString()
  {
    return MessageUtil.messagesToString(this);
  }

  public boolean isError()
  {
    boolean error = false;
    for (Message m : this)
    {
      if (m.isError())
      {
        error = true;
        break;
      }
    }
    return error;
  }
}
