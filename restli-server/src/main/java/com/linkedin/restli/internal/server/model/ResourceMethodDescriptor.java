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

package com.linkedin.restli.internal.server.model;

import com.linkedin.data.DataMap;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.FieldDef;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.TemplateRuntimeException;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.restspec.MaxBatchSizeSchema;
import com.linkedin.restli.server.ResourceLevel;
import com.linkedin.restli.server.annotations.ServiceErrors;
import com.linkedin.restli.server.errors.ServiceError;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Representation of a Rest.li resource method.
 *
 * @author dellamag
 */
public class ResourceMethodDescriptor
{
  private static final Logger LOGGER = LoggerFactory.getLogger(ResourceMethodDescriptor.class.getSimpleName());

  public enum InterfaceType
  {
    SYNC, CALLBACK, PROMISE, TASK
  }

  public static final Integer BATCH_FINDER_NULL_CRITERIA_INDEX = null;

  private ResourceModel                                 _resourceModel;
  private final ResourceMethod                          _type;
  private final Method                                  _method;
  private final List<Parameter<?>>                      _parameters;
  private final String                                  _finderName;
  private final String                                  _batchFinderName;
  // The input parameter index that represents the batch criteria in the resource method.
  private final Integer                                 _batchFinderCriteriaIndex;
  private final Class<? extends RecordTemplate>         _collectionCustomMetadataType;
  // Only applies to actions
  private final String                                  _actionName;
  private final ResourceLevel                           _actionResourceLevel;
  private final FieldDef<?>                             _actionReturnFieldDef;
  private final boolean                                 _isActionReadOnly;
  private final RecordDataSchema                        _actionReturnRecordDataSchema;
  private final RecordDataSchema                        _requestDataSchema;
  private final InterfaceType                           _interfaceType;
  private final DataMap                                 _customAnnotations;
  private final String                                  _linkedBatchFinderName;
  // Method-level service error definitions
  private List<ServiceError>                            _serviceErrors;
  private List<HttpStatus>                              _successStatuses;
  private MaxBatchSizeSchema                            _maxBatchSize;

  /**
   * Finder resource method descriptor factory.
   *
   * @param method resource {@link Method}
   * @param parameters rest.li method {@link Parameter}s
   * @param finderName finder name
   * @param metadataType finder metadata type
   * @param interfaceType method {@link InterfaceType}
   * @return finder {@link ResourceMethodDescriptor}
   */
  public static ResourceMethodDescriptor createForFinder(final Method method,
                                                         final List<Parameter<?>> parameters,
                                                         final String finderName,
                                                         final Class<? extends RecordTemplate> metadataType,
                                                         final InterfaceType interfaceType,
                                                         final DataMap customAnnotations)
  {
    return new ResourceMethodDescriptor(ResourceMethod.FINDER,
                                        method,
                                        parameters,
                                        finderName,
                                        null,
                                        BATCH_FINDER_NULL_CRITERIA_INDEX,
                                        null,
                                        null,
                                        null,
                                        null,
                                        false,
                                        null,
                                        metadataType,
                                        interfaceType,
                                        customAnnotations,
                                        null);
  }

  /**
   * Finder resource method descriptor factory.
   *
   * @param method resource {@link Method}
   * @param parameters rest.li method {@link Parameter}s
   * @param finderName finder name
   * @param metadataType finder metadata type
   * @param interfaceType method {@link InterfaceType}
   * @param customAnnotations All the custom annotations associated with this method encoded as a {@link DataMap}
   * @param linkedBatchFinderName The optional batch finder linked to this finder
   * @return finder {@link ResourceMethodDescriptor}
   */
  public static ResourceMethodDescriptor createForFinder(final Method method,
      final List<Parameter<?>> parameters,
      final String finderName,
      final Class<? extends RecordTemplate> metadataType,
      final InterfaceType interfaceType,
      final DataMap customAnnotations,
      final String linkedBatchFinderName)
  {
    return new ResourceMethodDescriptor(ResourceMethod.FINDER,
                                        method,
                                        parameters,
                                        finderName,
                                        null,
                                        BATCH_FINDER_NULL_CRITERIA_INDEX,
                                        null,
                                        null,
                                        null,
                                        null,
                                        false,
                                        null,
                                        metadataType,
                                        interfaceType,
                                        customAnnotations,
                                        linkedBatchFinderName);
  }

