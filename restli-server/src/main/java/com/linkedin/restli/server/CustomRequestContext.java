/*
   Copyright (c) 2017 LinkedIn Corp.

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


import java.util.Optional;


/**
 * APIs for managing custom request context data. These data are intended to be fully accessible and mutable across
 * the entire request lifecyle (specifically resource and filters).
 */
public interface CustomRequestContext
{
  /**
   * Fetch a custom data from request context. Empty Optional is returned for non-exist data.
   *
   * @param key key of the data
   * @return the custom data
   */
  default Optional<Object> getCustomContextData(String key)
  {
    return Optional.empty();
  }

  /**
   * Add a custom data (null will be ignored) to the request context that is shared between handling resource and filters.
   * Key with existing data will be replaced with new data.
   *
   * For sharing temporary data between filters only, use filter scratchpad instead.
   *  @param key identifier of the data
   * @param data custom data, null will be ignored
   */
  default void putCustomContextData(String key, Object data)
  {
    return;
  }

  /**
   * Remove a existing custom data from the request context if present.
   *
   * @param key key of the data
   */
  default Optional<Object> removeCustomContextData(String key)
  {
    return Optional.empty();
  }
}