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

package com.linkedin.d2.balancer.util;

import com.linkedin.jersey.api.uri.UriBuilder;
import com.linkedin.util.ArgumentUtil;
import java.net.URI;


/**
 * Rewrite d2 restli request to transporting request (http)
 */

public class D2URIRewriter implements URIRewriter
{
  final private URI _httpURI;
  final private boolean _skipReEncoding;

  public D2URIRewriter(URI httpURI)
  {
    this(httpURI, false);
  }

  /**
   * @param httpURI the target URI to rewrite to
   * @param skipReEncoding when true, constructs the rewritten URI directly from raw (already
   *                       percent-encoded) components, bypassing UriBuilder's character-by-character
   *                       re-encoding. This is significantly faster for URIs with large query strings
   *                       but assumes all URI components are already properly encoded (which is the
   *                       case when the source URI comes from {@link java.net.URI#getRawQuery()} etc.).
   */
  public D2URIRewriter(URI httpURI, boolean skipReEncoding)
  {
    _httpURI = ArgumentUtil.ensureNotNull(httpURI, "httpURI");
    _skipReEncoding = skipReEncoding;
  }

  @Override
  public URI rewriteURI(URI d2Uri)
  {
    return _skipReEncoding ? rewriteURIFromRaw(d2Uri) : rewriteURIWithBuilder(d2Uri);
  }

  private URI rewriteURIWithBuilder(URI d2Uri)
  {
    String path = d2Uri.getRawPath();

    UriBuilder builder = UriBuilder.fromUri(_httpURI);
    if (path != null)
    {
      builder.path(path);
    }
    builder.replaceQuery(d2Uri.getRawQuery());
    builder.fragment(d2Uri.getRawFragment());
    return builder.build();
  }

  /**
   * Builds the URI directly from already-encoded (raw) components, avoiding the expensive
   * character-by-character re-encoding in UriBuilder. All getRaw*() values are already
   * properly percent-encoded, so re-encoding is pure overhead.
   */
  private URI rewriteURIFromRaw(URI d2Uri)
  {
    final StringBuilder sb = new StringBuilder();

    final String scheme = _httpURI.getScheme();
    if (scheme != null)
    {
      sb.append(scheme).append("://");
    }

    final String authority = _httpURI.getRawAuthority();
    if (authority != null)
    {
      sb.append(authority);
    }

    final String basePath = _httpURI.getRawPath();
    final String appendPath = d2Uri.getRawPath();
    if (basePath != null && !basePath.isEmpty())
    {
      sb.append(basePath);
    }
    if (appendPath != null && !appendPath.isEmpty())
    {
      final boolean baseEndsWithSlash = basePath != null && !basePath.isEmpty()
          && basePath.charAt(basePath.length() - 1) == '/';
      final boolean appendStartsWithSlash = appendPath.charAt(0) == '/';
      if (baseEndsWithSlash && appendStartsWithSlash)
      {
        sb.append(appendPath, 1, appendPath.length());
      }
      else if (!baseEndsWithSlash && !appendStartsWithSlash && sb.length() > 0)
      {
        sb.append('/').append(appendPath);
      }
      else
      {
        sb.append(appendPath);
      }
    }

    final String rawQuery = d2Uri.getRawQuery();
    if (rawQuery != null)
    {
      sb.append('?').append(rawQuery);
    }

    final String rawFragment = d2Uri.getRawFragment();
    if (rawFragment != null)
    {
      sb.append('#').append(rawFragment);
    }

    return URI.create(sb.toString());
  }
}
