/*
   Copyright (c) 2020 LinkedIn Corp.

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

package com.linkedin.darkcluster.impl;

import com.linkedin.darkcluster.api.DarkClusterVerifier;
import com.linkedin.r2.message.rest.RestResponse;

class ResponseImpl implements DarkClusterVerifier.Response
{
  private final RestResponse _response;
  private final Throwable _ex;

  private ResponseImpl(RestResponse response, Throwable ex)
  {
    _response = response;
    _ex = ex;
  }

  public boolean hasError()
  {
    return _ex != null;
  }

  public Throwable getError()
  {
    return _ex;
  }

  public RestResponse getResponse()
  {
    return _response;
  }

  public static ResponseImpl success(RestResponse response)
  {
    return new ResponseImpl(response, null);
  }

  public static ResponseImpl error(Throwable ex)
  {
    return new ResponseImpl(null, ex);
  }

  public static DarkResponseImpl darkSuccess(RestResponse response, String darkClusterName)
  {
    return new DarkResponseImpl(response, null, darkClusterName);
  }

  public static DarkResponseImpl darkError(Throwable ex, String darkClusterName)
  {
    return new DarkResponseImpl(null, ex, darkClusterName);
  }

  private static class DarkResponseImpl extends ResponseImpl implements DarkClusterVerifier.DarkResponse
  {
    private final String _darkClusterName;

    DarkResponseImpl(RestResponse response, Throwable ex, String darkClusterName)
    {
      super(response, ex);
      _darkClusterName = darkClusterName;
    }

    public String getDarkClusterName()
    {
      return _darkClusterName;
    }
  }
}
