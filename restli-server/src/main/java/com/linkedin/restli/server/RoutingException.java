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

package com.linkedin.restli.server;

import java.nio.charset.Charset;

import com.linkedin.data.ByteString;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;

/**
 * @author dellamag
 */
public class RoutingException extends RuntimeException
{
  private static final long serialVersionUID = 1L;

  private final int _status;

  /**
   * @param status HTTP response status
   */
  public RoutingException(final int status)
  {
    _status = status;
  }

  /**
   * @param message error message
   * @param status HTTP response status
   */
  public RoutingException(final String message,
                          final int status)
  {
    super(message);
    _status = status;
  }

  public int getStatus()
  {
    return _status;
  }

  /**
   * @return {@link RestResponse} based on this exception
   */
  public RestResponse buildRestResponse()
  {
    RestResponseBuilder builder = new RestResponseBuilder().setStatus(_status);
    if (getMessage() != null)
    {
      builder.setEntity(ByteString.copyString(getMessage(), Charset.defaultCharset()));
    }

    return builder.build();
  }
}
