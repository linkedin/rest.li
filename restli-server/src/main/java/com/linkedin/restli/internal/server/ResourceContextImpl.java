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

package com.linkedin.restli.internal.server;


import com.linkedin.data.ByteString;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.template.StringArray;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.jersey.api.uri.UriComponent;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.entitystream.EntityStream;
import com.linkedin.r2.message.timing.TimingContextUtil;
import com.linkedin.r2.message.timing.TimingKey;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.attachments.RestLiAttachmentReader;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.CookieUtil;
import com.linkedin.restli.internal.common.PathSegment.PathSegmentSyntaxException;
import com.linkedin.restli.internal.common.ProtocolVersionUtil;
import com.linkedin.restli.internal.common.QueryParamsDataMap;
import com.linkedin.restli.internal.common.URIParamUtils;
import com.linkedin.restli.internal.server.util.ArgumentUtils;
import com.linkedin.restli.internal.server.util.MIMEParse;
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
import java.util.Optional;
import java.util.TreeMap;


/**
 * @author Josh Walker
 * @version $Revision: $
 */
public class ResourceContextImpl implements ServerResourceContext
{
  // Capacity base on guessumption that custom data count in most cases is either zero or one
  private static final int INITIAL_CUSTOM_REQUEST_CONTEXT_CAPACITY = 1;

  // Timing key to track the duration of Rest.li 2.0.0 URI parsing
  private static final TimingKey URI_PARSE_2_0_0_TIMING_KEY = TimingKey.registerNewKey(ResourceContextImpl.class.getSimpleName() + ".UriParse_2.0.0");

  private final MutablePathKeys                     _pathKeys;
  private final Request                             _request;
  private final DataMap                             _parameters;
  private final Map<String, String>                 _requestHeaders;
  private final Map<String, String>                 _responseHeaders;
  private final List<HttpCookie>                    _requestCookies;
  private final List<HttpCookie>                    _responseCookies;
  private final Map<Object, RestLiServiceException> _batchKeyErrors;
  private final RequestContext                      _requestContext;
  private final ProtocolVersion                     _protocolVersion;
  private String                                    _requestMimeType;
  private String                                    _responseMimeType;

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
  private RestLiAttachmentReader                    _requestAttachmentReader;
  private final boolean                             _responseAttachmentsAllowed;
  private RestLiResponseAttachments                 _responseStreamingAttachments;

  //Data map to store custom request context data
  private Map<String, Object>                       _customRequestContext;

