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
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.TemplateRuntimeException;
import com.linkedin.internal.common.util.CollectionUtils;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.restspec.ResourceEntityType;
import com.linkedin.restli.server.AlternativeKey;
import com.linkedin.restli.server.Key;
import com.linkedin.restli.server.ResourceDefinition;
import com.linkedin.restli.server.ResourceLevel;
import com.linkedin.restli.server.annotations.ServiceErrors;
import com.linkedin.restli.server.errors.ServiceError;
import com.linkedin.restli.server.resources.ComplexKeyResource;
import com.linkedin.restli.server.util.UnstructuredDataUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Representation of a Rest.li resource.
 *
 * @author dellamag
 */
public class ResourceModel implements ResourceDefinition
{
  private static final Logger LOGGER = LoggerFactory.getLogger(ResourceModel.class.getSimpleName());

  private final String                          _name;
  private final String                          _namespace;

  private final Class<?>                        _resourceClass;
  private final ResourceType                    _resourceType;

  private final boolean                         _root;
  private final Class<?>                        _parentResourceClass;
  private ResourceModel                         _parentResourceModel;

  private final Set<Key>                        _keys;
  private final Key                             _primaryKey;
  // These are the classes of the complex resource key RecordTemplate-derived
  // constituents Key and Params
  private final Class<? extends RecordTemplate> _keyKeyClass;
  private final Class<? extends RecordTemplate> _keyParamsClass;

  // Alternative key
  private Map<String, AlternativeKey<?, ?>>     _alternativeKeys;

  private final Map<String, Class<?>>           _keyClasses;
  private final Class<? extends RecordTemplate> _valueClass;

  private final List<ResourceMethodDescriptor>  _resourceMethodDescriptors;

  private final Map<String, ResourceModel>      _pathSubResourceMap;

  private DataMap                               _customAnnotations;

  // Resource-level service error definitions
  private List<ServiceError>                    _serviceErrors;

  /**
   * Constructor.
   *
   * @param primaryKey the primary {@link Key} of this resource
   * @param keyKeyClass class of the key part of a {@link ComplexResourceKey} if this is a
   *          {@link ComplexKeyResource}
   * @param keyParamsClass class of the param part of a {@link ComplexResourceKey} if this
   *          is a {@link ComplexKeyResource}
   * @param keys set of resource keys
   * @param valueClass resource value class
   * @param resourceClass resource class
   * @param parentResourceClass parent resource class
   * @param name resource name
   * @param resourceType {@link ResourceType}
   * @param namespace namespace
   */
  public ResourceModel(final Key primaryKey,
                       final Class<? extends RecordTemplate> keyKeyClass,
                       final Class<? extends RecordTemplate> keyParamsClass,
                       final Set<Key> keys,
                       final Class<? extends RecordTemplate> valueClass,
                       final Class<?> resourceClass,
                       final Class<?> parentResourceClass,
                       final String name,
                       final ResourceType resourceType,
                       final String namespace)
  {
    _keyKeyClass = keyKeyClass;
    _keyParamsClass = keyParamsClass;
    _keys = keys;
    _keyClasses = new HashMap<>(CollectionUtils.getMapInitialCapacity(_keys.size(), 0.75f), 0.75f);
    for (Key key : _keys)
    {
      _keyClasses.put(key.getName(), key.getType());
    }
    _valueClass = valueClass;
    _resourceClass = resourceClass;
    _name = name;
    _namespace = namespace;
    _root = (parentResourceClass == null);
    _parentResourceClass = parentResourceClass;
    _resourceMethodDescriptors = new ArrayList<>(5);
    _primaryKey = primaryKey;
    _resourceType = resourceType;
    _pathSubResourceMap = new HashMap<>();
  }

  /**
   * Constructor.
   *
   * @param valueClass resource value class
   * @param resourceClass resource class
   * @param parentResourceClass parent resource class
   * @param name resource name
   * @param resourceType {@link ResourceType}
   * @param namespace namespace
   *
   */
  public ResourceModel(final Class<? extends RecordTemplate> valueClass,
                       final Class<?> resourceClass,
                       final Class<?> parentResourceClass,
                       final String name,
                       final ResourceType resourceType,
                       final String namespace)
  {
    this(null,
         null,
         null,
         Collections.<Key>emptySet(),
         valueClass,
         resourceClass,
         parentResourceClass,
         name,
         resourceType,
         namespace);
  }

