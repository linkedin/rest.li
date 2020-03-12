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

package com.linkedin.darkcluster.api;

import com.linkedin.r2.message.rest.RestRequest;

/**
 * This is a NoOp implementation of DarkClusterVerifier. It is safe to use this in all cases, for testing and production, and avoids having to
 * check if the verifier is null.
 */
public class NoOpDarkClusterVerifierImpl implements DarkClusterVerifier
{
  @Override
  public void onResponse(RestRequest request, Response response)
  {

  }

  @Override
  public void onDarkResponse(RestRequest request, DarkResponse darkResponse)
  {

  }

  @Override
  public boolean isEnabled()
  {
    return false;
  }
}
