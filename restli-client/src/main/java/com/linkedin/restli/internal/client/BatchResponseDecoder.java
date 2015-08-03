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

package com.linkedin.restli.internal.client;

import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;

import java.util.Map;


/**
 * Converts a raw RestResponse into a type-bound batch response.
 * @author Steven Ihde
 * @version $Revision: $
 */

public class BatchResponseDecoder<T extends RecordTemplate> extends RestResponseDecoder<BatchResponse<T>>
{
  private final Class<T> _elementClass;

  public BatchResponseDecoder(Class<T> elementClass)
  {
    _elementClass = elementClass;
  }

  @Override
  public Class<?> getEntityClass()
  {
    return _elementClass;
  }

  @Override
  public BatchResponse<T> wrapResponse(DataMap dataMap, Map<String, String> headers, ProtocolVersion version)
  {
    return dataMap == null ? null : new BatchResponse<T>(dataMap, _elementClass);
  }
}