  /**
   * Batch Finder resource method descriptor factory.
   *
   * @param method resource {@link Method}
   * @param parameters rest.li method {@link Parameter}s
   * @param batchFinderName finder name
   * @param batchFinderCriteriaIndex parameter index of the criteria in the batch finder method
   * @param metadataType finder metadata type
   * @param interfaceType method {@link InterfaceType}
   * @return finder {@link ResourceMethodDescriptor}
   */
  public static ResourceMethodDescriptor createForBatchFinder(final Method method,
                                                              final List<Parameter<?>> parameters,
                                                              final String batchFinderName,
                                                              final Integer batchFinderCriteriaIndex,
                                                              final Class<? extends RecordTemplate> metadataType,
                                                              final InterfaceType interfaceType,
                                                              final DataMap customAnnotations)
  {
    return new ResourceMethodDescriptor(ResourceMethod.BATCH_FINDER,
                                        method,
                                        parameters,
                                        null,
                                        batchFinderName,
                                        batchFinderCriteriaIndex,
                                        null,
                                        null,
                                        null,
                                        null,
                                        false,
                                        null,
                                        metadataType,
                                        interfaceType,
                                        customAnnotations,
                     null);
  }

  /**
   * Action resource method descriptor factory.
   *
   * @param method resource {@link Method}
   * @param parameters rest.li method {@link Parameter}s
   * @param actionName action name
   * @param actionResourceType action {@link ResourceLevel}
   * @param actionReturnType action return type class
   * @param actionReturnRecordDataSchema the RecordDataSchema for the action return
   * @param recordDataSchema the RecordDataSchema for the method
   * @param interfaceType resource method {@link InterfaceType}
   * @return action {@link ResourceMethodDescriptor}
   */
  public static ResourceMethodDescriptor createForAction(
                                               final Method method,
                                               final List<Parameter<?>> parameters,
                                               final String actionName,
                                               final ResourceLevel actionResourceType,
                                               final FieldDef<?> actionReturnType,
                                               final RecordDataSchema actionReturnRecordDataSchema,
                                               final RecordDataSchema recordDataSchema,
                                               final InterfaceType interfaceType,
                                               final DataMap customAnnotations)
  {
    return new ResourceMethodDescriptor(ResourceMethod.ACTION,
                                        method,
                                        parameters,
                                        null,
                                        null,
                                        BATCH_FINDER_NULL_CRITERIA_INDEX,
                                        actionName,
                                        actionResourceType,
                                        actionReturnType,
                                        actionReturnRecordDataSchema,
                                        false,
                                        recordDataSchema,
                                        null,
                                        interfaceType,
                                        customAnnotations,
                                        null);
  }

  /**
   * Action resource method descriptor factory.
   *
   * @param method resource {@link Method}
   * @param parameters rest.li method {@link Parameter}s
   * @param actionName action name
   * @param actionResourceType action {@link ResourceLevel}
   * @param actionReturnType action return type class
   * @param actionReturnRecordDataSchema the RecordDataSchema for the action return
   * @param recordDataSchema the RecordDataSchema for the method
   * @param isActionReadOnly true if the action is read only, false otherwise.
   * @param interfaceType resource method {@link InterfaceType}
   * @return action {@link ResourceMethodDescriptor}
   */
  public static ResourceMethodDescriptor createForAction(
      final Method method,
      final List<Parameter<?>> parameters,
      final String actionName,
      final ResourceLevel actionResourceType,
      final FieldDef<?> actionReturnType,
      final RecordDataSchema actionReturnRecordDataSchema,
      final boolean isActionReadOnly,
      final RecordDataSchema recordDataSchema,
      final InterfaceType interfaceType,
      final DataMap customAnnotations)
  {
    return new ResourceMethodDescriptor(ResourceMethod.ACTION,
                                        method,
                                        parameters,
                                        null,
                                        null,
                                        BATCH_FINDER_NULL_CRITERIA_INDEX,
                                        actionName,
                                        actionResourceType,
                                        actionReturnType,
                                        actionReturnRecordDataSchema,
                                        isActionReadOnly,
                                        recordDataSchema,
                                        null,
                                        interfaceType,
                                        customAnnotations,
                                        null);
  }

