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
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.FieldDef;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.server.ResourceLevel;
import com.linkedin.restli.server.RestLiMethodContext;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author dellamag
 */
//TODO: Remove this once use of InvokeAware has been discontinued.
@SuppressWarnings("deprecation")
public class ResourceMethodDescriptor implements RestLiMethodContext
{
  public static enum InterfaceType
  {
    SYNC, CALLBACK, PROMISE, TASK
  }

  private ResourceModel                                 _resourceModel;
  private final ResourceMethod                          _type;
  private final Method                                  _method;
  private final List<Parameter<?>>                      _parameters;
  private final String                                  _finderName;
  private final Class<? extends RecordTemplate>         _collectionCustomMetadataType;
  // only applies to actions
  private final String                                  _actionName;
  private final ResourceLevel                           _actionResourceLevel;
  private final FieldDef<?>                             _actionReturnFieldDef;
  private final RecordDataSchema                        _actionReturnRecordDataSchema;
  private final RecordDataSchema                        _requestDataSchema;
  private final InterfaceType                           _interfaceType;
  private final DataMap                                 _customAnnotations;

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
                                        null,
                                        null,
                                        null,
                                        null,
                                        metadataType,
                                        interfaceType,
                                        customAnnotations);
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
                                        actionName,
                                        actionResourceType,
                                        actionReturnType,
                                        actionReturnRecordDataSchema,
                                        recordDataSchema,
                                        null,
                                        interfaceType,
                                        customAnnotations);
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
   * Create a CRUD (not action or finder) resource method descriptor with parameters.
   *
   * @param type rest.li {@link ResourceMethod}
   * @param method resource {@link Method}
   * @param parameters list of method {@link Parameter}s
   * @param interfaceType resource {@link InterfaceType}
   * @return CRUD {@link ResourceMethodDescriptor}
   *
   * @deprecated Use {@link #createForRestful(ResourceMethod, Method, List, Class, InterfaceType, DataMap)} instead
   */
  @Deprecated
  public static ResourceMethodDescriptor createForRestful(final ResourceMethod type,
                                                          final Method method,
                                                          final List<Parameter<?>> parameters,
                                                          final InterfaceType interfaceType,
                                                          final DataMap customAnnotations)
  {
    return createForRestful(type,
                            method,
                            parameters,
                            null,
                            interfaceType,
                            customAnnotations);
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
                                        null,
                                        null,
                                        null,
                                        null,
                                        collectionCustomMetadataType,
                                        interfaceType,
                                        customAnnotations);
  }

  /**
   * Constructor.
   */
  private ResourceMethodDescriptor(final ResourceMethod type,
                                   final Method method,
                                   final List<Parameter<?>> parameters,
                                   final String finderName,
                                   final String actionName,
                                   final ResourceLevel actionResourceLevel,
                                   final FieldDef<?> actionReturnType,
                                   final RecordDataSchema actionReturnRecordDataSchema,
                                   final RecordDataSchema requestDataSchema,
                                   final Class<? extends RecordTemplate> collectionCustomMetadataType,
                                   final InterfaceType interfaceType,
                                   final DataMap customAnnotations)
  {
    super();
    _type = type;
    _method = method;
    _parameters = parameters;
    _finderName = finderName;
    _actionName = actionName;
    _actionResourceLevel = actionResourceLevel;
    _actionReturnFieldDef = actionReturnType;
    _actionReturnRecordDataSchema = actionReturnRecordDataSchema;
    _requestDataSchema = requestDataSchema;
    _collectionCustomMetadataType = collectionCustomMetadataType;
    _interfaceType = interfaceType;
    _customAnnotations = customAnnotations;
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
    List<Parameter<?>> params = new ArrayList<Parameter<?>>();
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
  @Override
  public String getFinderName()
  {
    return _finderName;
  }

  /**
   * @deprecated Use {@link #getCollectionCustomMetadataType()} instead.
   */
  @Deprecated
  public Class<? extends RecordTemplate> getFinderMetadataType()
  {
    return _collectionCustomMetadataType;
  }

  public Class<? extends RecordTemplate> getCollectionCustomMetadataType()
  {
    return _collectionCustomMetadataType;
  }

  @Override
  public String getActionName()
  {
    return _actionName;
  }

  @Override
  public String getResourceName()
  {
    return _resourceModel.getName();
  }

  @Override
  public String getNamespace()
  {
    return _resourceModel.getNamespace();
  }

  @Override
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

  public RecordDataSchema getRequestDataSchema()
  {
    return _requestDataSchema;
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

  public boolean isPagingSupported()
  {
    return _parameters.stream().anyMatch(param -> param.getParamType().equals(
        Parameter.ParamType.PAGING_CONTEXT_PARAM));
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
    if (_actionName != null)
    {
      sb.append(", ").append("actionName=").append(_actionName);
    }
    return sb.toString();
  }
}
