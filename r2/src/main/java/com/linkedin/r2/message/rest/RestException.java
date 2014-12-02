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
package com.linkedin.r2.message.rest;

import com.linkedin.r2.RemoteInvocationException;

/**
 * A {@link RemoteInvocationException} which contains a {@link RestResponse} message.
 *
 * @author Chris Pettitt
 * @version $Revision$
 */
public class RestException extends RemoteInvocationException
{
  private static final long serialVersionUID = 1;

  private final RestResponse _response;

  /**
   * Construct a new instance, using the specified response message.
   *
   * @param response the {@link RestResponse} message for this exception.
   */
  public RestException(RestResponse response)
  {
    // TODO: we should probably should check the content-type / charset and decode appropriately
    super(response.getEntity().asAvroString());
    _response = response;
  }

  /**
   * Construct a new instance using the specified response message and cause.
   *
   * @param response the {@link RestResponse} message for this exception.
   * @param cause the cause of this exception.
   */
  public RestException(RestResponse response, Throwable cause)
  {
    super(cause);
    _response = response;
  }

  /**
   * Construct a new instance using the specified response message, exception message, and cause.
   *
   * @param response the {@link RestResponse} message for this exception.
   * @param message the exception message for this exception.
   * @param cause the cause of this exception.
   */
  public RestException(RestResponse response, String message, Throwable cause)
  {
    super(message, cause);
    _response = response;
  }

  /**
   * Construct a new instance using the specified response message and exception message.
   *
   * @param response the {@link RestResponse} message for this exception.
   * @param message the exception message for this exception.
   */
  public RestException(RestResponse response, String message)
  {
    super(message);
    _response = response;
  }

  /**
   * Return the {@link RestResponse} contained by this exception.
   *
   * @return the {@link RestResponse} contained by this exception.
   */
  public RestResponse getResponse()
  {
    return _response;
  }

  @Override
  public String toString()
  {
    return "RestException{" +
            "_response=" + _response +
            "} ";
  }

  /**
   * Factory method to obtain a new instance for a specified HTTP status code with the given cause.
   *
   * @param status the HTTP status code for the exception.
   * @param throwable the throwable to be used as the cause for this exception.
   * @return a new instance, as described above.
   */
  public static RestException forError(int status, Throwable throwable)
  {
    return new RestException(RestStatus.responseForError(status, throwable), throwable);
  }

  /**
   * Factory method to obtain a new instance for the specified HTTP status code.
   *
   * @param status the HTTP status code for the exception.
   * @param detail the detail message to be returned with this exception.
   * @return a new instance, as described above.
   */
  public static RestException forError(int status, String detail)
  {
    return new RestException(RestStatus.responseForStatus(status, detail));
  }

}