  /**
   * Create a CRUD (not action or finder) resource method descriptor with no parameters.
   *
   * @param type rest.li {@link ResourceMethod}
   * @param method resource {@link Method}
   * @param interfaceType resource {@link InterfaceType}
   * @return CRUD {@link ResourceMethodDescriptor}
   */
  public static ResourceMethodDescriptor createForRestful(final ResourceMethod type,
                                                          final Method method,
                                                          final InterfaceType interfaceType)
  {
    return createForRestful(type,
                            method,
                            Collections.<Parameter<?>> emptyList(),
                            null,
                            interfaceType,
                            null);
  }

  /**
   * Create a CRUD (not action or finder) resource method descriptor with parameters, custom annotations and
   * custom collection metadata.
   *
   * @param type rest.li {@link ResourceMethod}
   * @param method resource {@link Method}
   * @param parameters list of method {@link Parameter}s
   * @param collectionCustomMetadataType collection metadata type for GET_ALL method
   * @param interfaceType resource {@link InterfaceType}
   * @return CRUD {@link ResourceMethodDescriptor}
   */
  public static ResourceMethodDescriptor createForRestful(final ResourceMethod type,
                                                          final Method method,
                                                          final List<Parameter<?>> parameters,
                                                          final Class<? extends RecordTemplate> collectionCustomMetadataType,
                                                          final InterfaceType interfaceType,
                                                          final DataMap customAnnotations)
  {
    return new ResourceMethodDescriptor(type,
                                        method,
                                        parameters,
                                        null,
                                        null,
                                         BATCH_FINDER_NULL_CRITERIA_INDEX,
                                        null,
                                        null,
                                        null,
                                        null,
                                        false,
                                        null,
                                        collectionCustomMetadataType,
                                        interfaceType,
                                        customAnnotations,
                                        null);
  }

  /**
   * Constructor.
   */
  private ResourceMethodDescriptor(final ResourceMethod type,
                                   final Method method,
                                   final List<Parameter<?>> parameters,
                                   final String finderName,
                                   final String batchFinderName,
                                   final Integer batchFinderCriteriaIndex,
                                   final String actionName,
                                   final ResourceLevel actionResourceLevel,
                                   final FieldDef<?> actionReturnType,
                                   final RecordDataSchema actionReturnRecordDataSchema,
                                   final boolean isActionReadOnly,
                                   final RecordDataSchema requestDataSchema,
                                   final Class<? extends RecordTemplate> collectionCustomMetadataType,
                                   final InterfaceType interfaceType,
                                   final DataMap customAnnotations,
                                   final String linkedBatchFinderName)
  {
    super();
    _type = type;
    _method = method;
    _parameters = parameters;
    _finderName = finderName;
    _batchFinderName = batchFinderName;
    _actionName = actionName;
    _actionResourceLevel = actionResourceLevel;
    _actionReturnFieldDef = actionReturnType;
    _actionReturnRecordDataSchema = actionReturnRecordDataSchema;
    _isActionReadOnly = isActionReadOnly;
    _requestDataSchema = requestDataSchema;
    _collectionCustomMetadataType = collectionCustomMetadataType;
    _interfaceType = interfaceType;
    _customAnnotations = customAnnotations;
    _batchFinderCriteriaIndex = batchFinderCriteriaIndex;
    _linkedBatchFinderName = linkedBatchFinderName;
  }

  /**
   * Get {@link ResourceModel} of ResourceMethodDescriptor.
   *
   * @return {@link ResourceModel}
   */
  public ResourceModel getResourceModel()
  {
    return _resourceModel;
  }

  /**
   * Set {@link ResourceModel} of ResourceMethodDescriptor.
   *
   * @param resourceModel {@link ResourceModel} to be set in the descriptor
   */
  public void setResourceModel(final ResourceModel resourceModel)
  {
    _resourceModel = resourceModel;
  }

  /**
   * Get {@link ResourceMethod} of ResourceMethodDescriptor.
   *
   * @return {@link ResourceMethod}
   */
  public ResourceMethod getType()
  {
    return _type;
  }