  // Response entity stream
  private EntityStream<ByteString> _responseEntityStream;
  // Request entity stream
  private EntityStream<ByteString> _requestEntityStream;

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
                             final Request request,
                             final RequestContext requestContext) throws RestLiSyntaxException
  {
    _pathKeys = pathKeys;
    _request = request;
    _requestHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    _requestHeaders.putAll(request.getHeaders());
    _responseHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    _requestCookies = new ArrayList<>(CookieUtil.decodeCookies(_request.getCookies()));
    _responseCookies = new ArrayList<>();
    _requestContext = requestContext;
    _responseAttachmentsAllowed = isResponseAttachmentsAllowed(request);

    _protocolVersion = ProtocolVersionUtil.extractProtocolVersion(request.getHeaders());

    try
    {
      if (_protocolVersion.compareTo(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion()) >= 0)
      {
        TimingContextUtil.markTiming(requestContext, URI_PARSE_2_0_0_TIMING_KEY);
        Map<String, List<String>> queryParameters = UriComponent.decodeQuery(_request.getURI(), false);
        _parameters = URIParamUtils.parseUriParams(queryParameters);
        TimingContextUtil.markTiming(requestContext, URI_PARSE_2_0_0_TIMING_KEY);
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
      _projectionMask = ArgumentUtils.parseProjectionParameter(getParameter(RestConstants.FIELDS_PARAM));
    }
    else
    {
      _projectionMask = null;
    }

    if (_parameters.containsKey(RestConstants.METADATA_FIELDS_PARAM))
    {
      _metadataProjectionMask = ArgumentUtils.parseProjectionParameter(getParameter(RestConstants.METADATA_FIELDS_PARAM));
    }
    else
    {
      _metadataProjectionMask = null;
    }

    if (_parameters.containsKey(RestConstants.PAGING_FIELDS_PARAM))
    {
      _pagingProjectionMask = ArgumentUtils.parseProjectionParameter(getParameter(RestConstants.PAGING_FIELDS_PARAM));
    }
    else
    {
      _pagingProjectionMask = null;
    }

    _batchKeyErrors = new HashMap<>();

    _projectionMode = ProjectionMode.getDefault();
    _metadataProjectionMode = ProjectionMode.getDefault();
  }

  private static boolean isResponseAttachmentsAllowed(Request request)
  {
    final String acceptTypeHeader = request.getHeader(RestConstants.HEADER_ACCEPT);
    if (acceptTypeHeader != null)
    {
      final List<String> acceptTypes = MIMEParse.parseAcceptType(acceptTypeHeader);
      for (final String acceptType : acceptTypes)
      {
        if (acceptType.equalsIgnoreCase(RestConstants.HEADER_VALUE_MULTIPART_RELATED))
        {
          return true;
        }
      }
    }
    return false;
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
    return getParameter(RestConstants.ACTION_PARAM);
  }

  @Override
  public String getRequestFinderName()
  {
    return getParameter(RestConstants.QUERY_TYPE_PARAM);
  }

  @Override
  public String getRequestBatchFinderName()
  {
    return getParameter(RestConstants.BATCH_FINDER_QUERY_TYPE_PARAM);
  }

  @Override
  public String getMethodName()
  {
    String methodName = getRequestActionName();
    if (methodName == null)
    {
      methodName = getRequestFinderName();
    }
    if (methodName == null)
    {
      methodName = getRequestBatchFinderName();
    }

    return methodName;
  }

  @Override
  public String getMethodName(ResourceMethod type)
  {
    if (type.equals(ResourceMethod.ACTION))
    {
      return getRequestActionName();
    }
    else if (type.equals(ResourceMethod.FINDER))
    {
      return getRequestFinderName();
    }
    else if (type.equals(ResourceMethod.BATCH_FINDER))
    {
      return getRequestBatchFinderName();
    }

    return null;
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
    if (_request instanceof RestRequest)
    {
      return (RestRequest) _request;
    }

    // The content of the entity stream is not copied to the RestRequest. Reading the content is challenging because the entity
    // stream can only be read once. However, this is acceptable in this deprecated method because no application
    // depends on the entity content.
    return new RestRequestBuilder((StreamRequest) _request).build();
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
  public void setRequestMimeType(String type)
  {
    _requestMimeType = type;
  }

  @Override
  public String getRequestMimeType()
  {
    return _requestMimeType;
  }

  @Override
  public void setResponseMimeType(String type)
  {
    _responseMimeType = type;
  }

  @Override
  public String getResponseMimeType()
  {
    return _responseMimeType;
  }

  @Override
  public boolean responseAttachmentsSupported()
  {
    return _responseAttachmentsAllowed;
  }

  @Override
  public void setRequestAttachmentReader(RestLiAttachmentReader requestAttachmentReader)
  {
    _requestAttachmentReader = requestAttachmentReader;
  }

  @Override
  public RestLiAttachmentReader getRequestAttachmentReader()
  {
    return _requestAttachmentReader;
  }

  @Override
  public void setResponseEntityStream(EntityStream<ByteString> entityStream)
  {
    _responseEntityStream = entityStream;
  }

  @Override
  public EntityStream<ByteString> getResponseEntityStream()
  {
    return _responseEntityStream;
  }

  @Override
  public void setRequestEntityStream(EntityStream<ByteString> entityStream) {
    _requestEntityStream = entityStream;
  }

  @Override
  public EntityStream<ByteString> getRequestEntityStream() {
    return _requestEntityStream;
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

  @Override
  public boolean shouldReturnEntity()
  {
    String returnEntityValue = getParameter(RestConstants.RETURN_ENTITY_PARAM);
    if (returnEntityValue == null)
    {
      // Default to true for backward compatibility so that existing clients can receive entity without using parameter
      return true;
    }
    return ArgumentUtils.parseReturnEntityParameter(returnEntityValue);
  }

  @Override
  public Optional<Object> getCustomContextData(String key)
  {
    if (_customRequestContext != null && key != null && !key.isEmpty() && _customRequestContext.containsKey(key))
    {
      return Optional.of(_customRequestContext.get(key));
    }
    return Optional.empty();
  }

  @Override
  public void putCustomContextData(String key, Object data)
  {
    if (key != null && !key.isEmpty() && data != null)
    {
      if (_customRequestContext == null)
      {
        _customRequestContext = new HashMap<>(INITIAL_CUSTOM_REQUEST_CONTEXT_CAPACITY);
      }
      _customRequestContext.put(key, data);
    }
  }

  @Override
  public Optional<Object> removeCustomContextData(String key)
  {
    return getCustomContextData(key).isPresent() ? Optional.of(_customRequestContext.remove(key)) : Optional.empty();
  }
}
