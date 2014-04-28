/*
   Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.restli.client.response;


import com.linkedin.restli.common.ComplexKeySpec;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.TypeSpec;
import com.linkedin.restli.internal.common.ResponseUtils;

import java.util.Map;


/**
 * Typed create response so create requests made using 1.0 builders can access a non-serialized version of the key
 * returned from a {@link com.linkedin.restli.client.CreateRequest}.
 *
 * Users can cast {@link com.linkedin.restli.common.EmptyRecord}s returned from
 * {@link com.linkedin.restli.client.CreateRequest}s to {@link com.linkedin.restli.client.response.CreateResponse}s to
 * get a non-serialized form of the key.
 *
 * @author Moira Tagle
 */
public class CreateResponse<K> extends EmptyRecord
{
  private K _key;

  public CreateResponse(K key)
  {
    _key = key;
  }

  public K getId()
  {
    return _key;
  }
}
