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
import com.linkedin.util.ArgumentUtil;

/**
 * {@link MessageBuilder} subclass for a specific {@link Message} subclass.
 *
 * @author Chris Pettitt
 * @version $Revision$
 */
public abstract class BaseMessageBuilder<B extends BaseMessageBuilder<B>> implements MessageBuilder<B>
{
  private ByteString _entity;

  /**
   * Construct a new instance with an empty entity body.
   */
  public BaseMessageBuilder()
  {
    _entity = ByteString.empty();
  }

  /**
   * Construct a new instance by copying the entity body from the specified {@link Message}.
   *
   * @param message the {@link Message} from which the entity should be obtained.
   */
  public BaseMessageBuilder(Message message)
  {
    setEntity(message.getEntity());
  }

  @Override
  public B setEntity(ByteString entity)
  {
    ArgumentUtil.notNull(entity, "entity");

    _entity = entity;
    return thisBuilder();
  }

  @Override
  public B setEntity(byte[] entity)
  {
    ArgumentUtil.notNull(entity, "entity");

    _entity = ByteString.copy(entity);
    return thisBuilder();
  }

  @Override
  public ByteString getEntity()
  {
    return _entity;
  }

  @SuppressWarnings("unchecked")
  protected B thisBuilder()
  {
    return (B)this;
  }
}
