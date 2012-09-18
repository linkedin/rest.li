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

package com.linkedin.data.template;

/**
 * Common base class for {@link RuntimeException}'s thrown by this package.
 */
@SuppressWarnings("serial")
public class TemplateRuntimeException extends RuntimeException
{
  TemplateRuntimeException(String message)
  {
    super(message);
  }
  TemplateRuntimeException(String message, Exception exc)
  {
    super(message, exc);
  }
}
