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

package com.linkedin.restli.internal.common;


import com.linkedin.common.Version;


/**
 * @author Keren Jin
 */
public enum RestliVersion
{
  RESTLI_1_0_0(new Version(1, 0, 0)),
  RESTLI_2_0_0(new Version(2, 0, 0));

  private final Version _restliVersion;

  RestliVersion(Version Version)
  {
    _restliVersion = Version;
  }

  public Version getRestliVersion()
  {
    return _restliVersion;
  }

  public static RestliVersion lookUpRestliVersion(Version version)
  {
    for (RestliVersion v : RestliVersion.values())
    {
      if (version.equals(v.getRestliVersion()))
      {
        return v;
      }
    }

    return null;
  }
}
