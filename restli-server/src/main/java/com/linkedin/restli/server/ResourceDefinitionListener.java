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

import java.util.Map;


/**
 * A listener on {@link ResourceDefinition}s configured to an {@link RestLiServer}. It can be passed to <code>RestLiServer</code>
 * in {@link RestLiConfig}.
 */
public interface ResourceDefinitionListener
{
  /**
   * This method is invoked when <code>ResourceDefinition</code>s are initialized. The given map contains all the
   * top-level resources configured to the Rest.li server. Sub-resources can be obtained from the their parent resources.
   */
  void onInitialized(Map<String, ResourceDefinition> resourceDefinitions);
}
