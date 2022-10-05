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

package com.linkedin.restli.server;


import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.HttpStatus;
import java.util.Objects;


/**
 * @author Keren Jin
 */
public class GetResult<V extends RecordTemplate>
{
  private final V _value;
  private final HttpStatus _status;

  public GetResult(V value)
  {
    _value = value;
    _status = HttpStatus.S_200_OK;
  }

  public GetResult(V value, HttpStatus status)
  {
    _value = value;
    _status = status;
  }

  public V getValue()
  {
    return _value;
  }

  public HttpStatus getStatus()
  {
    return _status;
  }

  @Override
  public boolean equals(Object object)
  {
    if (this == object)
    {
      return true;
    }
    if (object == null || getClass() != object.getClass())
    {
      return false;
    }
    GetResult<?> getResult = (GetResult<?>) object;
    return Objects.equals(_value, getResult._value) && _status == getResult._status;
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(_value, _status);
  }
}
