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

package com.linkedin.restli.server.annotations;

import com.linkedin.data.template.TyperefInfo;

/**
 * @author dellamag
 */
public interface RestAnnotations
{
  /** Marker string for any default value. The annotation reader fills in the actual defaults */
  String DEFAULT = "__DEFAULT__";

  public static class ROOT { }

  public static class NULL_TYPEREF_INFO extends TyperefInfo
  {
    public NULL_TYPEREF_INFO()
    {
      super(null);
    }
  }
}
