/*
   Copyright (c) 2018 LinkedIn Corp.

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

package com.linkedin.d2.balancer.transportadaptor;

import com.linkedin.jersey.api.uri.UriBuilder;
import com.linkedin.util.ArgumentUtil;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Rewrite d2 restli request to transporting request (http)
 */

public class D2URIRewriter implements URIRewriter
{
  final private static Logger LOGGER = LoggerFactory.getLogger(D2URIRewriter.class);
  final private URI _httpURI;

  public D2URIRewriter(URI httpURI)
  {
    _httpURI = ArgumentUtil.ensureNotNull(httpURI, "httpURI");
  }

  @Override
  public URI rewriteURI(URI d2Uri)
  {
    String path = d2Uri.getRawPath();

    UriBuilder builder = UriBuilder.fromUri(_httpURI);
    if (path != null)
    {
      builder.path(path);
    }
    builder.replaceQuery(d2Uri.getRawQuery());
    builder.fragment(d2Uri.getRawFragment());
    URI rewrittenUri = builder.build();

    LOGGER.debug("rewrite uri {} -> {}", d2Uri, rewrittenUri);

    return rewrittenUri;
  }
}
