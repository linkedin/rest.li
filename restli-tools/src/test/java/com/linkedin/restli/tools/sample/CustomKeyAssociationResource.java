/*
   Copyright (c) 2019 LinkedIn Corp.

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

package com.linkedin.restli.tools.sample;

import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.server.BatchUpdateRequest;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.AssocKeyParam;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.Key;
import com.linkedin.restli.server.annotations.RestLiAssociation;
import com.linkedin.restli.server.resources.AssociationResourceTemplate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Sample association resource with a custom key.
 */
@RestLiAssociation(
        name = "customKeyAssociation",
        namespace = "com.linkedin.restli.tools.sample",
        assocKeys = {
                @Key(name = "longId", type = CustomLong.class, typeref = CustomLongRef.class),
                @Key(name = "dateId", type = String.class)
        }
)
public class CustomKeyAssociationResource extends AssociationResourceTemplate<SimpleGreeting>
{

  @Override
  public SimpleGreeting get(CompoundKey key)
  {
    CustomLong longId = (CustomLong)key.getPart("longId");
    String dateId = (String) key.getPart("dateId");

    return new SimpleGreeting().setId(longId.toLong()).setMessage(dateId);
  }

  @Override
  public BatchUpdateResult<CompoundKey, SimpleGreeting> batchUpdate(BatchUpdateRequest<CompoundKey, SimpleGreeting> entities)
  {
    Set<CompoundKey> keys = entities.getData().keySet();
    Map<CompoundKey, UpdateResponse> responseMap = new HashMap<>();

    for(CompoundKey key : keys)
    {
      responseMap.put(key, new UpdateResponse(HttpStatus.S_201_CREATED));
    }
    return new BatchUpdateResult<>(responseMap);
  }

  @Finder("longId")
  public List<SimpleGreeting> dateOnly(@AssocKeyParam(value="longId", typeref=CustomLongRef.class) CustomLong longId)
  {
    return Collections.emptyList();
  }

}
