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


import com.linkedin.r2.message.BaseResponse;
import com.linkedin.r2.message.stream.entitystream.EntityStream;
import com.linkedin.util.ArgumentUtil;

import java.util.List;
import java.util.Map;


/**
 * @author Zhenkai Zhu
 * @version $Revision$
 */
/* package private */ final class StreamResponseImpl extends BaseResponse implements StreamResponse
{
  private final EntityStream _entityStream;

  /* package private */ StreamResponseImpl(EntityStream entityStream, Map<String, String> headers, List<String> cookies, int status)
  {
    super(headers, cookies, status);
    ArgumentUtil.notNull(entityStream, "entityStream");
    _entityStream = entityStream;
  }

  @Override
  public StreamResponseBuilder builder()
  {
    return new StreamResponseBuilder(this);
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
    if (!(o instanceof StreamResponseImpl))
    {
      return false;
    }
    if (!super.equals(o))
    {
      return false;
    }

    StreamResponseImpl that = (StreamResponseImpl) o;
    return _entityStream == that._entityStream;
  }

  @Override
  public int hashCode()
  {
    int result = super.hashCode();
    result = 31 * result + _entityStream.hashCode();
    return result;
  }

  @Override
  public String toString()
  {
    return "StreamResponse[headers=" + getHeaders() + ",cookies=" + getCookies() + ",status=" + getStatus() + "]";
  }
}
