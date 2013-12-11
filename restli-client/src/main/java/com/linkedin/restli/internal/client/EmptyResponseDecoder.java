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
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;

/**
 * Converts a raw RestResponse into a status-only response by ensuring the RestResponse
 * contains no entity.
 * @author Steven Ihde
 * @version $Revision: $
 */

public class EmptyResponseDecoder extends RestResponseDecoder<EmptyRecord>
{
  @Override
  public Class<?> getEntityClass()
  {
    return Void.class;
  }

  @Override
  protected EmptyRecord wrapResponse(DataMap dataMap, ProtocolVersion version)
  {
    throw new UnsupportedOperationException("Empty response should have no entity");
  }
}
