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

/* $Id$ */
package com.linkedin.r2.message;


import com.linkedin.data.ByteString;


/**
 * Abstract implementation of the {@link Message} interface.
 *
 * @author Chris Pettitt
 * @version $Revision$
 */
public abstract class BaseMessage implements Message
{
  private final ByteString _body;

  /**
   * Construct a new instance with the specified body (entity).
   *
   * @param body the {@link ByteString} body to be used as the entity for this message.
   */
  public BaseMessage(ByteString body)
  {
    assert body != null;
    _body = body;
  }

  @Override
  public ByteString getEntity()
  {
    return _body;
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o)
    {
      return true;
    }

    if (!(o instanceof BaseMessage))
    {
      return false;
    }

    BaseMessage that = (BaseMessage) o;
    return _body.equals(that._body);
  }

  @Override
  public int hashCode()
  {
    return _body.hashCode();
  }
}
