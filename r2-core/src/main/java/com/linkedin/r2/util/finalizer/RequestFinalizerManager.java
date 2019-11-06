/*
   Copyright (c) 2019 LinkedIn Corp.

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

package com.linkedin.r2.util.finalizer;


/**
 * Manages {@link RequestFinalizer} registration. The executions
 * are intended to be the last logic run at the end of a request.
 *
 * @author Chris Zhang
 */
public interface RequestFinalizerManager
{
  /**
   * Register a {@link RequestFinalizer} to be run at the end of a request.
   *
   * @param requestFinalizer RequestFinalizer to register.
   * @return True if successfully registered, else false.
   */
  boolean registerRequestFinalizer(RequestFinalizer requestFinalizer);
}