  public ResourceType getResourceType()
  {
    return _resourceType;
  }

  public ResourceLevel getResourceLevel()
  {
    switch (_resourceType)
    {
      case COLLECTION:
      case ASSOCIATION:
      case ACTIONS:
        return ResourceLevel.COLLECTION;
      case SIMPLE:
        return ResourceLevel.ENTITY;
      default:
        return ResourceLevel.ANY;
    }
  }

  public Class<?> getKeyClass()
  {
    return _primaryKey == null ? null : _primaryKey.getType();
  }

  public Set<Key> getKeys()
  {
    return _keys;
  }

  public Map<String, Class<?>> getKeyClasses()
  {
    return _keyClasses;
  }

  /**
   * @param name key name
   * @return {@link Key} matching the name
   */
  public Key getKey(final String name)
  {
    for (Key key : _keys)
    {
      if (key.getName().equals(name))
      {
        return key;
      }
    }

    return null;
  }

  /**
   * @return Set of key names for this resource
   */
  public Set<String> getKeyNames()
  {
    Set<String> keyNames = new HashSet<>();
    for (Key key : _keys)
    {
      keyNames.add(key.getName());
    }
    return keyNames;
  }

  /**
   * Put the given alternative keys into this ResourceModel.
   *
   * @param alternativeKeys map from alternative key name to {@link com.linkedin.restli.server.AlternativeKey}.
   */
  public void putAlternativeKeys(Map<String, AlternativeKey<?, ?>> alternativeKeys)
  {
    _alternativeKeys = alternativeKeys;
  }

  /**
   * @return map from alternative key name to {@link com.linkedin.restli.server.AlternativeKey} of this resource.
   */
  public Map<String, AlternativeKey<?, ?>> getAlternativeKeys()
  {
    return _alternativeKeys;
  }

  /**
   * Add a {@link ResourceMethodDescriptor} to the model.
   *
   * @param methodDescriptor {@link ResourceMethodDescriptor} to add
   */
  public void addResourceMethodDescriptor(final ResourceMethodDescriptor methodDescriptor)
  {
    methodDescriptor.setResourceModel(this);
    _resourceMethodDescriptors.add(methodDescriptor);
  }

  public Class<? extends RecordTemplate> getValueClass()
  {
    return _valueClass;
  }

  @Override
  public ResourceDefinition getParent()
  {
    return _parentResourceModel;
  }

  public ResourceModel getParentResourceModel()
  {
    return _parentResourceModel;
  }

  public Class<?> getParentResourceClass()
  {
    return _parentResourceClass;
  }

  public void setParentResourceModel(final ResourceModel parentResourceModel)
  {
    _parentResourceModel = parentResourceModel;
  }

  public void setCustomAnnotation(DataMap customAnnotationData)
  {
    _customAnnotations = customAnnotationData;
  }

  /**
   * Add a sub-resource to the model.
   *
   * @param path path of the sub-resource to add
   * @param resourceModel {@link ResourceModel} of the subresource to add
   */
  public void addSubResource(final String path, final ResourceModel resourceModel)
  {
    _pathSubResourceMap.put(path, resourceModel);
  }

  /**
   * Get a sub-resource by name.
   *
   * @param subresourceName name of the sub-resource to get
   * @param <R> type of the resource model of the requested sub-resource
   * @return {@link ResourceModel} (or it's subclass R) of the requested sub-resource
   */
  @SuppressWarnings("unchecked")
  public <R extends ResourceModel> R getSubResource(final String subresourceName)
  {
    return (R) _pathSubResourceMap.get(subresourceName);
  }

  public Collection<ResourceModel> getSubResources()
  {
    return _pathSubResourceMap.values();
  }

  @Override
  public Map<String, ResourceDefinition> getSubResourceDefinitions()
  {
    return Collections.unmodifiableMap(_pathSubResourceMap);
  }

  /**
   * @return true if this resource has sub-resources, false otherwise
   */
  @Override
  public boolean hasSubResources()
  {
    return _pathSubResourceMap.size() > 0;
  }

