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

/* $Id$ */
package test.r2.perf.client;

import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;

import java.net.URI;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public class RestRequestGenerator implements RequestGenerator<RestRequest>
{
  private final URI _uri;
  private final StringRequestGenerator _generator;

  public RestRequestGenerator(URI uri, int numMsgs, int msgSize)
  {
    this(uri, new StringRequestGenerator(numMsgs, msgSize));
  }

  public RestRequestGenerator(URI uri, StringRequestGenerator generator)
  {
    _uri = uri;
    _generator = generator;
  }

  @Override
  public RestRequest nextMessage()
  {
    final String stringMsg = _generator.nextMessage();
    if (stringMsg == null)
    {
      return null;
    }

    return new RestRequestBuilder(_uri)
            .setEntity(stringMsg.getBytes())
            .setMethod("POST")
            .build();
  }
}
