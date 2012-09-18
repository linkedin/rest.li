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
 * $Id: $
 */

package com.linkedin.r2.util;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public interface TimeoutExecutor
{
  /**
   * Executes the given task when the associated timeout occurs, or immediately if the timeout
   * has already occurred.  If the timeout does not occur, the task will not be executed.
   * The task will never be executed more than once.
   *
   * @param task the task to be associated with the timeout.
   */
  void addTimeoutTask(Runnable task);
}
