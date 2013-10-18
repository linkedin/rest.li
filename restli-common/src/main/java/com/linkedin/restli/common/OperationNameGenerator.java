/*
   Copyright (c) 2013 LinkedIn Corp.

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

package com.linkedin.restli.common;


/**
 * @author kparikh
 */
public class OperationNameGenerator
{
  /**
   * Builds the operation string for a method
   * @param method
   * @param methodName
   * @return
   */
  public static String generate(ResourceMethod method, String methodName)
  {
    String operation = method.toString();
    final String ACTION_AND_FINDER_SEPARATOR = ":";

    switch (method)
    {
      case ACTION:
        operation += (ACTION_AND_FINDER_SEPARATOR + methodName);
        break;
      case FINDER:
        operation += (ACTION_AND_FINDER_SEPARATOR + methodName);
        break;
    }
    return operation;
  }
}
