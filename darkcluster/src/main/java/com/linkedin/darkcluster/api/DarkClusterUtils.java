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

import java.net.URI;

import com.linkedin.d2.balancer.util.LoadBalancerUtil;
import com.linkedin.jersey.api.uri.UriBuilder;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;

import static com.linkedin.d2.discovery.util.LogUtil.debug;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DarkClusterUtils
{
  private static final Logger LOG = LoggerFactory.getLogger(DarkClusterUtils.class);

  public static URI rewriteUri(String darkServiceName, URI uri)
  {
    String path = LoadBalancerUtil.getRawPathFromUri(uri);

    UriBuilder builder = UriBuilder.fromPath(darkServiceName);
    if (path != null)
    {
      builder.path(path);
    }
    builder.replaceQuery(uri.getRawQuery());
    builder.fragment(uri.getRawFragment());
    URI rewrittenUri = builder.build();

    debug(LOG, "dark canary rewrite uri " + uri + " -> " + rewrittenUri);

    return rewrittenUri;
  }

  public static RestRequest updateRequestInfo(String darkServiceName, RestRequest request)
  {
    RestRequestBuilder builder = request.builder();
    URI newUri = rewriteUri(darkServiceName, request.getURI());

    builder.setURI(newUri);

    return builder.build();
  }
}
