/*
   Copyright (c) 2020 LinkedIn Corp.

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

package com.linkedin.r2.transport.http.client;

import com.linkedin.r2.RetriableRequestException;


/**
 * Represents a wait time out error while waiting for object from the pool.
 *
 * @author Nizar Mankulangara
 */
public class WaiterTimeoutException extends RetriableRequestException
{
  private static final long serialVersionUID = 1L;

  /**
   * Construct a new instance.
   */
  public WaiterTimeoutException()
  {
  }

  /**
   * Construct a new instance with specified message.
   *
   * @param message the message to be used for this exception.
   */
  public WaiterTimeoutException(String message)
  {
    super(message);
  }

  /**
   * Construct a new instance with specified message and cause.
   *
   * @param message the message to be used for this exception.
   * @param cause the cause to be used for this exception.
   */
  public WaiterTimeoutException(String message, Throwable cause)
  {
    super(message, cause);
  }

  /**
   * Construct a new instance with specified cause.
   *
   * @param cause the cause to be used for this exception.
   */
  public WaiterTimeoutException(Throwable cause)
  {
    super(cause);
  }
}
