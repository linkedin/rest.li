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

import java.util.List;
import java.util.Map;

import com.linkedin.data.ByteString;
import com.linkedin.r2.message.BaseMessage;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
/* package private */ abstract class BaseRestMessage extends BaseMessage implements RestMessage
{
  private final Map<String, String> _headers;

  protected BaseRestMessage(ByteString entity, Map<String, String> headers)
  {
    super(entity);

    assert headers != null;
    _headers = headers;
  }

  @Override
  public RestMessageBuilder<? extends RestMessageBuilder<?>> restBuilder()
  {
    return builder();
  }

  @Override
  public abstract RestMessageBuilder<? extends RestMessageBuilder<?>> builder();

  @Override
  public String getHeader(String name)
  {
    return _headers.get(name);
  }

  @Override
  public List<String> getHeaderValues(String name)
  {
    final String headerVal = getHeader(name);
    if (headerVal == null)
    {
      return null;
    }
    return RestUtil.getHeaderValues(headerVal);
  }

  @Override
  public Map<String, String> getHeaders()
  {
    return _headers;
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o)
    {
      return true;
    }
    if (!(o instanceof BaseRestMessage))
    {
      return false;
    }
    if (!super.equals(o))
    {
      return false;
    }

    BaseRestMessage that = (BaseRestMessage) o;
    return _headers.equals(that._headers);
  }

  @Override
  public int hashCode()
  {
    int result = super.hashCode();
    result = 31 * result + _headers.hashCode();
    return result;
  }
}
