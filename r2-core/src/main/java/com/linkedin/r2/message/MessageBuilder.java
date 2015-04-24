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
 * An object that builds new messages (rest/rpc, request/response).<p/>
 *
 * {@link Message}s in the R2 system are immutable and are safe to use across threads. Builders
 * provide a way to build new Messages, either from scratch or by copying an existing message or
 * builder. Builders are not thread-safe, so they should be used only from one thread or proper
 * synchronization should be used. Alternatively, a message, which is immutable, can be used as a
 * template across threads - use {@link com.linkedin.r2.message.Message#builder()} to modify a
 * local copy.<p/>
 *
 * Builders are designed to be chainable. For example, the following is a possible use of a
 * RestBuilder:<p/>
 *
 * <pre>
 * final RestRequest request = new RestRequestBuilder(URI.create("test"))
            .setEntity(new byte[] {1,2,3,4})
            .setHeader("k1", "v1")
            .setMethod(RestMethod.PUT)
            .build()
 * </pre>
 *
 * @see com.linkedin.r2.message.rest.RestRequestBuilder
 * @see com.linkedin.r2.message.rest.RestResponseBuilder
 * @see com.linkedin.r2.message.rpc.RpcRequestBuilder
 * @see com.linkedin.r2.message.rpc.RpcResponseBuilder
 * @author Chris Pettitt
 * @version $Revision$
 */
public interface MessageBuilder<B extends MessageBuilder<B>>
{
  /**
   * Sets the entity for this message.<p/>
   *
   * Because {@link com.linkedin.data.ByteString}s are immutable, this does not result in a copy.
   *
   * @param entity the entity to set for this message
   *
   * @return this builder
   */
  B setEntity(ByteString entity);

  /**
   * Sets the entity for this message.<p/>
   *
   * This method copies the underlying byte[] to prevent accidental modification to the array.
   *
   * @param entity the entity to set for this message
   *
   * @return this builder
   */
  B setEntity(byte[] entity);

  /**
   * Returns the entity for this message.
   *
   * @return the entity for this message
   */
  ByteString getEntity();

  /**
   * Constructs an immutable {@link Message} using the settings configured in this builder.
   * Subsequent changes to this builder will not change the underlying message.
   *
   * @return a Message from the settings in this builder
   */
  Message build();

  /**
   * Similar to {@link #build}, but the returned Message is in canonical form.
   *
   * @return a Message from the settings in this builder.
   */
  Message buildCanonical();
}
