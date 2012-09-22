package com.linkedin.restli.internal.server.methods.response;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.CollectionMetadata;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.methods.AnyRecord;
import com.linkedin.restli.internal.server.util.RestUtils;
import com.linkedin.restli.server.CollectionResult;

public class FinderResponseBuilder implements RestLiResponseBuilder
{

  @Override
  public PartialRestResponse buildResponse(final RestRequest request,
                                           final RoutingResult routingResult,
                                           final Object object,
                                           final Map<String, String> headers)
      throws IOException
  {
    if (object instanceof List)
    {
      @SuppressWarnings({"unchecked"})
      /** constrained by {@link com.linkedin.restli.internal.server.model.RestLiAnnotationReader#validateFinderMethod(com.linkedin.restli.internal.server.model.ResourceMethodDescriptor, com.linkedin.restli.internal.server.model.ResourceModel)} */
      List<? extends RecordTemplate> result = (List<? extends RecordTemplate>)object;

      return buildResponseImpl(request,
                               routingResult,
                               result,
                               null,
                               null,
                               headers);
    }
    else
    {
      @SuppressWarnings({"unchecked"})
      /** constrained by {@link com.linkedin.restli.internal.server.model.RestLiAnnotationReader#validateFinderMethod(com.linkedin.restli.internal.server.model.ResourceMethodDescriptor, com.linkedin.restli.internal.server.model.ResourceModel)} */
      CollectionResult<? extends RecordTemplate, ? extends RecordTemplate> collectionResult =
          (CollectionResult<? extends RecordTemplate, ? extends RecordTemplate>) object;

      return buildResponseImpl(request,
                               routingResult,
                               collectionResult.getElements(),
                               collectionResult.getMetadata(),
                               collectionResult.getTotal(),
                               headers);
    }
  }

  private static PartialRestResponse buildResponseImpl(final RestRequest request,
                                                       final RoutingResult routingResult,
                                                       final List<? extends RecordTemplate> elements,
                                                       final RecordTemplate customMetadata,
                                                       final Integer totalResults,
                                                       final Map<String, String> headers)
      throws IOException
  {

    Class<? extends RecordTemplate> valueClass =
        routingResult.getResourceMethod().getResourceModel().getValueClass();
    CollectionResponse<AnyRecord> collectionResponse =
        new CollectionResponse<AnyRecord>(AnyRecord.class);

    CollectionMetadata pagingMetadata =
        RestUtils.buildMetadata(request.getURI(),
                                routingResult.getContext(),
                                routingResult.getResourceMethod(),
                                elements,
                                totalResults);
    collectionResponse.setPaging(pagingMetadata);

    DataList elementsMap =
        (DataList) collectionResponse.data().get(CollectionResponse.ELEMENTS);
    for (RecordTemplate entry : elements)
    {
      DataMap data = RestUtils.projectFields(entry.data(), routingResult.getContext());

      elementsMap.add(data);
    }

    if (customMetadata != null)
    {
      collectionResponse.setMetadataRaw(customMetadata.data());
    }
    headers.put(RestConstants.HEADER_LINKEDIN_TYPE, CollectionResponse.class.getName());
    headers.put(RestConstants.HEADER_LINKEDIN_SUB_TYPE, valueClass.getName());

    return new PartialRestResponse(collectionResponse);
  }


}
