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

package com.linkedin.restli.docgen;


import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.resolver.ClassNameDataSchemaResolver;
import com.linkedin.jersey.api.uri.UriBuilder;
import com.linkedin.jersey.api.uri.UriComponent;
import com.linkedin.jersey.core.util.MultivaluedMap;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.restli.common.HttpMethod;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.server.RestLiConfig;
import com.linkedin.restli.server.RestLiDocumentationRequestHandler;
import com.linkedin.restli.server.RoutingException;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * Default {@link RestLiDocumentationRequestHandler} that serves both HTML and JSON documentation.
 *
 * @author Keren Jin
 */
public class DefaultDocumentationRequestHandler implements RestLiDocumentationRequestHandler
{
  @Override
  public void initialize(RestLiConfig config, Map<String, ResourceModel> rootResources)
  {
    final DataSchemaResolver schemaResolver = new ClassNameDataSchemaResolver();
    final ResourceSchemaCollection resourceSchemas = ResourceSchemaCollection.loadOrCreateResourceSchema(rootResources);
    final RestLiResourceRelationship relationships = new RestLiResourceRelationship(resourceSchemas, schemaResolver);

    _htmlRenderer = new RestLiHTMLDocumentationRenderer(config.getServerNodeUri(),
                                                        relationships,
                                                        new VelocityTemplatingEngine(),
                                                        schemaResolver);
    _jsonRenderer = new RestLiJSONDocumentationRenderer(relationships);
  }

  @Override
  public boolean isDocumentationRequest(Request request)
  {
    final String path = request.getURI().getRawPath();
    final List<UriComponent.PathSegment> pathSegments = UriComponent.decodePath(path, true);
    return (pathSegments.size() > 2 &&
                DOC_PREFIX.equals(pathSegments.get(1).getPath()) &&
                DOC_VIEW_DOCS_ACTION.equals(pathSegments.get(2).getPath())) ||
            HttpMethod.valueOf(request.getMethod()) == HttpMethod.OPTIONS;
  }

  @Override
  @SuppressWarnings("fallthrough")
  public RestResponse processDocumentationRequest(Request request)
  {
    final String path = request.getURI().getRawPath();
    final List<UriComponent.PathSegment> pathSegments = UriComponent.decodePath(path, true);

    String prefixSegment = null;
    String actionSegment = null;
    String typeSegment = null;
    String objectSegment = null;

    switch (pathSegments.size())
    {
      case 5:
        objectSegment = pathSegments.get(4).getPath();
      case 4:
        typeSegment = pathSegments.get(3).getPath();
      case 3:
        actionSegment = pathSegments.get(2).getPath();
      case 2:
        prefixSegment = pathSegments.get(1).getPath();
    }

    assert(prefixSegment.equals(DOC_PREFIX) || (HttpMethod.valueOf(request.getMethod()) == HttpMethod.OPTIONS));

    final ByteArrayOutputStream out = new ByteArrayOutputStream(BAOS_BUFFER_SIZE);
    final RestLiDocumentationRenderer renderer;

    if (HttpMethod.valueOf(request.getMethod()) == HttpMethod.OPTIONS)
    {
      renderer = _jsonRenderer;
      renderer.renderResource(prefixSegment, out);
    }
    else if (HttpMethod.valueOf(request.getMethod()) == HttpMethod.GET)
    {
      if (!DOC_VIEW_DOCS_ACTION.equals(actionSegment))
      {
        throw createRoutingError(path);
      }

      final MultivaluedMap queryMap = UriComponent.decodeQuery(request.getURI().getQuery(), false);
      final List<String> formatList = queryMap.get("format");
      if (formatList == null)
      {
        renderer = _htmlRenderer;
      }
      else if (formatList.size() > 1)
      {
        throw new RoutingException(
            String.format("\"format\" query parameter must be unique, where multiple are specified: %s",
            Arrays.toString(formatList.toArray())),
            HttpStatus.S_400_BAD_REQUEST.getCode());
      }
      else
      {
        renderer = (formatList.contains(DOC_JSON_FORMAT) ? _jsonRenderer : _htmlRenderer);
      }

      if (renderer == _htmlRenderer)
      {
        _htmlRenderer.setJsonFormatUri(UriBuilder.fromUri(request.getURI())
                                                 .queryParam("format", DOC_JSON_FORMAT)
                                                 .build());
      }

      try
      {
        if (typeSegment == null || typeSegment.isEmpty())
        {
          renderer.renderHome(out);
        }
        else
        {
          if (DOC_RESOURCE_TYPE.equals(typeSegment))
          {
            if (objectSegment == null || objectSegment.isEmpty())
            {
              renderer.renderResourceHome(out);
            }
            else
            {
              renderer.renderResource(objectSegment, out);
            }
          }
          else if (DOC_DATA_TYPE.equals(typeSegment))
          {
            if (objectSegment == null || objectSegment.isEmpty())
            {
              renderer.renderDataModelHome(out);
            }
            else
            {
              renderer.renderDataModel(objectSegment, out);
            }
          }
          else
          {
            throw createRoutingError(path);
          }
        }
      }
      catch (RuntimeException e)
      {
        if (!renderer.handleException(e, out))
        {
          throw e;
        }
      }
    }
    else
    {
      throw new RoutingException(HttpStatus.S_405_METHOD_NOT_ALLOWED.getCode());
    }

    return new RestResponseBuilder().
           setStatus(HttpStatus.S_200_OK.getCode()).
           setHeader(RestConstants.HEADER_CONTENT_TYPE, renderer.getMIMEType()).
           setEntity(out.toByteArray()).
           build();
  }

  private static RoutingException createRoutingError(String path)
  {
    return new RoutingException(String.format("Invalid documentation path %s", path), HttpStatus.S_404_NOT_FOUND.getCode());
  }

  private static final String DOC_PREFIX = "restli";
  private static final String DOC_VIEW_DOCS_ACTION = "docs";
  private static final String DOC_RESOURCE_TYPE = "rest";
  private static final String DOC_DATA_TYPE = "data";
  private static final String DOC_JSON_FORMAT = "json";
  private static final int BAOS_BUFFER_SIZE = 8192;

  private RestLiHTMLDocumentationRenderer _htmlRenderer;
  private RestLiJSONDocumentationRenderer _jsonRenderer;
}
