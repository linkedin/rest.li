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

package com.linkedin.d2.balancer;

import com.linkedin.r2.RemoteInvocationException;

public class ServiceUnavailableException extends RemoteInvocationException
{
  private static final long serialVersionUID = 7302365592735805544L;

  private final String      _serviceName;
  private final String      _reason;

  public ServiceUnavailableException(String serviceName, String reason)
  {
    super("ServiceUnavailableException [_reason=" + reason + ", _serviceName=" + serviceName + "]");
    _serviceName = serviceName;
    _reason = reason;
  }

  public ServiceUnavailableException(String serviceName, String reason, Throwable cause)
  {
    super("ServiceUnavailableException [_reason=" + reason + ", _serviceName=" + serviceName + "]", cause);
    _serviceName = serviceName;
    _reason = reason;
  }
}
