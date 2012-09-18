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

package com.linkedin.restli.internal.server.methods;

import java.io.IOException;
import java.util.Map;

import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.CollectionRequest;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.CreateStatus;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.util.ArgumentUtils;
import com.linkedin.restli.internal.server.util.DataMapUtils;
import com.linkedin.restli.server.BatchCreateRequest;
import com.linkedin.restli.server.BatchCreateResult;
import com.linkedin.restli.server.CreateResponse;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class BatchCreateMethodAdapter
          implements RestLiMethodAdapter
{

  BatchCreateMethodAdapter()
  {
  }

  @Override
  public PartialRestResponse buildResponse(final RestRequest request,
                                           final RoutingResult routingResult,
                                           final Object object,
                                           final Map<String, String> headers)
      throws IOException
  {
    @SuppressWarnings({ "unchecked" })
    /** constrained by the signature of {@link CollectionResource#batchCreate} */
    BatchCreateResult<?, ?> list = (BatchCreateResult<?, ?>) object;

    CollectionResponse<CreateStatus> batchResponse =
        new CollectionResponse<CreateStatus>(CreateStatus.class);

    for (CreateResponse e : list.getResults())
    {
      CreateStatus s = new CreateStatus();
      if (e.hasId())
      {
        s.setId(e.getId().toString());
      }
      s.setStatus(e.getStatus().getCode());

      batchResponse.getElements().add(s);
    }
    headers.put(RestConstants.HEADER_LINKEDIN_TYPE, CollectionResponse.class.getName());
    headers.put(RestConstants.HEADER_LINKEDIN_SUB_TYPE, CreateStatus.class.getName());


    return new PartialRestResponse(batchResponse);
  }

  @Override
  public Object[] buildArguments(final RoutingResult routingResult,
                                 final RestRequest request)
  {
    Class<? extends RecordTemplate> valueClass =
        ArgumentUtils.getValueClass(routingResult);
    DataMap dataMap = DataMapUtils.readMap(request.getEntity().asInputStream());
    @SuppressWarnings({ "unchecked" })
    CollectionRequest collectionRequest = new CollectionRequest(dataMap, valueClass);
    @SuppressWarnings({ "unchecked" })
    BatchCreateRequest batchRequest =
        new BatchCreateRequest(collectionRequest.getElements());

    Object[] positionalArguments = { batchRequest };
    return ArgumentBuilder.buildArgs(positionalArguments,
                                     routingResult.getResourceMethod().getParameters(),
                                     routingResult.getContext());
  }
}
