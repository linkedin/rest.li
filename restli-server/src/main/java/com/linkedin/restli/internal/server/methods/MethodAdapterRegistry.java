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

package com.linkedin.restli.internal.server.methods;

import com.linkedin.restli.internal.server.response.ErrorResponseBuilder;

/**
 * @author Josh Walker
 * @version $Revision: $
 *
 * @deprecated renamed to {@link DefaultMethodAdapterProvider}. Keep this class for backward compatibility, like
 *   explicit construction of this class.
 */
@Deprecated
public class MethodAdapterRegistry extends DefaultMethodAdapterProvider
{
  public MethodAdapterRegistry(ErrorResponseBuilder errorResponseBuilder)
  {
    super(errorResponseBuilder);
  }
}
