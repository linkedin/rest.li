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

package com.linkedin.restli.internal.server.util;


import com.linkedin.data.DataMap;
import com.linkedin.data.collections.CheckedUtil;
import com.linkedin.data.element.DataElement;
import com.linkedin.data.it.Builder;
import com.linkedin.data.it.IterationOrder;
import com.linkedin.data.it.Predicate;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.transform.filter.CopyFilter;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.jersey.api.uri.UriBuilder;
import com.linkedin.restli.common.CollectionMetadata;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.Link;
import com.linkedin.restli.common.LinkArray;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.server.RestLiInternalException;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.model.Parameter;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.server.CollectionResult.PageIncrement;
import com.linkedin.restli.server.InvalidMimeTypeException;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.ProjectionMode;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.RoutingException;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang.StringUtils;


/**
 * @author dellamag
 */
public class RestUtils
{
  public static CollectionMetadata buildMetadata(final URI requestUri,
                                                 final ResourceContext resourceContext,
                                                 final ResourceMethodDescriptor methodDescriptor,
                                                 final List<?> resultElements,
                                                 final PageIncrement pageIncrement,
                                                 final Integer totalResults)
  {
    CollectionMetadata metadata = new CollectionMetadata();

    List<Parameter<?>> pagingContextParams =
        methodDescriptor.getParametersWithType(Parameter.ParamType.PAGING_CONTEXT_PARAM);
    PagingContext defaultPagingContext =
        pagingContextParams.isEmpty() ? null
            : (PagingContext) pagingContextParams.get(0).getDefaultValue();
    PagingContext pagingContext = getPagingContext(resourceContext, defaultPagingContext);

    metadata.setCount(pagingContext.getCount());
    metadata.setStart(pagingContext.getStart());

    if (totalResults != null)
    {
      metadata.setTotal(totalResults);
    }
    else
    {
      metadata.removeTotal();
    }

    LinkArray links = new LinkArray();

    String bestEncoding = pickBestEncoding(resourceContext.getRequestHeaders().get(RestConstants.HEADER_ACCEPT), Collections.emptySet());

    //links use count as the step interval, so links don't make sense with count==0
    if (pagingContext.getCount() > 0)
    {
      // prev link
      if (pagingContext.getStart() > 0)
      {
        int prevStart = Math.max(0, pagingContext.getStart() - pagingContext.getCount());
        String prevUri =
            buildPaginatedUri(requestUri, prevStart, pagingContext.getCount());
        Link prevLink = new Link();
        prevLink.setRel("prev");
        prevLink.setHref(prevUri);
        prevLink.setType(bestEncoding);
        links.add(prevLink);
      }

      // next link if there are more results, or we returned a full page
      Integer nextStart = getNextPageStart(resultElements.size(), totalResults, pagingContext, pageIncrement);
      if (nextStart != null)
      {
        // R2 doesn't expose host/port => can't build absolute URI (this is ok, as
        // relative URIs internally
        String nextUri =
            buildPaginatedUri(requestUri, nextStart, pagingContext.getCount());
        Link nextLink = new Link();
        nextLink.setRel("next");
        nextLink.setHref(nextUri);
        nextLink.setType(bestEncoding);
        links.add(nextLink);
      }

      metadata.setLinks(links);
    }
    return metadata;
  }

  private static Integer getNextPageStart(int elementsOnCurrentPage,
                                          final Integer totalResults,
                                          PagingContext pagingContext,
                                          final PageIncrement pageIncrement)
  {
    if(pageIncrement == PageIncrement.RELATIVE)
    {
      if ((totalResults != null && (pagingContext.getStart() + elementsOnCurrentPage < totalResults))
          || (elementsOnCurrentPage == pagingContext.getCount()))
      {
        return pagingContext.getStart() + elementsOnCurrentPage;
      }
      else
      {
        return null; // no next page
      }
    }
    else if (pageIncrement == PageIncrement.FIXED)
    {
      if(totalResults == null) throw new IllegalStateException("'total' must be non-null when PageIncrement is FIXED"); // this is also checked by the CollectionResult constructor
      if(pagingContext.getStart() + pagingContext.getCount() < totalResults)
      {
        return pagingContext.getStart() + pagingContext.getCount();
      }
      else
      {
        return null; // no next page
      }
    }
    else
    {
      throw new IllegalStateException("Unrecognized enumeration value: " + pageIncrement);
    }
  }

  private static String buildPaginatedUri(final URI requestUri,
                                          final int start,
                                          final Integer count)
  {
    UriBuilder builder = UriBuilder.fromUri(requestUri);
    builder.replaceQueryParam("start", String.valueOf(start));
    builder.replaceQueryParam("count", String.valueOf(count));

    return builder.build().toString();
  }

