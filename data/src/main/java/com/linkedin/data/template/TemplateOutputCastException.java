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
 * Exception that indicates that either a simple Data object cannot
 * be coerced to the desired type (e.g. integer to string) or
 * a wrapper of the desired type cannot be constructed to wrap a
 * complex Data object (e.g. wrapping a integer with a {@link RecordTemplate}.
 */
@SuppressWarnings("serial")
public class TemplateOutputCastException extends TemplateRuntimeException
{
  public TemplateOutputCastException(String message)
  {
    super(message);
  }
  public TemplateOutputCastException(String message, Exception exc)
  {
    super(message, exc);
  }
}
