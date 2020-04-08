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

import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;

import com.linkedin.darkcluster.api.DarkClusterVerifier;
import com.linkedin.darkcluster.api.DarkClusterVerifierManager;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;

public class DarkClusterVerifierManagerImpl implements DarkClusterVerifierManager
{
  private final DarkClusterVerifier _verifier;
  private final ExecutorService _executorService;

  public DarkClusterVerifierManagerImpl(@Nonnull DarkClusterVerifier verifier,
                                        @Nonnull ExecutorService executorService)
  {
    _verifier = verifier;
    _executorService = executorService;
  }

  @Override
  public void onDarkResponse(RestRequest originalRequest, RestResponse result, String darkClusterName)
  {
    if (_verifier.isEnabled())
    {
      _executorService.execute(() -> _verifier.onDarkResponse(originalRequest,
                                                                 ResponseImpl.darkSuccess(result, darkClusterName)));
    }
  }

  @Override
  public void onDarkError(RestRequest originalRequest, Throwable e, String darkClusterName)
  {
    if (_verifier.isEnabled())
    {
      _executorService.execute(
        () -> _verifier.onDarkResponse(originalRequest, ResponseImpl.darkError(e, darkClusterName)));
    }
  }

  @Override
  public void onResponse(RestRequest originalRequest, RestResponse result)
  {
    if (_verifier.isEnabled())
    {
      _executorService.execute(() -> _verifier.onResponse(originalRequest, ResponseImpl.success(result)));
    }
  }

  @Override
  public void onError(RestRequest originalRequest, Throwable e)
  {
    if (_verifier.isEnabled())
    {
      _executorService.execute(() -> _verifier.onResponse(originalRequest, ResponseImpl.error(e)));
    }
  }
}
