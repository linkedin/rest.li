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

package com.linkedin.restli.internal.server.methods.response;

import java.util.Map;

import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.methods.AnyRecord;
import com.linkedin.restli.internal.server.util.RestUtils;

public class GetResponseBuilder implements RestLiResponseBuilder
{

  @Override
  public PartialRestResponse buildResponse(final RestRequest request,
                                           final RoutingResult routingResult,
                                           final Object record,
                                           final Map<String, String> headers)
  {
    headers.put(RestConstants.HEADER_LINKEDIN_TYPE, record.getClass().getName());
    DataMap data =
        RestUtils.projectFields(((RecordTemplate) record).data(),
                                routingResult.getContext());

    return new PartialRestResponse(new AnyRecord(data));
  }
}
