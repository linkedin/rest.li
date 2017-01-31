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

package com.linkedin.restli.disruptor;

import com.linkedin.r2.disruptor.DisruptContext;
import com.linkedin.restli.common.ResourceMethod;


/**
 * A controller interface that decides if and how a request should be disrupted
 *
 * @author Sean Sheng
 * @version $Revision$
 */
public interface DisruptRestController
{
  /**
   * Gets the {@link DisruptContext} for the given resource
   *
   * @param resource Resource name
   * @return The {@link DisruptContext}. Returns {@code null} if request should not be disrupted
   */
  DisruptContext getDisruptContext(String resource);

  /**
   * Gets the {@link DisruptContext} for the given resource method and resource
   *
   * @param resource Resource name
   * @param method Resource method
   * @return The {@link DisruptContext}. Returns {@code null} if request should not be disrupted
   */
  DisruptContext getDisruptContext(String resource, ResourceMethod method);

  /**
   * Gets the {@link DisruptContext} for the given resource method, resource, and method name
   *
   * @param resource Resource name
   * @param method Resource method
   * @param name Method name used to identify finders and actions
   * @return The {@link DisruptContext}. Returns {@code null} if request should not be disrupted
   */
  DisruptContext getDisruptContext(String resource, ResourceMethod method, String name);
}