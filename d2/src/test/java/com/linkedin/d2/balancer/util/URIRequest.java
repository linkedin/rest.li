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

import com.linkedin.data.ByteString;
import com.linkedin.r2.message.MessageBuilder;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestBuilder;

import java.net.URI;

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

  @Override
  public RequestBuilder<? extends RequestBuilder<?>> requestBuilder()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public ByteString getEntity()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public MessageBuilder<? extends MessageBuilder<?>> builder()
  {
    throw new UnsupportedOperationException();
  }
}
