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

package com.linkedin.darkcluster;

import com.linkedin.darkcluster.api.DarkClusterVerifierManager;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;

public class MockDarkClusterVerifierManager implements DarkClusterVerifierManager
{
  @Override
  public void onDarkResponse(RestRequest originalRequest, RestResponse result, String darkClusterName)
  {

  }

  @Override
  public void onDarkError(RestRequest originalRequest, Throwable e, String darkClusterName)
  {

  }

  @Override
  public void onResponse(RestRequest originalRequest, RestResponse result)
  {

  }

  @Override
  public void onError(RestRequest originalRequest, Throwable e)
  {

  }
}
