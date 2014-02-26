/*
   Copyright (c) 2014 LinkedIn Corp.

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


/**
 * The callback interface to be invoked at the end of a Rest.li request execution.
 * @param <T>
 */
public interface RequestExecutionCallback<T>
{
  /**
   * Called if the asynchronous operation failed with an error.
   *
   * @param e the error
   * @param executionReport contains data about the request execution process. This parameter will contain a value
   *                        only if the request was a debug request.
   */
  void onError(Throwable e, RequestExecutionReport executionReport);

  /**
   * Called if the asynchronous operation completed with a successful result.
   *
   * @param result the result of the asynchronous operation
   * @param executionReport contains data about the request execution process. This parameter will contain a value
   *                        only if the request was a debug request.
   */
  void onSuccess(T result, RequestExecutionReport executionReport);
}
