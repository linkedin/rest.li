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

package com.linkedin.restli.client;


/**
 * {@link ErrorHandlingBehavior} specifies the way {@link RestClient} will handle errors
 * occurred during request execution.
 */
public enum ErrorHandlingBehavior
{
  /**
   * All errors will be treated as a request failure.
   */
  FAIL_ON_ERROR,

  /**
   * The server error responses will be transformed into regular responses that can be used
   * to access partial results as well as the original exception. The communication errors will
   * still be handled as a request failure.
   */
  TREAT_SERVER_ERROR_AS_SUCCESS
}