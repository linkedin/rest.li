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
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.internal.server.RestLiResponseEnvelope;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.methods.AnyRecord;
import com.linkedin.restli.internal.server.response.CollectionResponseEnvelope;
import com.linkedin.restli.internal.server.util.RestUtils;
import com.linkedin.restli.server.CollectionResult;
import com.linkedin.restli.server.CollectionResult.PageIncrement;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.ProjectionMode;
import com.linkedin.restli.server.ResourceContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class CollectionResponseBuilder implements RestLiResponseBuilder
{
  @Override
  public PartialRestResponse buildResponse(RoutingResult routingResult, RestLiResponseEnvelope responseData)
  {
    CollectionResponseEnvelope response = responseData.getCollectionResponseEnvelope();
    PartialRestResponse.Builder builder = new PartialRestResponse.Builder();
    CollectionResponse<AnyRecord> collectionResponse = new CollectionResponse<AnyRecord>(AnyRecord.class);
    collectionResponse.setPaging(response.getCollectionResponsePaging());
    DataList elementsMap = (DataList) collectionResponse.data().get(CollectionResponse.ELEMENTS);
    for (RecordTemplate entry : response.getCollectionResponse())
    {
      CheckedUtil.addWithoutChecking(elementsMap, entry.data());
    }
    if (response.getCollectionResponseCustomMetadata() != null)
    {
      collectionResponse.setMetadataRaw(response.getCollectionResponseCustomMetadata().data());
    }
    builder.entity(collectionResponse);
    return builder.headers(responseData.getHeaders()).build();
  }

  @Override
  public RestLiResponseEnvelope buildRestLiResponseData(RestRequest request, RoutingResult routingResult,
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

      //Verify that a null wasn't passed into the collection result. If so, this is a developer error.
      if (collectionResult.getElements() == null)
      {
        throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
            "Unexpected null encountered. Null elements List inside of CollectionResult returned by the resource method: "
                + routingResult.getResourceMethod());
      }

      return buildRestLiResponseData(request, routingResult, collectionResult.getElements(),
                                     collectionResult.getPageIncrement(), collectionResult.getMetadata(),
                                     collectionResult.getTotal(), headers);
    }
  }

  private static RestLiResponseEnvelope buildRestLiResponseData(final RestRequest request,
                                                                     final RoutingResult routingResult,
                                                                     final List<? extends RecordTemplate> elements,
                                                                     final PageIncrement pageIncrement,
                                                                     final RecordTemplate customMetadata,
                                                                     final Integer totalResults,
                                                                     final Map<String, String> headers)
  {
    //Extract the resource context that contains projection information for root object entities, metadata and paging.
    final ResourceContext resourceContext = routingResult.getContext();

    //Calculate paging metadata and apply projection
    final CollectionMetadata paging =
        RestUtils.buildMetadata(request.getURI(), resourceContext, routingResult.getResourceMethod(),
                                elements, pageIncrement, totalResults);

    //PagingMetadata cannot be null at this point so we skip the null check. Notice here that we are using automatic
    //intentionally since resource methods cannot explicitly project paging. However, it should be noted that client
    //resource methods have the option of selectively setting the total to null. This happens if a client decides
    //that they want the total in the paging response, which the resource method will see in their paging path spec,
    //and then specify total when they create CollectionResult. Restli will then also subsequently separately project
    //paging using this same path spec.
    //Note that there is no chance of potential data loss here:
    //If the client decides they don't want total in their paging response, then the resource method will
    //see the lack of total in their paging path spec and then decide to set total to null. We will then also exclude it
    //when we project paging.
    //If the client decides they want total in their paging response, then the resource method will see total in their
    //paging path spec and then decide to set total to a non null value. We will then also include it when we project
    //paging.
    final RecordTemplate anyRecord = new AnyRecord(RestUtils.projectFields(paging.data(),
        ProjectionMode.AUTOMATIC, resourceContext.getPagingProjectionMask()));
    final CollectionMetadata projectedPaging = new CollectionMetadata(anyRecord.data());

    //For root object entities
    List<AnyRecord> processedElements = new ArrayList<AnyRecord>(elements.size());
    for (RecordTemplate entry : elements)
    {
      //We don't permit null elements in our lists. If so, this is a developer error.
      if (entry == null)
      {
        throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
            "Unexpected null encountered. Null element inside of a List returned by the resource method: " + routingResult
                .getResourceMethod());
      }
      processedElements.add(new AnyRecord(RestUtils
          .projectFields(entry.data(), resourceContext.getProjectionMode(), resourceContext.getProjectionMask())));
    }

    //Now for custom metadata
    final AnyRecord projectedCustomMetadata;
    if (customMetadata != null)
    {
      projectedCustomMetadata = new AnyRecord(RestUtils
          .projectFields(customMetadata.data(), resourceContext.getMetadataProjectionMode(),
              resourceContext.getMetadataProjectionMask()));
    }
    else
    {
      projectedCustomMetadata = null;
    }

    return new CollectionResponseEnvelope(processedElements,
                                          projectedPaging,
                                          projectedCustomMetadata,
                                          headers);
  }
}
