/*
   Copyright (c) 2016 LinkedIn Corp.

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

package com.linkedin.r2;



/**
 * Represents an error that needs retry the same request to a different host.
 * For example, trying to load cache from a host in cluster but found a miss.
 * The same request will be made again if RetryDynamicClient is used.
 *
 * @author Xialin Zhu
 */
public class RetriableRequestException extends RemoteInvocationException
{
  private static final long serialVersionUID = 1L;

  private boolean _doNotRetryOverride = false;

  /**
   * Construct a new instance.
   */
  public RetriableRequestException()
  {
  }

  /**
   * Construct a new instance with specified message.
   *
   * @param message the message to be used for this exception.
   */
  public RetriableRequestException(String message)
  {
    super(message);
  }

  /**
   * Construct a new instance with specified message and cause.
   *
   * @param message the message to be used for this exception.
   * @param cause the cause to be used for this exception.
   */
  public RetriableRequestException(String message, Throwable cause)
  {
    super(message, cause);
  }

  /**
   * Construct a new instance with specified cause.
   *
   * @param cause the cause to be used for this exception.
   */
  public RetriableRequestException(Throwable cause)
  {
    super(cause);
  }

  public void setDoNotRetryOverride(boolean doNotRetryOverride)
  {
    _doNotRetryOverride = doNotRetryOverride;
  }

  public boolean getDoNotRetryOverride()
  {
   return _doNotRetryOverride;
  }
}
