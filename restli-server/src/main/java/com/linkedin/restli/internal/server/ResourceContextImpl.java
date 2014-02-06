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
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.PathSegment.PathSegmentSyntaxException;
import com.linkedin.restli.internal.common.QueryParamsDataMap;
import com.linkedin.restli.internal.server.util.ArgumentUtils;
import com.linkedin.restli.internal.server.util.RestLiSyntaxException;
import com.linkedin.restli.server.ProjectionMode;
import com.linkedin.restli.server.RestLiServiceException;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class ResourceContextImpl implements ServerResourceContext
{
  private final MutablePathKeys                     _pathKeys;
  private final RestRequest                         _request;
  private final DataMap                             _parameters;
  private final MaskTree                            _projectionMask;
  private final Map<String, String>                 _responseHeaders;
  private final Map<String, RestLiServiceException> _batchKeyErrors;
  private final RequestContext                      _requestContext;
  private final ProtocolVersion                     _protocolVersion;

  private ProjectionMode                      _projectionMode;

  private String                                    _mimeType;

  /**
   * Default constructor.
   *
   * @throws RestLiSyntaxException cannot happen here
   */
  public ResourceContextImpl() throws RestLiSyntaxException
  {
    this(new PathKeysImpl(), null, new RequestContext());
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
  public ResourceContextImpl(final MutablePathKeys pathKeys, final RestRequest request,
                             final RequestContext requestContext) throws
          RestLiSyntaxException
  {
    _pathKeys = pathKeys;
    _request = request;
    _requestContext = requestContext;

    Map<String, List<String>> queryParameters =
        ArgumentUtils.getQueryParameters(_request != null ? _request.getURI()
            : URI.create(""));
    try
    {
      _parameters = QueryParamsDataMap.parseDataMapKeys(queryParameters);
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
    _responseHeaders = new HashMap<String, String>();
    _batchKeyErrors = new HashMap<String, RestLiServiceException>();

    _projectionMode = ProjectionMode.getDefault();

    if (request != null)
    {
      String protocolVersionString = request.getHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION);
      if (protocolVersionString != null)
      {
        _protocolVersion = new ProtocolVersion(protocolVersionString);
      }
      else
      {
        _protocolVersion = AllProtocolVersions.DEFAULT_PROTOCOL_VERSION;
      }
    }
    else
    {
      _protocolVersion = AllProtocolVersions.DEFAULT_PROTOCOL_VERSION;
    }

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
    return ArgumentUtils.argumentAsString(getParameter(RestConstants.ACTION_PARAM),
                                          RestConstants.ACTION_PARAM);
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

    if (paramObject instanceof String)
    {
      return Collections.singletonList((String) paramObject);
    }

    if (!(paramObject instanceof DataList))
    {
      throw new RestLiInternalException("Invalid value type for parameter " + key);
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
    return _request.getHeaders();
  }

  @Override
  public void setResponseHeader(final String name, final String value)
  {
    _responseHeaders.put(name, value);
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
  public Map<String, RestLiServiceException> getBatchKeyErrors()
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
  public void setResponseMimeType(String type)
  {
    _mimeType = type;
  }

  @Override
  public String getResponseMimeType()
  {
    return _mimeType;
  }
}