  /**
   * Get resource {@link Method}.
   *
   * @return {@link Method}
   */
  public Method getMethod()
  {
    return _method;
  }

  /**
   * Get resource method name.
   *
   * @return String
   */
  public String getMethodName()
  {
    switch (_type) {
      case ACTION:
        return getActionName();
      case FINDER:
        return getFinderName();
      case BATCH_FINDER:
        return getBatchFinderName();
    }

    return _type.toString();
  }

  /**
   * @return The first actual type from a parameterized type as a class.
   */
  private Class<?> getFirstActualType(ParameterizedType parameterizedType) {
    Type unwrappedType = parameterizedType.getActualTypeArguments()[0];
    // Now there are 2 cases. The generic type may represent a parameterized type itself, in which case we need
    // to extract its raw type because we don't care about its generic type. Else we can just cast it to a class
    // and return it.
    if (unwrappedType instanceof ParameterizedType) {
      return (Class<?>) ((ParameterizedType) unwrappedType).getRawType();
    }
    return (Class<?>) unwrappedType;
  }

  /**
   * Get the list of the method {@link Parameter}s.
   *
   * @return List<{@link Parameter}>
   */
  public List<Parameter<?>> getParameters()
  {
    return _parameters;
  }

  /**
   * Return a list of parameters with the provided type.
   *
   * @param type {@link Parameter.ParamType} to filter by
   *
   * @return list of parameters that match the provided type
   */
  public List<Parameter<?>> getParametersWithType(final Parameter.ParamType type)
  {
    List<Parameter<?>> params = new ArrayList<>();
    for (Parameter<?> p : _parameters)
    {
      if (p.getParamType() == type)
      {
        params.add(p);
      }
    }
    return params;
  }

  /**
   * Get parameter value by name.
   *
   * @param name parameter name
   * @param <T> parameter value type
   * @return parameter value
   */
  @SuppressWarnings("unchecked")
  public <T> Parameter<T> getParameter(final String name)
  {
    for (Parameter<?> p : _parameters)
    {
      if (p.getName().equals(name))
      {
        return (Parameter<T>) p;
      }
    }

    return null;
  }

  /**
   * @return method finder name
   */
  public String getFinderName()
  {
    return _finderName;
  }

  /**
   * @return method batch finder name
   */
  public String getBatchFinderName()
  {
    return _batchFinderName;
  }

  /**
   * @return the name of the batch finder method linked with a finder method
   */
  public String getLinkedBatchFinderName() {
    return _linkedBatchFinderName;
  }

  /**
   * @return the batch finder criteria parameter index
   */
  public Integer getBatchFinderCriteriaParamIndex()
  {
    return _batchFinderCriteriaIndex;
  }

  public Class<? extends RecordTemplate> getCollectionCustomMetadataType()
  {
    return _collectionCustomMetadataType;
  }

  public String getActionName()
  {
    return _actionName;
  }

  public String getResourceName()
  {
    return _resourceModel.getName();
  }

  public String getNamespace()
  {
    return _resourceModel.getNamespace();
  }

  public ResourceMethod getMethodType()
  {
    return _type;
  }

  public ResourceLevel getActionResourceLevel()
  {
    return _actionResourceLevel;
  }

  public Class<?> getActionReturnType()
  {
    if (_actionReturnFieldDef == null)
    {
      return Void.TYPE;
    }
    return _actionReturnFieldDef.getType();
  }

  public FieldDef<?> getActionReturnFieldDef()
  {
    return _actionReturnFieldDef;
  }

  public RecordDataSchema getActionReturnRecordDataSchema()
  {
    return _actionReturnRecordDataSchema;
  }

  public boolean isActionReadOnly()
  {
    return _isActionReadOnly;
  }

  public RecordDataSchema getRequestDataSchema()
  {
    return _requestDataSchema;
  }

