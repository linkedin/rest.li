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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.PathSegment.PathSegmentSyntaxException;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.internal.server.util.ArgumentUtils;
import com.linkedin.restli.internal.server.util.RestLiSyntaxException;
import com.linkedin.restli.server.ResourceLevel;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.RoutingException;
import com.linkedin.restli.server.resources.ComplexKeyResource;

/**
 * Navigates the resource hierarchy to find a Resource handler for the a given URI.
 *
 * @author Josh Walker
 */
public class RestLiRouter
{
  private static final Logger log = LoggerFactory.getLogger(RestLiRouter.class);
  private static final Map<ResourceMethodMatchKey, ResourceMethod> _resourceMethodLookup = setupResourceMethodLookup();
  private final Map<String, ResourceModel> _pathRootResourceMap;

  /**
   * Constructor.
   *
   * @param pathRootResourceMap a map of resource root paths to corresponding
   *          {@link ResourceModel}s
   */
  public RestLiRouter(final Map<String, ResourceModel> pathRootResourceMap)
  {
    super();
    _pathRootResourceMap = pathRootResourceMap;
  }

  private static final Pattern SLASH_PATTERN = Pattern.compile(Pattern.quote("/"));

  /**
   * Processes provided {@link RestRequest}.
   *
   * @param req {@link RestRequest}
   * @return {@link RoutingResult}
   */
  public RoutingResult process(final RestRequest req, final RequestContext requestContext)
  {
    String path = req.getURI().getRawPath();
    if (path.length() < 2)
    {
      throw new RoutingException(HttpStatus.S_404_NOT_FOUND.getCode());
    }

    if (path.charAt(0) == '/')
    {
      path = path.substring(1);
    }

    Queue<String> remainingPath =
        new LinkedList<String>(Arrays.asList(SLASH_PATTERN.split(path)));

    String rootPath = "/" + remainingPath.poll();

    ResourceModel currentResource;
    try
    {
      currentResource =
          _pathRootResourceMap.get(URLDecoder.decode(rootPath,
                                                     RestConstants.DEFAULT_CHARSET_NAME));
    }
    catch (UnsupportedEncodingException e)
    {
      throw new RestLiInternalException("UnsupportedEncodingException while trying to decode the root path",
                                        e);
    }
    if (currentResource == null)
    {
      throw new RoutingException(String.format("No root resource defined for path '%s'",
                                               rootPath),
                                 HttpStatus.S_404_NOT_FOUND.getCode());
    }
    ServerResourceContext context;

    try
    {
      context = new ResourceContextImpl(new PathKeysImpl(), req, requestContext);
    }
    catch (RestLiSyntaxException e)
    {
      throw new RoutingException(e.getMessage(), HttpStatus.S_400_BAD_REQUEST.getCode());
    }

    return processResourceTree(currentResource, context, remainingPath);
  }

  private RoutingResult processResourceTree(final ResourceModel resource,
                                            final ServerResourceContext context,
                                            final Queue<String> remainingPath)
  {
    ResourceModel currentResource = resource;

    // iterate through all path segments, simultaneously descending the resource hierarchy
    // and parsing path keys
    // the goal of this loop is to locate the leaf resource, which will be set in
    // currentResource, and to parse the necessary information into the context
    ResourceLevel currentLevel = ResourceLevel.COLLECTION;

    while (remainingPath.peek() != null)
    {
      String currentPathSegment = remainingPath.poll();

      if (currentLevel.equals(ResourceLevel.ENTITY))
      {
        currentResource =
            currentResource.getSubResource(parseSubresourceName(currentPathSegment));
        currentLevel = ResourceLevel.COLLECTION;
      }
      else
      {
        ResourceModel currentCollectionResource = currentResource;
        if (currentResource.getKeys().isEmpty())
        {
          throw new RoutingException(String.format("Path key not supported on resource '%s' for URI '%s'",
                                                   currentResource.getName(),
                                                   context.getRequestURI()),
                                     HttpStatus.S_400_BAD_REQUEST.getCode());
        }
        else if (currentResource.getKeyClass() == ComplexResourceKey.class)
        {
          parseComplexKey(currentResource, context, currentPathSegment);
          currentLevel = ResourceLevel.ENTITY;
        }

        else if (currentResource.getKeyClass() == CompoundKey.class)
        {
          CompoundKey compoundKey =
              parseCompoundKey(currentCollectionResource, context, currentPathSegment);
          if (compoundKey != null
              && compoundKey.getPartKeys().containsAll(currentResource.getKeyNames()))
          {
            // full match on key parts means that we are targeting a unique entity
            currentLevel = ResourceLevel.ENTITY;
          }
        }

        // Must be a simple key then
        else
        {
          parseSimpleKey(currentResource, context, currentPathSegment);
          currentLevel = ResourceLevel.ENTITY;
        }
      }

      if (currentResource == null)
      {
        throw new RoutingException(HttpStatus.S_404_NOT_FOUND.getCode());
      }
    }

    parseBatchKeysParameter(currentResource, context); //now we know the key type, look for batch parameter

    return findMethodDescriptor(currentResource, currentLevel, context);
  }

