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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.linkedin.data.schema.TyperefDataSchema;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.server.ResourceLevel;

/**
 * @author dellamag
 */
public final class ResourceMethodDescriptor
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
  private final Class<? extends RecordTemplate>         _finderMetadataType;
  // only applies to actions
  private final String                                  _actionName;
  private final ResourceLevel                           _actionResourceLevel;
  private final Class<?>                                _actionReturnType;
  private final TyperefDataSchema                       _actionReturnTyperefSchema;
  private final InterfaceType                           _interfaceType;

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
                                                         final InterfaceType interfaceType)
  {
    return new ResourceMethodDescriptor(ResourceMethod.FINDER,
                                        method,
                                        parameters,
                                        finderName,
                                        null,
                                        null,
                                        null,
                                        null,
                                        metadataType,
                                        interfaceType);
  }


  /**
   * Action resource method descriptor factory.
   *
   * @param method resource {@link Method}
   * @param parameters rest.li method {@link Parameter}s
   * @param actionName action name
   * @param actionResourceType action {@link ResourceLevel}
   * @param actionReturnType action return type class
   * @param actionReturnTyperefSchema action return {@link TyperefDataSchema}
   * @param interfaceType resource method {@link InterfaceType}
   * @return action {@link ResourceMethodDescriptor}
   */
  public static ResourceMethodDescriptor createForAction(
                                               final Method method,
                                               final List<Parameter<?>> parameters,
                                               final String actionName,
                                               final ResourceLevel actionResourceType,
                                               final Class<?> actionReturnType,
                                               final TyperefDataSchema actionReturnTyperefSchema,
                                               final InterfaceType interfaceType)
  {
    return new ResourceMethodDescriptor(ResourceMethod.ACTION,
                                        method,
                                        parameters,
                                        null,
                                        actionName,
                                        actionResourceType,
                                        actionReturnType,
                                        actionReturnTyperefSchema,
                                        null,
                                        interfaceType);
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
                            interfaceType);
  }

  /**
   * Create a CRUD (not action or finder) resource method descriptor with parameters.
   *
   * @param type rest.li {@link ResourceMethod}
   * @param method resource {@link Method}
   * @param parameters list of method {@link Parameter}s
   * @param interfaceType resource {@link InterfaceType}
   * @return CRUD {@link ResourceMethodDescriptor}
   */
  public static ResourceMethodDescriptor createForRestful(final ResourceMethod type,
                                                          final Method method,
                                                          final List<Parameter<?>> parameters,
                                                          final InterfaceType interfaceType)
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
                                        interfaceType);
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
                                   final Class<?> actionReturnType,
                                   final TyperefDataSchema actionReturnTyperefSchema,
                                   final Class<? extends RecordTemplate> finderMetadataType,
                                   final InterfaceType interfaceType)
  {
    super();
    _type = type;
    _method = method;
    _parameters = parameters;
    _finderName = finderName;
    _actionName = actionName;
    _actionResourceLevel = actionResourceLevel;
    _actionReturnType = actionReturnType;
    _actionReturnTyperefSchema = actionReturnTyperefSchema;
    _finderMetadataType = finderMetadataType;
    _interfaceType = interfaceType;
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
  public String getFinderName()
  {
    return _finderName;
  }

  public Class<? extends RecordTemplate> getFinderMetadataType()
  {
    return _finderMetadataType;
  }

  public String getActionName()
  {
    return _actionName;
  }

  public ResourceLevel getActionResourceLevel()
  {
    return _actionResourceLevel;
  }

  public Class<?> getActionReturnType()
  {
    return _actionReturnType;
  }

  public TyperefDataSchema getActionReturnTyperefSchema()
  {
    return _actionReturnTyperefSchema;
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

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("type=").append(_type).append(", pathName=").append(_finderName);
    return sb.toString();
  }
}
