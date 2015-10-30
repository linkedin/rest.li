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

/**
 * $Id: $
 */

package com.linkedin.d2.balancer.util;

import com.linkedin.r2.message.Request;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Intended for use when the only significant aspect of the request is the URI.
 * @author Steven Ihde
 * @version $Revision: $
 */

public class URIRequest implements Request
{
  private final URI _uri;

  public URIRequest(URI uri)
  {
    _uri = uri;
  }

  public URIRequest(String uri)
  {
    _uri = URI.create(uri);
  }

  @Override
  public URI getURI()
  {
    return _uri;
  }

  public String getMethod()
  {
    throw new UnsupportedOperationException();
  }

  public String getHeader(String name)
  {
    throw new UnsupportedOperationException();
  }

  public List<String> getHeaderValues(String name)
  {
    throw new UnsupportedOperationException();
  }

  public List<String> getCookies()
  {
    throw new UnsupportedOperationException();
  }

  public Map<String, String> getHeaders()
  {
    throw new UnsupportedOperationException();
  }
}