  /** given path segment, parses subresource name out of it */
  private static String parseSubresourceName(final String pathSegment)
  {
    try
    {
      return URLDecoder.decode(pathSegment, RestConstants.DEFAULT_CHARSET_NAME);
    }
    catch (UnsupportedEncodingException e)
    {
      throw new RestLiInternalException("UnsupportedEncodingException while trying to decode the subresource name", e);
    }
  }

  private RoutingResult findMethodDescriptor(final ResourceModel resource,
                                             final ResourceLevel resourceLevel,
                                             final ServerResourceContext context)
  {
    ResourceMethod type = mapResourceMethod(context, resourceLevel);

    String methodName = context.getRequestActionName();
    if (methodName == null)
    {
      methodName = context.getRequestFinderName();
    }

    ResourceMethodDescriptor methodDescriptor = resource.matchMethod(type, methodName, resourceLevel);

    if (methodDescriptor != null)
    {
      return new RoutingResult(context, methodDescriptor);
    }

    String httpMethod = context.getRequestMethod();
    if (methodName != null)
    {
      throw new RoutingException(
              String.format("%s operation " +
                            "named %s " +
                            "not supported on resource '%s' " +
                            "URI: '%s'",
                            httpMethod,
                            methodName,
                            resource.getResourceClass().getName(),
                            context.getRequestURI().toString()),
                            HttpStatus.S_400_BAD_REQUEST.getCode());
    }

    throw new RoutingException(
            String.format("%s operation not supported " +
                          "for URI: '%s' " +
                          "with " + RestConstants.HEADER_RESTLI_REQUEST_METHOD + ": '%s'",
                          httpMethod,
                          context.getRequestURI().toString(),
                          context.getRestLiRequestMethod()),
                          HttpStatus.S_400_BAD_REQUEST.getCode());
  }

