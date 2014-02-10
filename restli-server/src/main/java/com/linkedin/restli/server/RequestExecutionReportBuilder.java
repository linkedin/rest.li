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
 * The builder class for {@link RequestExecutionReport} objects.
 */
public class RequestExecutionReportBuilder
{
  private Trace _parseqTrace;

  /**
   * Sets the Parseq trace.
   * @param parseqTrace The Parseq trace collected from a Rest.li request execution through Parseq engine.
   */
  public void setParseqTrace(Trace parseqTrace)
  {
    _parseqTrace = parseqTrace;
  }

  /**
   * Builds a {@link RequestExecutionReport} object.
   * @return A {@link RequestExecutionReport} object.
   */
  public RequestExecutionReport build()
  {
    return new RequestExecutionReport(_parseqTrace);
  }
}