  public static PagingContext getPagingContext(final ResourceContext context,
                                               final PagingContext defaultContext)
  {
    String startString = context.getParameter(RestConstants.START_PARAM);
    String countString = context.getParameter(RestConstants.COUNT_PARAM);
    try
    {
      int defaultStart =
          defaultContext == null ? RestConstants.DEFAULT_START
              : defaultContext.getStart();
      int defaultCount =
          defaultContext == null ? RestConstants.DEFAULT_COUNT
              : defaultContext.getCount();

      int start =
          startString == null || StringUtils.isEmpty(startString.trim()) ? defaultStart
              : Integer.parseInt(startString);
      int count =
          countString == null || StringUtils.isEmpty(countString.trim()) ?
              defaultCount : Integer.parseInt(countString);
      if (count < 0 || start < 0)
      {
        throw new RoutingException("start/count parameters must be non-negative", 400);
      }
      return new PagingContext(start, count, startString != null, countString != null);
    }
    catch (NumberFormatException e)
    {
      throw new RoutingException("Invalid (non-integer) start/count parameters", 400);
    }
  }

  public static String pickBestEncoding(String acceptHeader, Set<String> customMimeTypesSupported)
  {
    if (acceptHeader == null || acceptHeader.isEmpty())
    {
      return RestConstants.HEADER_VALUE_APPLICATION_JSON;
    }

    //For backward compatibility reasons, we have to assume that if there is ONLY multipart/related as an accept
    //type that this means to default to JSON.
    try
    {
      final List<String> acceptTypes = MIMEParse.parseAcceptType(acceptHeader);
      if (acceptTypes.size() == 1 && acceptTypes.get(0).equalsIgnoreCase(RestConstants.HEADER_VALUE_MULTIPART_RELATED))
      {
        return RestConstants.HEADER_VALUE_APPLICATION_JSON;
      }

      return MIMEParse.bestMatch(
          Stream.concat(customMimeTypesSupported.stream(), RestConstants.SUPPORTED_MIME_TYPES.stream())
              .collect(Collectors.toList()),
          acceptHeader);
    }

    // Handle the case when an accept MIME type that was passed in along with the
    // request is invalid.
    catch (InvalidMimeTypeException e)
    {
      throw new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST, String
          .format("Encountered invalid MIME type '%s' in accept header.", e.getType()));
    }
  }

  /**
   * Filter input {@link DataMap} by the projection mask from the input.
   * {@link ResourceContext}.
   *
   * @param dataMap {@link DataMap} to filter
   * @param projectionMode {@link ProjectionMode} to decide if restli should project or not
   * @param  projectionMask {@link MaskTree} the mask to use when projecting
   * @return filtered DataMap. Empty one if the projection mask specifies no fields.
   */
  public static DataMap projectFields(final DataMap dataMap, final ProjectionMode projectionMode,
      final MaskTree projectionMask)
  {
    return projectFields(dataMap, projectionMode, projectionMask, Collections.emptySet());
  }

  /**
   * Filter input {@link DataMap} by the projection mask from the resource context {@link ResourceContext}.
   *
   * @param dataMap {@link DataMap} to filter
   * @param resourceContext Resource context from which projection inputs like mask, mode and always included fields
   *                        are obtained.
   * @return filtered DataMap. Empty one if the projection mask specifies no fields.
   */
  public static DataMap projectFields(final DataMap dataMap, final ResourceContext resourceContext)
  {
    return projectFields(dataMap, resourceContext.getProjectionMode(), resourceContext.getProjectionMask(),
        resourceContext.getAlwaysProjectedFields());
  }

  /**
   * Filter input {@link DataMap} by the projection mask from the input.
   * {@link ResourceContext}.
   *
   * @param dataMap {@link DataMap} to filter
   * @param projectionMode {@link ProjectionMode} to decide if restli should project or not
   * @param  projectionMask {@link MaskTree} the mask to use when projecting
   * @param alwaysIncludedFields Set of fields that are always included in the result.
   * @return filtered DataMap. Empty one if the projection mask specifies no fields.
   */
  public static DataMap projectFields(final DataMap dataMap, final ProjectionMode projectionMode,
      final MaskTree projectionMask, Set<String> alwaysIncludedFields)
  {
    if (projectionMode == ProjectionMode.MANUAL)
    {
      return dataMap;
    }

    if (projectionMask == null)
    {
      return dataMap;
    }

    final DataMap filterMap = projectionMask.getDataMap();
    //Special-case: when present, an empty filter and no fields to include by default means the result would be empty.
    if (filterMap.isEmpty() && (alwaysIncludedFields == null || alwaysIncludedFields.isEmpty()))
    {
      return EMPTY_DATAMAP;
    }

    try
    {
      return (DataMap) new CopyFilter(alwaysIncludedFields).filter(dataMap, filterMap);
    }
    catch (Exception e)
    {
      throw new RestLiInternalException("Error projecting fields", e);
    }
  }

  /**
   * Validate request headers.
   *
   * @param headers
   *          Request headers.
   * @throws RestLiServiceException
   *           if any of the headers are invalid.
   */
  public static void validateRequestHeadersAndUpdateResourceContext(final Map<String, String> headers,
                                                                    final Set<String> customMimeTypesSupported,
                                                                    ServerResourceContext resourceContext)
  {
    // Validate whether the accept headers have at least one type that we support.
    // Fail the validation if we will be unable to support the requested accept type.
    String mimeType = pickBestEncoding(headers.get(RestConstants.HEADER_ACCEPT), customMimeTypesSupported);
    if (StringUtils.isEmpty(mimeType))
    {
      throw new RestLiServiceException(HttpStatus.S_406_NOT_ACCEPTABLE,
          "None of the types in the request's 'Accept' header are supported. Supported MIME types are: "
              + RestConstants.SUPPORTED_MIME_TYPES
              + customMimeTypesSupported);
    }
    else
    {
      resourceContext.setResponseMimeType(mimeType);
    }
    // Do other header validation here....
  }

  private static final DataMap EMPTY_DATAMAP = new DataMap();
  static
  {
    EMPTY_DATAMAP.makeReadOnly();
  }

  /**
   * This method recursively removes all values from a RecordTemplate
   * that do not match some field in the schema via an all positive
   * projection generated from the schema unless whitelisted by the
   * override parameter.
   *
   * RecordTemplates with readonly datamaps should not invoke this.
   * An exception will be thrown if this is done.
   *
   * @param recordTemplate represents the RecordTemplate that will be trimmed.
   * @param override represents the MaskTree that defines how fields outside the schema should be preserved.
   * @param failOnMismatch true if an exception should be thrown if there is a data-schema mismatch.
   */
  public static void trimRecordTemplate(RecordTemplate recordTemplate, MaskTree override, final boolean failOnMismatch)
  {
    trimRecordTemplate(recordTemplate.data(), recordTemplate.schema(), override, failOnMismatch);
  }

  /**
   * This method recursively removes all values from a DataMap
   * that do not match some field in the schema via an all positive
   * projection generated from the schema unless whitelisted by the
   * override parameter.
   *
   * Readonly datamaps should not invoke this.
   * An exception will be thrown if this is done.
   *
   * @param dataMap represents the DataMap that will be trimmed.
   * @param schema represents the schema that the datamap should match.
   * @param override represents the MaskTree that defines how fields outside the schema should be preserved.
   * @param failOnMismatch true if an exception should be thrown if there is a data-schema mismatch.
   */
  public static void trimRecordTemplate(DataMap dataMap, RecordDataSchema schema, MaskTree override, final boolean failOnMismatch)
  {
    if (dataMap == null || schema == null) {
      return;
    }

    DataMap overrideResults = RestUtils.projectFields(dataMap, ProjectionMode.AUTOMATIC, override);
    Builder.create(dataMap, schema, IterationOrder.PRE_ORDER).filterBy(new Predicate() {
      @Override
      public boolean evaluate(DataElement element) {
        if (failOnMismatch && element.getSchema() == null) {
          throw new IllegalArgumentException("Schema validation did not succeed: " + element.getValue().toString());
        }

        return element.getSchema() == null;
      }
    }).remove();

    CheckedUtil.putAllWithoutChecking(dataMap, overrideResults);
  }

  /**
    * This method recursively removes all values from RecordTemplate
    * that do not match some field in the schema.
    *
    * Readonly datamaps should not invoke this.
    * An exception will be thrown if this is done.
    *
    * @param dataMap represents the dataMap that will be trimmed.
    * @param failOnMismatch true if an exception should be thrown if there is a data-schema mismatch.
    */
  public static void trimRecordTemplate(DataMap dataMap, RecordDataSchema schema, final boolean failOnMismatch) {
    trimRecordTemplate(dataMap, schema, new MaskTree(), failOnMismatch);
  }

  /**
   * This method recursively removes all values from RecordTemplate
   * that do not match some field in the schema.
   *
   * RecordTemplates with readonly datamaps should not invoke this.
   * An exception will be thrown if this is done.
   *
   * @param recordTemplate represents the RecordTemplate that will be trimmed.
   * @param failOnMismatch true if an exception should be thrown if there is a data-schema mismatch.
   */
  public static void trimRecordTemplate(RecordTemplate recordTemplate, final boolean failOnMismatch)
  {
    if (recordTemplate == null)
    {
      return;
    }
    trimRecordTemplate(recordTemplate.data(), recordTemplate.schema(), failOnMismatch);
  }
}
