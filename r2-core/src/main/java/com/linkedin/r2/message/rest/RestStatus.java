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

import java.nio.charset.Charset;

/**
 * Old-style enum to list commonly used REST status codes.<p/>
 *
 * We don't use a {@link Enum}, which is in most cases more convenient, because we preserve any
 * status code we receive - even when we don't know how to map it to a symbolic name.
 *
 * @author Chris Pettitt
 * @version $Revision$
 */
public class RestStatus
{
  private static final Charset UTF8 = Charset.forName("UTF-8");

  public static int OK = 200;
  public static int MOVED_PERMANENTLY = 301;
  public static int FOUND = 302;
  public static int SEE_OTHER = 303;
  public static int TEMPORARY_REDIRECT = 307;
  public static int BAD_REQUEST = 400;
  public static int NOT_FOUND = 404;
  public static int INTERNAL_SERVER_ERROR = 500;

  /**
   * Return true iff the status code indicates an HTTP 2xx status.
   *
   * @param status the status code to be checked.
   * @return true iff the status code is in the 2xx range.
   */
  public static boolean isOK(int status)
  {
    return status >= 200 && status < 300;
  }

  /**
   * return trues iff the status code indicates an HTTP 4xx status.
   *
   * @param status the status code to be checked.
   * @return true iff the status code is in the 4xx range.
   */
  public static boolean isClientError(int status)
  {
    return status >= 400 && status < 500;
  }

  /**
   * return trues iff the status code indicates an HTTP 5xx status.
   *
   * @param status the status code to be checked.
   * @return true iff the status code is in the 5xx range.
   */
  public static boolean isServerError(int status)
  {
    return status >= 500 && status < 600;
  }

  /**
   * Return a new {@link RestResponse} message with the specified status code.
   *
   * @param status the status code to use for the response message.
   * @param e a {@link Throwable} to be used as detail for the response message.
   * @return a new {@link RestResponse}, as described above.
   */
  public static RestResponse responseForError(int status, Throwable e)
  {
    // N.B., toString() is preferred over getMessage(), because many exceptions
    // e.g. NullPointerException usually have no message.  toString() will get us
    // the exception type in both cases, and the message if present.
    return responseForStatus(status, e.toString());
  }

  /**
   * Return a new {@link RestResponse} message with the specified status code.
   *
   * @param status the status code to use for the response message.
   * @param detail a detail message to be included as the body of the {@link RestResponse}.
   * @return a new {@link RestResponse} constructed as described above.
   */
  public static RestResponse responseForStatus(int status, String detail)
  {
    if (detail == null)
    {
      detail = "No detailed message";
    }

    // TODO: set charset in Content-Type header
    return new RestResponseBuilder()
            .setStatus(status)
            .setEntity(detail.getBytes(UTF8))
            .setHeader("Content-Type", "text/plain")
            .build();
  }

  private RestStatus() {}
}
