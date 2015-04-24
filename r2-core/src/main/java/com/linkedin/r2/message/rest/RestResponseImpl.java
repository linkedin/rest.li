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
package com.linkedin.r2.message.rest;


import com.linkedin.data.ByteString;

import java.util.List;
import java.util.Map;


/**
 * @author Chris Pettitt
 * @version $Revision$
 */
/* package private */ final class RestResponseImpl extends BaseRestMessage implements RestResponse
{
  private final int _status;

  /* package private */ RestResponseImpl(
      ByteString entity, Map<String, String> headers, List<String> cookies, int status)
  {
    super(entity, headers, cookies);
    _status = status;
  }

  public int getStatus()
  {
    return _status;
  }

  @Override
  public RestResponseBuilder builder()
  {
    return new RestResponseBuilder(this);
  }

  @Override
  public RestResponseBuilder responseBuilder()
  {
    return builder();
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o)
    {
      return true;
    }
    if (!(o instanceof RestResponseImpl))
    {
      return false;
    }
    if (!super.equals(o))
    {
      return false;
    }

    RestResponseImpl that = (RestResponseImpl) o;
    return _status == that._status;
  }

  @Override
  public int hashCode()
  {
    int result = super.hashCode();
    result = 31 * result + _status;
    return result;
  }

  @Override
  public String toString()
  {
    StringBuilder builder = new StringBuilder();
    builder.append("RestResponse[headers=")
        .append(getHeaders())
        .append("cookies=")
        .append(getCookies())
        .append(",status=")
        .append(_status)
        .append(",entityLength=")
        .append(getEntity().length())
        .append("]");
    return builder.toString();
  }
}
