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

package com.linkedin.restli.internal.server;


import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.template.StringArray;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.jersey.api.uri.UriComponent;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.attachments.RestLiAttachmentReader;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.CookieUtil;
import com.linkedin.restli.internal.common.PathSegment.PathSegmentSyntaxException;
import com.linkedin.restli.internal.common.ProtocolVersionUtil;
import com.linkedin.restli.internal.common.QueryParamsDataMap;
import com.linkedin.restli.internal.common.URIParamUtils;
import com.linkedin.restli.internal.server.util.ArgumentUtils;
import com.linkedin.restli.internal.server.util.RestLiSyntaxException;
import com.linkedin.restli.server.ProjectionMode;
import com.linkedin.restli.server.RestLiResponseAttachments;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.RoutingException;

import java.net.HttpCookie;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * @author Josh Walker
 * @version $Revision: $
 */
public class ResourceContextImpl implements ServerResourceContext
{
  private final MutablePathKeys                     _pathKeys;
  private final RestRequest                         _request;
  private final DataMap                             _parameters;
  private final Map<String, String>                 _requestHeaders;
  private final Map<String, String>                 _responseHeaders;
  private final List<HttpCookie>                    _requestCookies;
  private final List<HttpCookie>                    _responseCookies;
  private final Map<Object, RestLiServiceException> _batchKeyErrors;
  private final RequestContext                      _requestContext;
  private final ProtocolVersion                     _protocolVersion;
  private String                                    _mimeType;

  //For root object entities
  private ProjectionMode                            _projectionMode;
  private MaskTree                                  _projectionMask;

  //For the metadata inside of a CollectionResult
  private ProjectionMode                            _metadataProjectionMode;
  private MaskTree                                  _metadataProjectionMask;

  //For paging. Note that there is no projection mode for paging (CollectionMetadata) because its fully automatic.
  //Client resource methods have the option of setting the total if they so desire, but restli will always
  //project CollectionMetadata if the client asks for it.
  //The paging projection mask is still available to both parties (the resource method and restli).
  private MaskTree                                  _pagingProjectionMask;

  //For streaming attachments
  private final RestLiAttachmentReader              _requestAttachmentReader;
  private final boolean                             _responseAttachmentsAllowed;
  private RestLiResponseAttachments                 _responseStreamingAttachments;