  // We use a table match to ensure that we have no subtle ordering dependencies in conditional logic
  //
  // Currently only POST requests set RMETHOD header (HEADER_RESTLI_REQUEST_METHOD), however we include
  // a table entry for GET methods as well to make sure the routing doesn't fail if the client sets the header
  // when it's not necessary, as long as it doesn't conflict with the rest of the parameters.
  private static Map<ResourceMethodMatchKey, ResourceMethod> setupResourceMethodLookup()
  {
    HashMap<ResourceMethodMatchKey, ResourceMethod> result = new HashMap<ResourceMethodMatchKey, ResourceMethod>();
    //                                 METHOD    RMETHOD                    ACTION   QUERY   BATCH   ENTITY
    Object[] config =
    {
            new ResourceMethodMatchKey("GET",    "",                        false,   false,  false,  true),  ResourceMethod.GET,

            new ResourceMethodMatchKey("GET",    "",                        false,   true,   false,  false), ResourceMethod.FINDER,
            new ResourceMethodMatchKey("PUT",    "",                        false,   false,  false,  true),  ResourceMethod.UPDATE,
            new ResourceMethodMatchKey("POST",   "",                        false,   false,  false,  true),  ResourceMethod.PARTIAL_UPDATE,
            new ResourceMethodMatchKey("DELETE", "",                        false,   false,  false,  true),  ResourceMethod.DELETE,
            new ResourceMethodMatchKey("POST",   "",                        true,    false,  false,  true),  ResourceMethod.ACTION,
            new ResourceMethodMatchKey("POST",   "",                        true,    false,  false,  false), ResourceMethod.ACTION,
            new ResourceMethodMatchKey("POST",   "",                        false,   false,  false,  false), ResourceMethod.CREATE,

            new ResourceMethodMatchKey("GET",    "",                        false,   false,  false,  false), ResourceMethod.GET_ALL,
            new ResourceMethodMatchKey("GET",    "GET_ALL",                 false,   false,  false,  false), ResourceMethod.GET_ALL,

            new ResourceMethodMatchKey("GET",    "GET",                     false,   false,  false,  true),  ResourceMethod.GET,
            new ResourceMethodMatchKey("GET",    "FINDER",                  false,   true,   false,  false), ResourceMethod.FINDER,
            new ResourceMethodMatchKey("PUT",    "UPDATE",                  false,   false,  false,  true),  ResourceMethod.UPDATE,
            new ResourceMethodMatchKey("POST",   "PARTIAL_UPDATE",          false,   false,  false,  true),  ResourceMethod.PARTIAL_UPDATE,
            new ResourceMethodMatchKey("DELETE", "DELETE",                  false,   false,  false,  true),  ResourceMethod.DELETE,
            new ResourceMethodMatchKey("POST",   "ACTION",                  true,    false,  false,  true),  ResourceMethod.ACTION,
            new ResourceMethodMatchKey("POST",   "ACTION",                  true,    false,  false,  false), ResourceMethod.ACTION,
            new ResourceMethodMatchKey("POST",   "CREATE",                  false,   false,  false,  false), ResourceMethod.CREATE,

            new ResourceMethodMatchKey("GET",    "",                        false,   false,  true,   false), ResourceMethod.BATCH_GET,
            new ResourceMethodMatchKey("DELETE", "",                        false,   false,  true,   false), ResourceMethod.BATCH_DELETE,
            new ResourceMethodMatchKey("PUT",    "",                        false,   false,  true,   false), ResourceMethod.BATCH_UPDATE,

            new ResourceMethodMatchKey("GET",    "BATCH_GET",               false,   false,  true,   false), ResourceMethod.BATCH_GET,
            new ResourceMethodMatchKey("DELETE", "BATCH_DELETE",            false,   false,  true,   false), ResourceMethod.BATCH_DELETE,
            new ResourceMethodMatchKey("PUT",    "BATCH_UPDATE",            false,   false,  true,   false), ResourceMethod.BATCH_UPDATE,

            new ResourceMethodMatchKey("POST",   "BATCH_CREATE",            false,   false,  false,  false), ResourceMethod.BATCH_CREATE,
            new ResourceMethodMatchKey("POST",   "BATCH_PARTIAL_UPDATE",    false,   false,  true,  false),  ResourceMethod.BATCH_PARTIAL_UPDATE
    };

    for (int ii = 0; ii < config.length; ii += 2)
    {
      ResourceMethodMatchKey key = (ResourceMethodMatchKey) config[ii];
      ResourceMethod method = (ResourceMethod) config[ii + 1];
      ResourceMethod prevValue = result.put(key, method);
      if (prevValue != null)
      {
        throw new RestLiInternalException("Routing Configuration conflict: "
            + prevValue.toString() + " conflicts with " + method.toString());
      }
    }

    return result;
  }

  private ResourceMethod mapResourceMethod(final ServerResourceContext context,
                                           final ResourceLevel resourceLevel)
  {
    ResourceMethodMatchKey key =
        new ResourceMethodMatchKey(context.getRequestMethod(),
                                   context.getRestLiRequestMethod(),
                                   context.getRequestActionName() != null,
                                   context.getRequestFinderName() != null,
                                   context.getPathKeys().getBatchKeys().size() > 0,
                                   resourceLevel.equals(ResourceLevel.ENTITY));

    if (_resourceMethodLookup.containsKey(key))
    {
      return _resourceMethodLookup.get(key);
    }

    if (context.hasParameter(RestConstants.ACTION_PARAM)
        && !"POST".equalsIgnoreCase(context.getRequestMethod()))
    {
      throw new RoutingException(
              String.format("All action methods (specified via '%s' in URI) must " +
                            "be submitted as a POST (was %s)",
                            RestConstants.ACTION_PARAM,
                            context.getRequestMethod()),
                            HttpStatus.S_400_BAD_REQUEST.getCode());

    }

    throw new RoutingException(String.format("Method '%s' is not supported for URI '%s'",
                                             context.getRequestMethod(),
                                             context.getRequestURI()),
                               HttpStatus.S_400_BAD_REQUEST.getCode());

  }

