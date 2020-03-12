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
import com.linkedin.r2.message.rest.RestRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This wrapper class ensures we catch and handle throwables from user's verifier code and
 * makes it safe.
 *
 * @author Zhenkai Zhu
 * @author David Hoa
 */
public class SafeDarkClusterVerifier implements DarkClusterVerifier
{
  private static final Logger LOG = LoggerFactory.getLogger(SafeDarkClusterVerifier.class);

  private final DarkClusterVerifier _verifier;

  public SafeDarkClusterVerifier(DarkClusterVerifier verifier)
  {
    _verifier = verifier;
  }

  @Override
  public void onResponse(RestRequest request, Response response)
  {
    try
    {
      _verifier.onResponse(request, response);
    }
    catch (Throwable error)
    {
      LOG.info("DarkCanaryVerifier " + _verifier + " throws: ", error);
    }
  }

  @Override
  public void onDarkResponse(RestRequest request, DarkResponse darkResponse)
  {
    try
    {
      _verifier.onDarkResponse(request, darkResponse);
    }
    catch (Throwable error)
    {
      LOG.info("DarkCanaryVerifier " + _verifier + " throws: ", error);
    }
  }

  @Override
  public boolean isEnabled()
  {
    return _verifier.isEnabled();
  }
}
