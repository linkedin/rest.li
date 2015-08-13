/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.restli.tools.clientgen.builderspec;


/**
 * Base class for method specification contained in individual request builder.
 *
 * @author Min Chen
 */
public abstract class RequestBuilderMethodSpec
{
  protected String _methodName;

  public RequestBuilderMethodSpec()
  {

  }

  public RequestBuilderMethodSpec(String methodName)
  {
    _methodName = methodName;
  }

  public String getMethodName()
  {
    return _methodName;
  }

  public void setMethodName(String methodName)
  {
    _methodName = methodName;
  }
}
