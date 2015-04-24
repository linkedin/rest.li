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
 * $Id: $
 */

package com.linkedin.r2;

/**
 * Represents an error condition encountered during the process of remote service invocation,
 * or an unexpected error reported by the remote service.  This exception MUST NOT
 * be used to describe remote business logic exceptions.
 *
 * @author Steven Ihde
 * @version $Revision: $
 */

public class RemoteInvocationException extends Exception
{
  private static final long serialVersionUID = 1L;

  /**
   * Construct a new instance.
   */
  public RemoteInvocationException()
  {
  }

  /**
   * Construct a new instance with specified message.
   *
   * @param message the message to be used for this exception.
   */
  public RemoteInvocationException(String message)
  {
    super(message);
  }

  /**
   * Construct a new instance with specified message and cause.
   *
   * @param message the message to be used for this exception.
   * @param cause the cause to be used for this exception.
   */
  public RemoteInvocationException(String message, Throwable cause)
  {
    super(message, cause);
  }

  /**
   * Construct a new instance with specified cause.
   *
   * @param cause the cause to be used for this exception.
   */
  public RemoteInvocationException(Throwable cause)
  {
    super(cause);
  }
}
