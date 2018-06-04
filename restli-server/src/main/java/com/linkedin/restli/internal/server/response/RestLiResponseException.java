/*
    Copyright (c) 2018 LinkedIn Corp.

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

package com.linkedin.restli.internal.server.response;

/**
 * This is an exception that wraps a {@link RestLiResponse}. It is used to provide a custom error for a
 * <code>Callback&lt;RestLiResponse&gt;</code>.
 */
@SuppressWarnings("serial")
public class RestLiResponseException extends Exception
{
  private RestLiResponse _restLiResponse;

  public RestLiResponseException(Throwable cause, RestLiResponse restLiResponse)
  {
    super(cause);
    _restLiResponse = restLiResponse;
  }

  public RestLiResponse getRestLiResponse()
  {
    return _restLiResponse;
  }
}
