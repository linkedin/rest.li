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
 * An object that represents a message, either REST or RPC, and either request or response.<p/>
 *
 * Messages are immutable and thread-safe. It is possible to clone an existing Message, modify
 * details in the copy, and create a new Message instance that has the concrete type of the original
 * message (REST/RPC, request/response) using the {@link #builder()} method.
 *
 * @see com.linkedin.r2.message.rest.RestRequest
 * @see com.linkedin.r2.message.rest.RestResponse
 * @author Chris Pettitt
 * @version $Revision$
 */
public interface Message
{
  /**
   * Returns the entity for this message.
   *
   * @return the entity for this message
   */
  ByteString getEntity();

  /**
   * Returns a {@link MessageBuilder}, which provides a means of constructing a new message using
   * this message as a starting point. Changes made with the builder are not reflected by this
   * message instance.
   *
   * @return a builder for this message
   */
  MessageBuilder<? extends MessageBuilder<?>> builder();
}