  /**
   * Collect all the data schemas referenced by this method descriptor.
   */
  public void collectReferencedDataSchemas(Set<DataSchema> schemas)
  {
    if (_requestDataSchema != null)
    {
      schemas.add(_requestDataSchema);
    }

    if (_actionReturnRecordDataSchema != null)
    {
      schemas.add(_actionReturnRecordDataSchema);
    }

    if (_actionReturnFieldDef != null)
    {
      DataSchema schema = _actionReturnFieldDef.getDataSchema();
      if (schema != null)
      {
        schemas.add(schema);
      }
    }

    if (_collectionCustomMetadataType != null)
    {
      try
      {
        schemas.add(DataTemplateUtil.getSchema(_collectionCustomMetadataType));
      }
      catch (TemplateRuntimeException e)
      {
        LOGGER.debug("Failed to get schema for collection metadata type: " + _collectionCustomMetadataType.getName(), e);
      }
    }

    if (_parameters != null)
    {
      for (Parameter<?> parameter : _parameters)
      {
        DataSchema schema = parameter.getDataSchema();
        if (schema != null)
        {
          schemas.add(schema);
        }
      }
    }
  }

  /**
   * @param type {@link Parameter.ParamType} parameter type to find the index of
   * @return index of the first parameter with the given type, or -1 if none exists
   */
  public int indexOfParameterType(final Parameter.ParamType type)
  {
    for (int i = 0; i < _parameters.size(); i++)
    {
      if (_parameters.get(i).getParamType().equals(type))
      {
        return i;
      }
    }
    return -1;
  }

  /**
   * Get method {@link InterfaceType}.
   *
   * @return {@link InterfaceType}
   */
  public InterfaceType getInterfaceType()
  {
    return _interfaceType;
  }

  public DataMap getCustomAnnotationData()
  {
    return _customAnnotations;
  }

  public <T extends Annotation> T getAnnotation(Class<T> annotationClass)
  {
    return this.getMethod().getAnnotation(annotationClass);
  }

  public boolean isPagingSupported()
  {
    return _parameters.stream().anyMatch(param -> param.getParamType().equals(
        Parameter.ParamType.PAGING_CONTEXT_PARAM));
  }

  /**
   * Gets an immutable view of the expected service errors for this resource method, or null if errors aren't defined.
   * @return {@link List}&#60;{@link ServiceError}&#62;
   */
  public List<ServiceError> getServiceErrors()
  {
    return _serviceErrors == null ? null : Collections.unmodifiableList(_serviceErrors);
  }

  /**
   * Sets the list of expected service errors for this resource method.
   * Note that a null list and an empty list are semantically different (see {@link ServiceErrors}).
   * @param serviceErrors {@link List}&#60;{@link ServiceError}&#62;
   */
  public void setServiceErrors(final Collection<ServiceError> serviceErrors)
  {
    _serviceErrors = serviceErrors == null ? null : new ArrayList<>(serviceErrors);
  }

  /**
   * Gets an immutable view of the expected success Http status codes for this resource method, or null if none are defined.
   * @return {@link List}&#60;{@link Integer}&#62; list of expected success Http status codes
   */
  public List<HttpStatus> getSuccessStatuses()
  {
    return _successStatuses == null ? null : Collections.unmodifiableList(_successStatuses);
  }

  /**
   * Sets the list of expected success Http status codes for this resource method.
   * @param successStatuses {@link List}&#60;{@link Integer}&#62;
   */
  public void setSuccessStatuses(final Collection<HttpStatus> successStatuses)
  {
    _successStatuses = successStatuses == null ? null : new ArrayList<>(successStatuses);
  }

  /**
   * Gets the max batch size for this resource method, or null if it is not defined.
   * @return {@link MaxBatchSizeSchema}
   */
  public MaxBatchSizeSchema getMaxBatchSize()
  {
    return _maxBatchSize;
  }

  /**
   * Sets the max batch size for this resource method
   */
  public void setMaxBatchSize(MaxBatchSizeSchema maxBatchSize)
  {
    _maxBatchSize = maxBatchSize;
  }

  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder("type=").append(_type);
    sb.append(", ");
    sb.append("resourceName=").append(_resourceModel.getName());
    if (_finderName != null)
    {
      sb.append(", ").append("finderName=").append(_finderName);
    }
    if (_batchFinderName != null) {
      sb.append(", ").append("batchFinderName=").append(_batchFinderName);
    }
    if (_actionName != null)
    {
      sb.append(", ").append("actionName=").append(_actionName);
    }
    return sb.toString();
  }
}
