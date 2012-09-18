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
package com.linkedin.r2.transport.common.bridge.common;

import java.util.Map;

/**
 * An object that represents the union of response or error, both coupled with wire attributes.
 *
 * @author Chris Pettitt
 * @version $Revision$
 */
public interface TransportResponse<T>
{
  /**
   * Returns the underlying value for this response. If this response has an error then this method
   * will return {@code null}.
   *
   * @return the value for this response or {@code null} if this response has an error.
   */
  T getResponse();

  /**
   * Returns {@code true} if this response has an error. Use {@link #getError()} to get the error.
   *
   * @return {@code true} if this response has an error.
   */
  boolean hasError();

  /**
   * If this response has an error, this method returns the error. Otherwise {@code null} is
   * returned.
   *
   * @return the response for this error or {@code null} if there is no error.
   */
  Throwable getError();

  /**
   * @return the wire attributes for this response.
   */
  Map<String, String> getWireAttributes();
}