  @Override
  public Class<?> getResourceClass()
  {
    return _resourceClass;
  }

  @Override
  public String getName()
  {
    return _name;
  }

  @Override
  public String getNamespace()
  {
    return _namespace;
  }

  @Override
  public boolean isRoot()
  {
    return _root;
  }

  public List<ResourceMethodDescriptor> getResourceMethodDescriptors()
  {
    return _resourceMethodDescriptors;
  }

  public boolean isActions()
  {
    return ResourceType.ACTIONS.equals(getResourceType());
  }

  public ResourceEntityType getResourceEntityType()
  {
    return UnstructuredDataUtil.getResourceEntityType(getResourceClass());
  }

  /**
   * @param type {@link ResourceMethod}
   * @param name method name
   * @param resourceLevel {@link ResourceLevel}
   * @return {@link ResourceMethodDescriptor} that matches the arguments or null if none
   *         match.
   */
  public final ResourceMethodDescriptor matchMethod(final ResourceMethod type,
                                                    final String name,
                                                    final ResourceLevel resourceLevel)
  {
    if (type.equals(ResourceMethod.ACTION))
    {
      return findActionMethod(name, resourceLevel);
    }
    else if (type.equals(ResourceMethod.FINDER))
    {
      return findFinderMethod(name);
    }
    else if (type.equals(ResourceMethod.BATCH_FINDER))
    {
      return findBatchFinderMethod(name);
    }
    else
    {
      return findMethod(type);
    }
  }

  /**
   * @param type {@link ResourceMethod} to find the method by
   * @return {@link ResourceMethodDescriptor} that matches the provided type or null if
   *         none match.
   */
  public final ResourceMethodDescriptor findMethod(final ResourceMethod type)
  {
    for (ResourceMethodDescriptor methodDescriptor : _resourceMethodDescriptors)
    {
      if (methodDescriptor.getType().equals(type))
      {
        return methodDescriptor;
      }
    }

    return null;
  }

  /**
   * @param actionName action name
   * @param resourceLevel {@link ResourceLevel}
   * @return {@link ResourceMethodDescriptor} of a method matching name and resourceLevel,
   *         null if none match.
   */
  public final ResourceMethodDescriptor findActionMethod(final String actionName,
                                                         final ResourceLevel resourceLevel)
  {
    for (ResourceMethodDescriptor methodDescriptor : _resourceMethodDescriptors)
    {
      if (methodDescriptor.getType().equals(ResourceMethod.ACTION)
              && actionName.equals(methodDescriptor.getActionName())
              && methodDescriptor.getActionResourceLevel().equals(resourceLevel))
      {
        return methodDescriptor;
      }
    }

    return null;
  }

  /**
   * @param batchFinderName method name
   * @return {@link ResourceMethodDescriptor} matching the name, null if none match
   */
  public final ResourceMethodDescriptor findBatchFinderMethod(final String batchFinderName)
  {
    for (ResourceMethodDescriptor methodDescriptor : _resourceMethodDescriptors)
    {
      if ((ResourceMethod.BATCH_FINDER.equals(methodDescriptor.getType()))
          && batchFinderName.equals(methodDescriptor.getBatchFinderName()))
      {
        return methodDescriptor;
      }
    }

    return null;
  }

