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
package com.linkedin.r2.message;

import java.util.List;
import java.util.Map;

/**
 * @author Zhenkai Zhu
 */
public abstract class BaseResponse extends BaseMessage implements Response
{
  private final int _status;

  protected BaseResponse(Map<String, String> headers, List<String> cookies, int status)
  {
    super(headers, cookies);
    _status = status;
  }

  @Override
  public int getStatus()
  {
    return _status;
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o)
    {
      return true;
    }
    if (!(o instanceof BaseResponse))
    {
      return false;
    }
    if (!super.equals(o))
    {
      return false;
    }

    return _status == ((BaseResponse) o)._status;
  }

  @Override
  public int hashCode()
  {
    int result = super.hashCode();
    result = 31 * result + _status;
    return result;
  }
}
