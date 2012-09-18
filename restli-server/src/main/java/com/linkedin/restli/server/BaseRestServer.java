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

package com.linkedin.restli.server;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linkedin.common.callback.Callback;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.transport.common.RestRequestHandler;
import com.linkedin.restli.internal.server.model.ResourceModel;

/**
 * @author dellamag
 */
public abstract class BaseRestServer implements RestRequestHandler
{
  private static final Logger log = LoggerFactory.getLogger(BaseRestServer.class);

  private final RestLiConfig _config;
  protected Map<String, ResourceModel> _rootResources;

  public BaseRestServer(final RestLiConfig config)
  {
    _config = config;
  }

  /**
   * @see com.linkedin.r2.transport.common.RestRequestHandler#handleRequest(com.linkedin.r2.message.rest.RestRequest,
   *      com.linkedin.common.callback.Callback)
   */
  @Override
  public void handleRequest(final RestRequest request,
                            final Callback<RestResponse> callback)
  {
    try
    {
      doHandleRequest(request, callback);
    }
    catch (Exception e)
    {
      log.error("Uncaught exception", e);
      callback.onError(e);
    }
  }

  protected abstract void doHandleRequest(RestRequest request,
                                          Callback<RestResponse> callback);

  protected RestLiConfig getConfig()
  {
    return _config;
  }
}
