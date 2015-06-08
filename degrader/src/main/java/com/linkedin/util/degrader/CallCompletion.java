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
package com.linkedin.util.degrader;


/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public interface CallCompletion
{
  /**
   * record the time but do not end the call. if record is called before endCall, the recorded time would be used as
   * end time of the call
   */
  void record();

  /**
   * Used to indicate that a call completed successfully.
   */
  void endCall();

  /**
   * Used to indicate that a call completed with an error.
   */
  void endCallWithError();

  /**
   * Used to indicate that a call completed with an error.
   * @param errorType auxillary data that indicates what kind of error it is and how it should be counted for tracking
   */
  void endCallWithError(ErrorType errorType);
}