  /**
   * @param finderName method name
   * @return {@link ResourceMethodDescriptor} matching the name, null if none match
   */
  public final ResourceMethodDescriptor findFinderMethod(final String finderName)
  {
    for (ResourceMethodDescriptor methodDescriptor : _resourceMethodDescriptors)
    {
      if ((ResourceMethod.FINDER.equals(methodDescriptor.getType()))
          && finderName.equals(methodDescriptor.getFinderName()))
      {
        return methodDescriptor;
      }
    }

    return null;
  }

  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((_name == null) ? 0 : _name.hashCode());
    result = prime * result + ((_resourceClass == null) ? 0 : _resourceClass.hashCode());
    return result;
  }

  @Override
  public boolean equals(final Object obj)
  {
    if (this == obj)
    {
      return true;
    }
    if (obj == null)
    {
      return false;
    }
    if (getClass() != obj.getClass())
    {
      return false;
    }
    ResourceModel other = (ResourceModel) obj;
    if (!_namespace.equals(other._namespace))
    {
      return false;
    }
    if (_name == null)
    {
      if (other._name != null)
      {
        return false;
      }
    }
    else if (!_name.equals(other._name))
    {
      return false;
    }
    if (_resourceClass == null)
    {
      if (other._resourceClass != null)
      {
        return false;
      }
    }
    else if (!_resourceClass.equals(other._resourceClass))
    {
      return false;
    }
    return true;
  }

  public String getKeyName()
  {
    return _primaryKey == null ? null : _primaryKey.getName();
  }

  public Key getPrimaryKey()
  {
    return _primaryKey;
  }

  public Class<? extends RecordTemplate> getKeyKeyClass()
  {
    return _keyKeyClass;
  }

  public Class<? extends RecordTemplate> getKeyParamsClass()
  {
    return _keyParamsClass;
  }

  public DataMap getCustomAnnotationData()
  {
    return _customAnnotations;
  }

  /**
   * Gets an immutable view of the expected service errors for this resource, or null if errors aren't defined.
   * @return {@link List}&#60;{@link ServiceError}&#62;
   */
  public List<ServiceError> getServiceErrors()
  {
    return _serviceErrors == null ? null : Collections.unmodifiableList(_serviceErrors);
  }

  /**
   * Sets the list of expected service errors for this resource.
   * Note that a null list and an empty list are semantically different (see {@link ServiceErrors}).
   * @param serviceErrors {@link List}&#60;{@link ServiceError}&#62;
   */
  public void setServiceErrors(final Collection<ServiceError> serviceErrors)
  {
    _serviceErrors = serviceErrors == null ? null : new ArrayList<>(serviceErrors);
  }

  /**
   * Returns <code>true</code> if this resource or any of its resource methods define a set of expected service errors.
   * This should correspond with whether the original resource class or any of its methods were annotated with a
   * {@link ServiceErrors} annotation.
   */
  public boolean isAnyServiceErrorListDefined()
  {
    return _serviceErrors != null || _resourceMethodDescriptors.stream()
        .map(ResourceMethodDescriptor::getServiceErrors)
        .anyMatch(Objects::nonNull);
  }

  /**
   * Collect all the data schemas referenced by this model into the given set.
   */
  @Override
  public void collectReferencedDataSchemas(Set<DataSchema> schemas)
  {
    // Add schemas referenced by method descriptors.
    _resourceMethodDescriptors.forEach(descriptor -> descriptor.collectReferencedDataSchemas(schemas));

    // Add complex resource key RecordTemplate-derived constituents Key and Params
    if (_keyKeyClass != null)
    {
      try
      {
        schemas.add(DataTemplateUtil.getSchema(_keyKeyClass));
      }
      catch (TemplateRuntimeException e)
      {
        LOGGER.debug("Failed to get schema for complex key type: " + _keyKeyClass.getName(), e);
      }
    }

    if (_keyParamsClass != null)
    {
      try
      {
        schemas.add(DataTemplateUtil.getSchema(_keyParamsClass));
      }
      catch (TemplateRuntimeException e)
      {
        LOGGER.debug("Failed to get schema for complex param type: " + _keyParamsClass.getName(), e);
      }
    }

    // Add value class
    if (_valueClass != null)
    {
      try
      {
        schemas.add(DataTemplateUtil.getSchema(_valueClass));
      }
      catch (TemplateRuntimeException e)
      {
        LOGGER.debug("Failed to get schema for value class: " + _valueClass.getName(), e);
      }
    }

    // Add resource keys
    if (_keys != null)
    {
      for (Key key : _keys)
      {
        DataSchema schema = key.getDataSchema();
        if (schema != null)
        {
          schemas.add(schema);
        }
      }
    }

    // Add alternate keys.
    if (_alternativeKeys != null)
    {
      for (AlternativeKey<?, ?> alternativeKey : _alternativeKeys.values())
      {
        DataSchema schema = alternativeKey.getDataSchema();
        if (schema != null)
        {
          schemas.add(schema);
        }
      }
    }

    // Recurse over all sub-resources and repeat.
    if (hasSubResources())
    {
      getSubResources().forEach(subResourceModel -> subResourceModel.collectReferencedDataSchemas(schemas));
    }
  }
}