  private CompoundKey parseCompoundKey(final ResourceModel resource,
                                final ServerResourceContext context,
                                final String pathSegment)
 {
    CompoundKey compoundKey =
        ArgumentUtils.parseCompoundKey(pathSegment, resource.getKeys());

    for (String simpleKeyName : compoundKey.getPartKeys())
    {
      context.getPathKeys().append(simpleKeyName, compoundKey.getPart(simpleKeyName));
    }
    context.getPathKeys().append(resource.getKeyName(), compoundKey);
    return compoundKey;
  }


  /**
   * Instantiate the complex key from the current path segment (treat is as a list of
   * query parameters) and put it into the context.
   *
   * @param currentPathSegment
   * @param context
   * @param resource
   * @return
   */
  private static void parseComplexKey(final ResourceModel resource,
                                      final ServerResourceContext context,
                                      final String currentPathSegment)
  {
    try
    {
      ComplexResourceKey<RecordTemplate, RecordTemplate> complexKey =
          ComplexResourceKey.parseFromPathSegment(currentPathSegment,
                                                  resource.getKeyKeyClass(),
                                                  resource.getKeyParamsClass());

      context.getPathKeys().append(resource.getKeyName(), complexKey);
    }
    catch (PathSegmentSyntaxException e)
    {
      throw new RoutingException(String.format("Complex key query parameters parsing error: '%s'",
                                               e.getMessage()),
                                 HttpStatus.S_400_BAD_REQUEST.getCode());
    }
  }

  private void parseBatchKeysParameter(final ResourceModel resource,
                                       final ServerResourceContext context)
  {
    // Complex key batch get
    if (ComplexKeyResource.class.isAssignableFrom(resource.getResourceClass()))
    {
      // Parse all query parameters into a data map.
      DataMap allParametersDataMap = context.getParameters();

      // Get the batch request keys from the IDS list at the root of the map.
      DataList batchIds = allParametersDataMap.getDataList(RestConstants.QUERY_BATCH_IDS_PARAM);
      if (batchIds == null || batchIds.isEmpty())
      {
        return;
      }

      // Validate the complex keys and put them into the context batch keys
      for (Object complexKey : batchIds) {
        if (!(complexKey instanceof DataMap))
        {
          log.warn("Invalid structure of key '" + complexKey.toString() + "', skipping key.");
          context.getBatchKeyErrors().put(complexKey.toString(), new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST));
          continue;
        }
        context.getPathKeys()
               .appendBatchValue(ComplexResourceKey.buildFromDataMap((DataMap) complexKey,
                                                                     resource.getKeyKeyClass(),
                                                                     resource.getKeyParamsClass()));
      }
    }
    // collection batch get
    else if (context.hasParameter(RestConstants.QUERY_BATCH_IDS_PARAM))
    {
      List<String> ids = context.getParameterValues(RestConstants.QUERY_BATCH_IDS_PARAM);
      for (String id : ids)
      {
        try
        {
          context.getPathKeys()
                 .appendBatchValue(ArgumentUtils.parseOptionalKey(id, resource));
        }
        catch (NumberFormatException e)
        {
          log.warn("Caught NumberFormatException parsing batch key '" + id + "', skipping key.");
          context.getBatchKeyErrors().put(id, new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST, null, e));
        }
        catch (IllegalArgumentException e)
        {
          log.warn("Caught IllegalArgumentException parsing batch key '" + id + "', skipping key.");
          context.getBatchKeyErrors().put(id, new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST, null, e));
        }
        catch (PathSegmentSyntaxException e)
        {
          log.warn("Caught PathSegmentSyntaxException parsing batch key '" + id + "', skipping key.");
          context.getBatchKeyErrors().put(id, new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST, null, e));
        }
      }
    }
  }

  private void parseSimpleKey(final ResourceModel resource,
                              final ServerResourceContext context,
                              final String pathSegment)
  {
    try
    {
      context.getPathKeys()
             .append(resource.getKeyName(),
                     ArgumentUtils.parseKeyIntoCorrectType(URLDecoder.decode(pathSegment,
                                                                             RestConstants.DEFAULT_CHARSET_NAME),
                                                           resource));
    }
    catch (UnsupportedEncodingException e)
    {
      throw new RestLiInternalException("UnsupportedEncodingException while trying to decode the key", e);
    }
  }

}