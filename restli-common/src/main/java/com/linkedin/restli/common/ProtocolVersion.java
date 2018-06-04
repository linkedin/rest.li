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


import com.linkedin.common.Version;


/**
 * Represents a Rest.li protocol version.
 *
 * @see com.linkedin.common.Version
 * @see com.linkedin.restli.internal.common.AllProtocolVersions
 *
 * @author kparikh
 */
public class ProtocolVersion extends Version
{
  public ProtocolVersion(String version)
  {
    super(version);
  }

  public ProtocolVersion(int major, int minor, int patch)
  {
    super(major, minor, patch);
  }
}