  /**
   * Default constructor.
   *
   * @throws RestLiSyntaxException cannot happen here
   */
  public ResourceContextImpl() throws RestLiSyntaxException
  {
    this(new PathKeysImpl(),
         new RestRequestBuilder(URI.create(""))
             .setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, AllProtocolVersions.LATEST_PROTOCOL_VERSION.toString())
             .build(),
         new RequestContext());
  }

  /**
   * Constructor.
   *
   * @param pathKeys path keys object
   * @param request request
   * @param requestContext context for the request
   * @throws RestLiSyntaxException if the syntax of query parameters in the request is
   *           incorrect
   */
  public ResourceContextImpl(final MutablePathKeys pathKeys,
                             final RestRequest request,
                             final RequestContext requestContext) throws RestLiSyntaxException
  {
    this(pathKeys, request, requestContext, false, null);
  }

  public ResourceContextImpl(final MutablePathKeys pathKeys,
                             final RestRequest request,
                             final RequestContext requestContext,
                             final boolean responseAttachmentsAllowed,
                             final RestLiAttachmentReader restLiAttachmentReader) throws RestLiSyntaxException
  {
    _pathKeys = pathKeys;
    _request = request;
    _requestHeaders = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
    _requestHeaders.putAll(request.getHeaders());
    _responseHeaders = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
    _requestCookies = new ArrayList<HttpCookie>(CookieUtil.decodeCookies(_request.getCookies()));
    _responseCookies = new ArrayList<HttpCookie>();
    _requestContext = requestContext;
    _responseAttachmentsAllowed = responseAttachmentsAllowed;
    _requestAttachmentReader = restLiAttachmentReader;

    _protocolVersion = ProtocolVersionUtil.extractProtocolVersion(request.getHeaders());

    try
    {
      if (_protocolVersion.compareTo(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion()) >= 0)
      {
        Map<String, List<String>> queryParameters = UriComponent.decodeQuery(_request.getURI(), false);
        _parameters = URIParamUtils.parseUriParams(queryParameters);
      }
      else
      {
        Map<String, List<String>> queryParameters = ArgumentUtils.getQueryParameters(_request.getURI());
        _parameters = QueryParamsDataMap.parseDataMapKeys(queryParameters);
      }
    }
    catch (PathSegmentSyntaxException e)
    {
      throw new RestLiSyntaxException("Invalid query parameters syntax: "
          + _request.getURI().toString(), e);
    }

    if (_parameters.containsKey(RestConstants.FIELDS_PARAM))
    {
      _projectionMask =
          ArgumentUtils.parseProjectionParameter(ArgumentUtils.argumentAsString(getParameter(RestConstants.FIELDS_PARAM),
                                                                                RestConstants.FIELDS_PARAM));
    }
    else
    {
      _projectionMask = null;
    }

    if (_parameters.containsKey(RestConstants.METADATA_FIELDS_PARAM))
    {
      _metadataProjectionMask = ArgumentUtils.parseProjectionParameter(ArgumentUtils
          .argumentAsString(getParameter(RestConstants.METADATA_FIELDS_PARAM), RestConstants.METADATA_FIELDS_PARAM));
    }
    else
    {
      _metadataProjectionMask = null;
    }

    if (_parameters.containsKey(RestConstants.PAGING_FIELDS_PARAM))
    {
      _pagingProjectionMask = ArgumentUtils.parseProjectionParameter(ArgumentUtils
          .argumentAsString(getParameter(RestConstants.PAGING_FIELDS_PARAM), RestConstants.PAGING_FIELDS_PARAM));
    }
    else
    {
      _pagingProjectionMask = null;
    }

    _batchKeyErrors = new HashMap<Object, RestLiServiceException>();

    _projectionMode = ProjectionMode.getDefault();
    _metadataProjectionMode = ProjectionMode.getDefault();
  }

  @Override
  public DataMap getParameters()
  {
    return _parameters;
  }

  @Override
  public URI getRequestURI()
  {
    return _request.getURI();
  }

  @Override
  public String getRequestActionName()
  {
    return ArgumentUtils.argumentAsString(getParameter(RestConstants.ACTION_PARAM), RestConstants.ACTION_PARAM);
  }

  @Override
  public String getRequestFinderName()
  {
    return ArgumentUtils.argumentAsString(getParameter(RestConstants.QUERY_TYPE_PARAM),
                                          RestConstants.QUERY_TYPE_PARAM);
  }

  @Override
  public String getRequestMethod()
  {
    return _request.getMethod();
  }

  @Override
  public MutablePathKeys getPathKeys()
  {
    return _pathKeys;
  }

  @Override
  @Deprecated
  public RestRequest getRawRequest()
  {
    return _request;
  }

  @Override
  public MaskTree getProjectionMask()
  {
    return _projectionMask;
  }

  @Override
  public void setProjectionMask(MaskTree projectionMask)
  {
    _projectionMask = projectionMask;
  }

  @Override
  public MaskTree getMetadataProjectionMask() {
    return _metadataProjectionMask;
  }

  @Override
  public void setMetadataProjectionMask(MaskTree metadataProjectionMask)
  {
    _metadataProjectionMask = metadataProjectionMask;
  }

  @Override
  public MaskTree getPagingProjectionMask() {
    return _pagingProjectionMask;
  }

  @Override
  public void setPagingProjectionMask(MaskTree pagingProjectionMask)
  {
    _pagingProjectionMask = pagingProjectionMask;
  }

  @Override
  public String getParameter(final String key)
  {
    Object paramValueObj = _parameters.get(key);
    if (paramValueObj == null)
    {
      return null;
    }

    if (paramValueObj instanceof List)
    {
      List<?> paramValueList = (List<?>) paramValueObj;
      if (paramValueList.isEmpty())
      {
        return null;
      }
      return paramValueList.get(0).toString();
    }
    return paramValueObj.toString();
  }

  @Override
  public Object getStructuredParameter(final String key)
  {
    return _parameters.get(key);
  }

  /*
   * This method is only applicable for "simple", i.e. non-RecordTemplate-based query
   * parameters. For backwards compatibility return List<String> but make sure the
   * parameter conforms to this type.
   */
  @Override
  public List<String> getParameterValues(final String key)
  {
    Object paramObject = _parameters.get(key);
    if (paramObject == null)
    {
      return null;
    }

    if (paramObject instanceof String && _protocolVersion.compareTo(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion()) == 0)
    {
      return Collections.singletonList((String) paramObject);
    }

    if (!(paramObject instanceof DataList))
    {
      throw new RoutingException("Invalid value type for parameter " + key,  HttpStatus.S_400_BAD_REQUEST.getCode());
    }

    return new StringArray((DataList) paramObject);
  }

  @Override
  public boolean hasParameter(final String key)
  {
    return _parameters.containsKey(key);
  }

  @Override
  public Map<String, String> getRequestHeaders()
  {
    return _requestHeaders;
  }

  @Override
  public List<HttpCookie> getRequestCookies()
  {
    return _requestCookies;
  }

  /**
   * @throws IllegalArgumentException when trying to set {@link RestConstants#HEADER_ID} or {@link RestConstants#HEADER_RESTLI_ID}.
   */
  @Override
  public void setResponseHeader(final String name, final String value)
  {
    final String headerName;
    if (RestConstants.HEADER_ID.equals(name))
    {
      headerName = RestConstants.HEADER_ID;
    }
    else if (RestConstants.HEADER_RESTLI_ID.equals(name))
    {
      headerName = RestConstants.HEADER_RESTLI_ID;
    }
    else
    {
      headerName = null;
    }

    if (headerName != null)
    {
      throw new IllegalArgumentException("Illegal to set the \"" + headerName + "\" header. This header is reserved for the ID returned from create method on the resource.");
    }

    _responseHeaders.put(name, value);
  }

  @Override
  public void addResponseCookie(HttpCookie cookie)
  {
    if (cookie != null)
      _responseCookies.add(cookie);
  }

  @Override
  public RequestContext getRawRequestContext()
  {
    return _requestContext;
  }

  @Override
  public Map<String, String> getResponseHeaders()
  {
    return Collections.unmodifiableMap(_responseHeaders);
  }

  @Override
  public List<HttpCookie> getResponseCookies()
  {
    return _responseCookies;
  }

  @Override
  public Map<Object, RestLiServiceException> getBatchKeyErrors()
  {
    return _batchKeyErrors;
  }

  @Override
  public String getRestLiRequestMethod()
  {
    String headerValue = _request.getHeader(RestConstants.HEADER_RESTLI_REQUEST_METHOD);
    return headerValue == null ? "" : headerValue;
  }

  @Override
  public ProtocolVersion getRestliProtocolVersion()
  {
    return _protocolVersion;
  }

  @Override
  public ProjectionMode getProjectionMode()
  {
    return _projectionMode;
  }

  @Override
  public void setProjectionMode(ProjectionMode projectionMode)
  {
    _projectionMode = projectionMode;
  }

  @Override
  public ProjectionMode getMetadataProjectionMode()
  {
    return _metadataProjectionMode;
  }

  @Override
  public void setMetadataProjectionMode(ProjectionMode metadataProjectionMode)
  {
    _metadataProjectionMode = metadataProjectionMode;
  }

  @Override
  public void setResponseMimeType(String type)
  {
    _mimeType = type;
  }

  @Override
  public String getResponseMimeType()
  {
    return _mimeType;
  }

  @Override
  public boolean responseAttachmentsSupported()
  {
    return _responseAttachmentsAllowed;
  }

  @Override
  public RestLiAttachmentReader getRequestAttachmentReader()
  {
    return _requestAttachmentReader;
  }

  @Override
  public void setResponseAttachments(final RestLiResponseAttachments responseAttachments) throws IllegalStateException
  {
    if (!_responseAttachmentsAllowed)
    {
      throw new IllegalStateException("Response attachments can only be set if the client request indicates permissibility");
    }
    _responseStreamingAttachments = responseAttachments;
  }

  @Override
  public RestLiResponseAttachments getResponseAttachments()
  {
    return _responseStreamingAttachments;
  }
}
