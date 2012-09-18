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

package com.linkedin.restli.internal.client;

import com.linkedin.data.DataMap;
import com.linkedin.restli.common.ActionResponse;


/**
 * Converts a raw RestResponse to a type-bound action response.
 *
 * @param <T> response type
 *
 * @author Eran Leshem
 */
public class ActionResponseDecoder<T> extends RestResponseDecoder<T>
{
  private final Class<T> _returnType;

  public ActionResponseDecoder(Class<T> returnType)
  {
    _returnType = returnType;
  }

  @Override
  public Class<?> getEntityClass()
  {
    return _returnType;
  }

  @Override
  protected T wrapResponse(DataMap dataMap)
  {
    return new ActionResponse<T>(dataMap, _returnType).getValue();
  }
}
