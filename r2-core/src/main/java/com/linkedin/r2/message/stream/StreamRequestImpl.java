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
package com.linkedin.r2.message.stream;


import com.linkedin.r2.message.BaseRequest;
import com.linkedin.r2.message.stream.entitystream.EntityStream;

import java.net.URI;
import java.util.List;
import java.util.Map;


/**
 * @author Zhenkai Zhu
 * @version $Revision$
 */
/* package private */ final class StreamRequestImpl extends BaseRequest implements StreamRequest
{
  private final EntityStream _entityStream;

  /* package private */ StreamRequestImpl(EntityStream entityStream, Map<String, String> headers, List<String> cookies, URI uri, String method)
  {
    super(headers, cookies, uri, method);
    _entityStream = entityStream;
  }

  @Override
  public StreamRequestBuilder builder()
  {
    return new StreamRequestBuilder(this);
  }

  @Override
  public EntityStream getEntityStream()
  {
    return _entityStream;
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o)
    {
      return true;
    }
    if (!(o instanceof StreamRequestImpl))
    {
      return false;
    }
    if (!super.equals(o))
    {
      return false;
    }

    return _entityStream == ((StreamRequestImpl) o)._entityStream;
  }

  @Override
  public int hashCode()
  {
    int result = super.hashCode();
    return 31 * result + _entityStream.hashCode();
  }

  @Override
  public String toString()
  {
    StringBuilder builder = new StringBuilder();
    builder.append("StreamRequest[headers=")
        .append(getHeaders())
        .append("cookies=")
        .append(getCookies())
        .append(",uri=")
        .append(getURI())
        .append(",method=")
        .append(getMethod())
        .append("]");
    return builder.toString();
  }
}
