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

/**
 * $id$
 */
package com.linkedin.data.transform;

/**
 * Exception used to implement fast-fail functionality. When error message is being
 * added and Interpreter is in a fast-fail mode, this exception is thrown, which
 * immediately stops processing of current instruction.
 * <p>This exception should not leak internals of data processing frameworks i.e.
 * client of data processing should not deal with this exception.
 * @author jodzga
 *
 */
class FastFailException extends RuntimeException
{

  private static final long serialVersionUID = 1L;

}
