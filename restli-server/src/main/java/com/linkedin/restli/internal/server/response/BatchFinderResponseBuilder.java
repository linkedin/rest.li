/*
   Copyright (c) 2017 LinkedIn Corp.

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

package com.linkedin.restli.internal.server.response;

import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.collections.CheckedUtil;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.timing.FrameworkTimingKeys;
import com.linkedin.r2.message.timing.TimingContextUtil;
import com.linkedin.restli.common.BatchCollectionResponse;
import com.linkedin.restli.common.CollectionMetadata;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.URIParamUtils;
import com.linkedin.restli.internal.server.ResourceContextImpl;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.methods.AnyRecord;
import com.linkedin.restli.internal.server.response.BatchFinderResponseEnvelope.BatchFinderEntry;
import com.linkedin.restli.internal.server.util.RestUtils;
import com.linkedin.restli.server.BatchFinderResult;
import com.linkedin.restli.server.CollectionResult;
import com.linkedin.restli.server.ProjectionMode;
import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.RestLiServiceException;
import java.net.HttpCookie;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.linkedin.restli.common.HttpStatus.*;


/**
 * {@link BatchFinderResponseBuilder} is the implementation of {@link RestLiResponseBuilder}
 * for BATCH_FINDER responses
 *
 * @author Maxime Lamure
 */
