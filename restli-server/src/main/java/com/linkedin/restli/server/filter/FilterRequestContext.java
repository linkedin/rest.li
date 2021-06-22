/*
   Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.restli.server.filter;


import com.linkedin.data.DataMap;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.transform.ImmutableList;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.server.model.Parameter;
import com.linkedin.restli.server.CustomRequestContext;
import com.linkedin.restli.server.PathKeys;
import com.linkedin.restli.server.ProjectionMode;
import com.linkedin.restli.server.RestLiRequestData;
import com.linkedin.restli.server.errors.ServiceError;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public interface FilterRequestContext extends CustomRequestContext
{
  Logger LOG = LoggerFactory.getLogger(FilterRequestContext.class);

  /**
   * Get the URI of the request.
   *
   * @return request {@link URI}
   */
  URI getRequestURI();

  /**
   * Get the RestLi Protocol version of the request.
   *
   * @return Rest.li protocol version used by the client sending the request
   */
  ProtocolVersion getRestliProtocolVersion();

  /**
   * get the PathKeys parsed from the URI path.
   *
   * @return PathKeys for this context.
   */
  PathKeys getPathKeys();

  /**
   * get the projection mask parsed from the query for root object entities.
   *
   * @return MaskTree parsed from query, or null if no projection mask was requested.
   */
  MaskTree getProjectionMask();

  /**
   * get the projection mask parsed from the query for CollectionResult metadata
   *
   * @return MaskTree parsed from query, or null if no projection mask was requested.
   */
  MaskTree getMetadataProjectionMask();

  /**
   * Get the projection mask parsed from the query for paging (CollectionMetadata)
   *
   * @return MaskTree parsed from query, or null if no paging projection mask was requested.
   */
  MaskTree getPagingProjectionMask();

  /**
   * Sets the specified projection mask for root entity in the response. Setting the projection mask to {@code null}
   * implies all fields should be projected.
   *
   * @param projectionMask Projection mask to use for root entity
   */
  void setProjectionMask(MaskTree projectionMask);

  /**
   * Sets the specified projection mask for CollectionResult metadata in the response. Setting the projection mask to
   * {@code null} implies all fields should be projected.
   *
   * @param metadataProjectionMask Projection mask to use for CollectionResult metadata
   */
  void setMetadataProjectionMask(MaskTree metadataProjectionMask);

  /**
   * Sets the specified projection mask for paging metadata in the response (applies only for collection responses).
   * Setting the projection mask to {@code null} implies all fields should be projected.
   *
   * @param pagingProjectionMask Projection mask to use for paging metadata
   */
  void setPagingProjectionMask(MaskTree pagingProjectionMask);

  /**
   * Get all query parameters from the request.
   *
   * @return {@link DataMap} of request query parameters.
   */
  DataMap getQueryParameters();

  /**
   * Get a mutable representation of all headers from the request.
   *
   * @return a mutable map of header name -> header value
   */
  Map<String, String> getRequestHeaders();

  /**
   * Get the projection mode to be applied to the response body.
   *
   * @return Projection mode for the response body.
   */
  ProjectionMode getProjectionMode();

  /**
   * Get the name of the target resource.
   *
   * @return Name of the resource.
   * @deprecated Use {@link #getFilterResourceModel()} then {@link FilterResourceModel#getResourceName()} instead.
   */
  @Deprecated
  String getResourceName();

  /**
   * Get the namespace of the target resource.
   *
   * @return Namespace of the resource.
   * @deprecated Use {@link #getFilterResourceModel()} then {@link FilterResourceModel#getResourceNamespace()} instead.
   */
  @Deprecated
  String getResourceNamespace();

  /**
   * Obtain finder name if the invoked method is a FINDER.
   *
   * @return Method name if the method is a finder; else, null.
   */
  String getFinderName();

  /**
   * Obtain batch finder name if the invoked method is a BATCH_FINDER.
   *
   * @return Method name if the method is a batch_finder; else, null.
   */
  String getBatchFinderName();

  /**
   * Obtain the name of the action associate with the resource.
   *
   * @return Method name if the method is an action; else, null.
   */
  String getActionName();

  /**
   * Get the type of operation, GET, CREATE, UPDATE, etc.
   *
   * @return Type of operation that's being invoked on the resource.
   */
  ResourceMethod getMethodType();

  /**
   * Gets an immutable view of the expected service errors for the resource method, or null if errors aren't defined.
   *
   * @return {@link List}&#60;{@link ServiceError}&#62; defined for the resource method
   */
  List<ServiceError> getMethodServiceErrors();

  /**
   * Get custom annotations defined on the resource.
   *
   * @return customer annotations defined on the resource.
   */
  DataMap getCustomAnnotations();

  /**
   * Get request data.
   *
   * @return Request data.
   */
  RestLiRequestData getRequestData();

  /**
   * Get a Map that can be used as a scratch-pad between filters.
   *
   * @return a scratch pad in the form of a Map.
   */
  Map<String, Object> getFilterScratchpad();

  /**
   * Obtain the {@link FilterResourceModel} corresponding to the resource.
   *
   * @return ResourceModel corresponding to the resource.
   */
  FilterResourceModel getFilterResourceModel();

  /**
   * Obtain the collection metadata schema of finders corresponding to the resource.
   *
   * @return Metadata schema for the resource.
   */
  RecordDataSchema getCollectionCustomMetadataSchema();

  /**
   * Obtain the logical return type of the action method being queried, or null if the method being queried is not an
   * action method.
   * <p>
   * The type returned will be the "logical" return type in the sense that wrapper types such as
   * {@link com.linkedin.restli.server.ActionResult} and {@link com.linkedin.parseq.Task} will not be present.
   * For methods with a void return type, {@link Void#TYPE} will be returned.
   * <p>
   * For instance, this method will return {@code String} for a method with the return type
   * {@code Task<ActionResult<String>>}.
   *
   * @return the action method's return type.
   */
  Class<?> getActionReturnType();

  /**
   * Obtain the schema of the action request object.
   *
   * @return record template of the request object.
   */
  RecordDataSchema getActionRequestSchema();

  /**
   * Obtain the schema of the action response object.
   *
   * @return record template of the response object.
   */
  RecordDataSchema getActionResponseSchema();

  /**
   * Get {@link Method} that's being invoked on the resource.
   *
   * @return {@link Method}
   */
  Method getMethod();

  /**
   * Gets an immutable view of the parameters defined for the target resource method.
   * TODO: Remove the "default" implementation in the next major version.
   *
   * @return list of method parameters
   */
  default List<Parameter<?>> getMethodParameters()
  {
    return Collections.unmodifiableList(Collections.emptyList());
  }

  /**
   * Return the attributes from R2 RequestContext.
   *
   * @see RequestContext#getLocalAttrs()
   * @return the attributes contained by RequestContext.
   */
  Map<String, Object> getRequestContextLocalAttrs();

  /**
   * Returns whether the resource method being queried is a "return entity" method, meaning that it's
   * annotated with the {@link com.linkedin.restli.server.annotations.ReturnEntity} annotation.
   * This is used primarily for methods that don't normally return an entity (e.g. CREATE).
   *
   * @return true if the method being queried is a "return entity" method.
   */
  boolean isReturnEntityMethod();

  /**
   * Returns whether or not the client is requesting that the entity (or entities) be returned. Reads the appropriate
   * query parameter to determine this information, defaults to true if the query parameter isn't present, and throws
   * an exception if the parameter's value is not a boolean value. Keep in mind that the value of this method should be
   * inconsequential if the resource method at hand doesn't have a "return entity" method signature.
   *
   * @return whether the request specifies that the resource should return an entity
   */
  boolean isReturnEntityRequested();

  @SuppressWarnings("unchecked")
  default Class<? extends RecordTemplate> getValueClass()
  {
    try {
      String version = getRequestHeaders().get(RestConstants.HEADER_RESTLI_SCHEMA_VERSION);
      if (version != null) {
        int ver = Integer.parseInt(version);
        return (Class<? extends RecordTemplate>) getFilterResourceModel().getValueClass().getDeclaredMethod("getVersionedClass", int.class).invoke(null, ver);
      }
    } catch (NumberFormatException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      // Ignore invalid version?
    }
    return getFilterResourceModel().getValueClass();
  }
}