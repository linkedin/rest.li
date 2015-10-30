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
package com.linkedin.r2.message.stream;

import com.linkedin.r2.RemoteInvocationException;

/**
 * A {@link RemoteInvocationException} which contains a {@link StreamResponse} message.
 *
 * @author Zhenkai Zhu
 */
public class StreamException extends RemoteInvocationException
{
  private static final long serialVersionUID = 1;

  private final StreamResponse _response;

  /**
   * Construct a new instance using the specified response message and exception message.
   *
   * @param response the {@link StreamResponse} message for this exception.
   */
  public StreamException(StreamResponse response)
  {
    _response = response;
  }

  /**
   * Construct a new instance using the specified response message and cause.
   *
   * @param response the {@link StreamResponse} message for this exception.
   * @param cause the cause of this exception.
   */
  public StreamException(StreamResponse response, Throwable cause)
  {
    super(cause);
    _response = response;
  }

  /**
   * Construct a new instance using the specified response message, exception message, and cause.
   *
   * @param response the {@link StreamResponse} message for this exception.
   * @param message the exception message for this exception.
   * @param cause the cause of this exception.
   */
  public StreamException(StreamResponse response, String message, Throwable cause)
  {
    super(message, cause);
    _response = response;
  }

  /**
   * Construct a new instance using the specified response message and exception message.
   *
   * @param response the {@link StreamResponse} message for this exception.
   * @param message the exception message for this exception.
   */
  public StreamException(StreamResponse response, String message)
  {
    super(message);
    _response = response;
  }

  /**
   * Return the {@link StreamResponse} contained by this exception.
   *
   * @return the {@link StreamResponse} contained by this exception.
   */
  public StreamResponse getResponse()
  {
    return _response;
  }

  @Override
  public String toString()
  {
    return "StreamException{" +
            "_response=" + _response +
            "} ";
  }

}