public class BatchFinderResponseBuilder
    implements RestLiResponseBuilder<RestLiResponseData<BatchFinderResponseEnvelope>>
{

  private final ErrorResponseBuilder _errorResponseBuilder;

  public BatchFinderResponseBuilder(ErrorResponseBuilder errorResponseBuilder)
  {
    _errorResponseBuilder = errorResponseBuilder;
  }

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public RestLiResponse buildResponse(RoutingResult routingResult,
      RestLiResponseData<BatchFinderResponseEnvelope> responseData)
  {
    BatchFinderResponseEnvelope response = responseData.getResponseEnvelope();

    DataMap dataMap = new DataMap();
    DataList elementsMap = new DataList();
    for (BatchFinderEntry entry : response.getItems())
    {
      CheckedUtil.addWithoutChecking(elementsMap, entry.toResponse(_errorResponseBuilder));
    }
    CheckedUtil.putWithoutChecking(dataMap, CollectionResponse.ELEMENTS, elementsMap);
    BatchCollectionResponse<?> collectionResponse = new BatchCollectionResponse<>(dataMap, null);
    RestLiResponse.Builder builder = new RestLiResponse.Builder();
    return builder.entity(collectionResponse)
                  .headers(responseData.getHeaders())
                  .cookies(responseData.getCookies())
                  .build();
  }

  @Override
  @SuppressWarnings("unchecked")
  public RestLiResponseData<BatchFinderResponseEnvelope> buildRestLiResponseData(Request request,
                                                                                 RoutingResult routingResult,
                                                                                 Object object,
                                                                                 Map<String, String> headers,
                                                                                 List<HttpCookie> cookies)
  {
    BatchFinderResult<RecordTemplate, RecordTemplate, RecordTemplate> result =
        (BatchFinderResult<RecordTemplate, RecordTemplate, RecordTemplate>) object;

    DataList criteriaParams = getCriteriaParameters(routingResult);
    List<BatchFinderEntry> collectionResponse = new ArrayList<>(criteriaParams.size());

    final ResourceContextImpl resourceContext = (ResourceContextImpl) routingResult.getContext();

    TimingContextUtil.beginTiming(routingResult.getContext().getRawRequestContext(),
        FrameworkTimingKeys.SERVER_RESPONSE_RESTLI_PROJECTION_APPLY.key());

    try
    {
      for (Object criteriaParam : criteriaParams.values())
      {
        RecordTemplate criteria = new AnyRecord((DataMap) criteriaParam);
        BatchFinderEntry entry;
        if (result.getResults().containsKey(criteria))
        {
          CollectionResult<RecordTemplate, RecordTemplate> cr = result.getResult(criteria);

          //Process elements
          List<AnyRecord> elements = buildElements(cr, resourceContext);

          //Process paging
          final CollectionMetadata projectedPaging =
              buildPaginationMetaData(routingResult, criteria, resourceContext, request, cr);

          //Process metadata
          final AnyRecord projectedCustomMetadata = buildMetaData(cr, resourceContext);

          entry = new BatchFinderEntry(elements, projectedPaging, projectedCustomMetadata);
        }
        else if (result.getErrors().containsKey(criteria))
        {
          entry = new BatchFinderEntry(result.getErrors().get(criteria));
        }
        else
        {
          entry = new BatchFinderEntry(
              new RestLiServiceException(S_404_NOT_FOUND, "The server didn't find a representation for this criteria"));
        }

        collectionResponse.add(entry);
      }

      TimingContextUtil.endTiming(routingResult.getContext().getRawRequestContext(),
          FrameworkTimingKeys.SERVER_RESPONSE_RESTLI_PROJECTION_APPLY.key());

      return new RestLiResponseDataImpl<>(new BatchFinderResponseEnvelope(HttpStatus.S_200_OK, collectionResponse),
          headers,
          cookies);
    }
    catch (CloneNotSupportedException exception)
    {
      throw new RestLiServiceException(S_500_INTERNAL_SERVER_ERROR, "Batch finder response builder failed when rebuild projection URI");
    }
  }

  private List<AnyRecord> buildElements(CollectionResult<RecordTemplate, RecordTemplate> cr,
                                        ResourceContextImpl resourceContext)
  {
    List<? extends RecordTemplate> elements = cr.getElements();
    List<AnyRecord> response = new ArrayList<>(elements.size());
    for (int j = 0; j < elements.size(); j++)
    {
      response.add(new AnyRecord(RestUtils.projectFields(elements.get(j).data(),
                                                         resourceContext.getProjectionMode(),
                                                         resourceContext.getProjectionMask())));
    }
    return response;
  }

  private CollectionMetadata buildPaginationMetaData(RoutingResult routingResult,
                                                     RecordTemplate criteria,
                                                     ResourceContextImpl resourceContext,
                                                     Request request,
                                                     CollectionResult<RecordTemplate,
                                                     RecordTemplate> cr) throws CloneNotSupportedException
  {
    String batchParameterName = getBatchParameterName(routingResult);
    URI criteriaURI = buildCriteriaURI(resourceContext, criteria, batchParameterName, request.getURI());

    final CollectionMetadata paging = RestUtils.buildMetadata(criteriaURI,
                                                              resourceContext,
                                                              routingResult.getResourceMethod(),
                                                              cr.getElements(),
                                                              cr.getPageIncrement(),
                                                              cr.getTotal());

    return new CollectionMetadata(RestUtils.projectFields(paging.data(),
                                                          ProjectionMode.AUTOMATIC,
                                                          resourceContext.getPagingProjectionMask()));
  }

  private AnyRecord buildMetaData(CollectionResult<RecordTemplate, RecordTemplate> cr,
                                  ResourceContextImpl resourceContext)
  {
    if (cr.getMetadata() != null)
    {
      return new AnyRecord(RestUtils.projectFields(cr.getMetadata().data(),
                                                   resourceContext.getMetadataProjectionMode(),
                                                   resourceContext.getMetadataProjectionMask()));
    }

    return null;
  }

  private String getBatchParameterName(RoutingResult routingResult)
  {
    int batchFinderCriteriaIndex = routingResult.getResourceMethod().getBatchFinderCriteriaParamIndex();
    return routingResult.getResourceMethod().getParameters().get(batchFinderCriteriaIndex).getName();
  }

  private DataList getCriteriaParameters(RoutingResult routingResult)
  {
    String batchParameterName = getBatchParameterName(routingResult);
    return(DataList)routingResult.getContext().getStructuredParameter(batchParameterName);
  }

  protected static URI buildCriteriaURI(ResourceContextImpl resourceContext, RecordTemplate criteria, String batchParameterName, URI uri)
      throws CloneNotSupportedException
  {
    DataList criteriaList = new DataList(1);
    criteriaList.add(criteria.data());
    DataMap queryParams = extractQueryParamsFromResourceContext(resourceContext);
    return URIParamUtils.replaceQueryParam(uri,
                                           batchParameterName,
                                           criteriaList,
                                           queryParams,
                                           resourceContext.getRestliProtocolVersion());
  }

  protected static DataMap extractQueryParamsFromResourceContext(ResourceContextImpl resourceContext)
      throws CloneNotSupportedException
  {
    DataMap queryParams = resourceContext.getParameters().clone();
    if (queryParams.containsKey(RestConstants.FIELDS_PARAM))
    {
      queryParams.put(RestConstants.FIELDS_PARAM, resourceContext.getProjectionMask().getDataMap());
    }
    if (queryParams.containsKey(RestConstants.PAGING_FIELDS_PARAM))
    {
      queryParams.put(RestConstants.PAGING_FIELDS_PARAM, resourceContext.getPagingProjectionMask().getDataMap());
    }
    if (queryParams.containsKey(RestConstants.METADATA_FIELDS_PARAM))
    {
      queryParams.put(RestConstants.METADATA_FIELDS_PARAM, resourceContext.getMetadataProjectionMask().getDataMap());
    }
    return queryParams;
  }
}
