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


import com.linkedin.parseq.trace.Trace;


/**
 * The request execution report contains information about the execution of a Rest.li request such as
 * traces, logs, measurements etc. The information is collected by the Rest.li server infrastructure at
 * various points of the request execution. The instances of this class should be created by using a
 * {@link RequestExecutionReportBuilder} object.
 */
public class RequestExecutionReport
{
  private final Trace _parseqTrace;

  RequestExecutionReport(Trace parseqTrace)
  {
    _parseqTrace = parseqTrace;
  }

  /**
   * Gets the Parseq trace information if the request was executed by the Parseq engine.
   * @return Parseq Trace of the request if the request was executed by the Parseq engine, otherwise null.
   */
  public Trace getParseqTrace()
  {
    return _parseqTrace;
  }
}
