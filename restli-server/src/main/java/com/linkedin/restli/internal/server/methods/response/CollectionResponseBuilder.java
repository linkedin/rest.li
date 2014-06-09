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

package com.linkedin.restli.internal.server.methods.response;


import com.linkedin.data.DataList;
import com.linkedin.data.collections.CheckedUtil;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.CollectionMetadata;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.internal.server.AugmentedRestLiResponseData;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.methods.AnyRecord;
import com.linkedin.restli.internal.server.util.RestUtils;
import com.linkedin.restli.server.CollectionResult;
import com.linkedin.restli.server.CollectionResult.PageIncrement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class CollectionResponseBuilder implements RestLiResponseBuilder
{
  @Override
  public PartialRestResponse buildResponse(RoutingResult routingResult, AugmentedRestLiResponseData responseData)
  {
    PartialRestResponse.Builder builder = new PartialRestResponse.Builder();
    CollectionResponse<AnyRecord> collectionResponse = new CollectionResponse<AnyRecord>(AnyRecord.class);
    collectionResponse.setPaging(responseData.getCollectionResponsePaging());
    DataList elementsMap = (DataList) collectionResponse.data().get(CollectionResponse.ELEMENTS);
    for (RecordTemplate entry : responseData.getCollectionResponse())
    {
      CheckedUtil.addWithoutChecking(elementsMap, entry.data());
    }
    if (responseData.getCollectionResponseCustomMetadata() != null)
    {
      collectionResponse.setMetadataRaw(responseData.getCollectionResponseCustomMetadata().data());
    }
    builder.entity(collectionResponse);
    return builder.headers(responseData.getHeaders()).build();
  }

  @Override
  public AugmentedRestLiResponseData buildRestLiResponseData(RestRequest request, RoutingResult routingResult,
                                                             Object object, Map<String, String> headers)
  {
    if (object instanceof List)
    {
      @SuppressWarnings({ "unchecked" })
      /** constrained by {@link com.linkedin.restli.internal.server.model.RestLiAnnotationReader#validateFinderMethod(com.linkedin.restli.internal.server.model.ResourceMethodDescriptor, com.linkedin.restli.internal.server.model.ResourceModel)} */
      List<? extends RecordTemplate> result = (List<? extends RecordTemplate>) object;

      return buildRestLiResponseData(request, routingResult, result, PageIncrement.RELATIVE, null, null, headers);
    }
    else
    {
      @SuppressWarnings({ "unchecked" })
      /** constrained by {@link com.linkedin.restli.internal.server.model.RestLiAnnotationReader#validateFinderMethod(com.linkedin.restli.internal.server.model.ResourceMethodDescriptor, com.linkedin.restli.internal.server.model.ResourceModel)} */
      CollectionResult<? extends RecordTemplate, ? extends RecordTemplate> collectionResult =
          (CollectionResult<? extends RecordTemplate, ? extends RecordTemplate>) object;

      return buildRestLiResponseData(request, routingResult, collectionResult.getElements(),
                                     collectionResult.getPageIncrement(), collectionResult.getMetadata(),
                                     collectionResult.getTotal(), headers);
    }
  }

  private static AugmentedRestLiResponseData buildRestLiResponseData(final RestRequest request,
                                                                     final RoutingResult routingResult,
                                                                     final List<? extends RecordTemplate> elements,
                                                                     final PageIncrement pageIncrement,
                                                                     final RecordTemplate customMetadata,
                                                                     final Integer totalResults,
                                                                     final Map<String, String> headers)
  {

    CollectionMetadata pagingMetadata =
        RestUtils.buildMetadata(request.getURI(), routingResult.getContext(), routingResult.getResourceMethod(),
                                elements, pageIncrement, totalResults);

    // Process projection.
    List<AnyRecord> processedElements = new ArrayList<AnyRecord>(elements.size());
    for (RecordTemplate entry : elements)
    {
      processedElements.add(new AnyRecord(RestUtils.projectFields(entry.data(), routingResult.getContext())));
    }
    return new AugmentedRestLiResponseData.Builder(routingResult.getResourceMethod().getMethodType()).headers(headers)
                                                                                                     .collectionEntities(processedElements)
                                                                                                     .collectionResponsePaging(pagingMetadata)
                                                                                                     .collectionCustomMetadata(customMetadata)
                                                                                                     .build();
  }
}
