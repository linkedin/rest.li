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
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.server.Key;
import com.linkedin.restli.server.ResourceLevel;
import com.linkedin.restli.server.resources.ComplexKeyResource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author dellamag
 */
public class ResourceModel
{
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

  private final Map<String, Class<?>>           _keyClasses;
  private final Class<? extends RecordTemplate> _valueClass;

  private final List<ResourceMethodDescriptor>  _resourceMethodDescriptors;

  private final Map<String, ResourceModel>      _pathSubResourceMap;

  private DataMap                               _customAnnotations;

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
    _keyClasses = new HashMap<String, Class<?>>((int) Math.ceil(_keys.size() / 0.75));
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
    _resourceMethodDescriptors = new ArrayList<ResourceMethodDescriptor>(5);
    _primaryKey = primaryKey;
    _resourceType = resourceType;
    _pathSubResourceMap = new HashMap<String, ResourceModel>();
  }

  /**
   * Constructor.
   *
   * @param keyClass resource key class
   * @param keyKeyClass class of the key part of a {@link ComplexResourceKey} if this is a
   *          {@link ComplexKeyResource}
   * @param keyParamsClass class of the param part of a {@link ComplexResourceKey} if this
   *          is a {@link ComplexKeyResource}
   * @param keys set of resource keys
   * @param valueClass resource value class
   * @param resourceClass resource class
   * @param parentResourceClass parent resource class
   * @param name resource name
   * @param keyName resource key name
   * @param resourceType {@link ResourceType}
   * @param namespace namespace
   *
   * @deprecated should pass in a fully formed primary key rather than a class and a name
   */
  @Deprecated
  public ResourceModel(final Class<?> keyClass,
                       final Class<? extends RecordTemplate> keyKeyClass,
                       final Class<? extends RecordTemplate> keyParamsClass,
                       final Set<Key> keys,
                       final Class<? extends RecordTemplate> valueClass,
                       final Class<?> resourceClass,
                       final Class<?> parentResourceClass,
                       final String name,
                       final String keyName,
                       final ResourceType resourceType,
                       final String namespace)
  {
    this(new Key(keyName, keyClass),
         keyKeyClass,
         keyParamsClass,
         keys, valueClass,
         resourceClass,
         parentResourceClass,
         name,
         resourceType,
         namespace);
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
    Set<String> keyNames = new HashSet<String>();
    for (Key key : _keys)
    {
      keyNames.add(key.getName());
    }
    return keyNames;
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

  public Iterable<ResourceModel> getSubResources()
  {
    return _pathSubResourceMap.values();
  }

  /**
   * @return true if this resource has sub-resources, false otherwise
   */
  public boolean hasSubResources()
  {
    return _pathSubResourceMap.size() > 0;
  }

  public Class<?> getResourceClass()
  {
    return _resourceClass;
  }


  public String getName()
  {
    return _name;
  }

  public String getNamespace()
  {
    return _namespace;
  }

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
      return findNamedMethod(name);
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
   * @param name method name
   * @return {@link ResourceMethodDescriptor} matching the name, null if none match
   */
  public final ResourceMethodDescriptor findNamedMethod(final String name)
  {
    for (ResourceMethodDescriptor methodDescriptor : _resourceMethodDescriptors)
    {
      if ((ResourceMethod.FINDER.equals(methodDescriptor.getType()))
          && name.equals(methodDescriptor.getFinderName()))
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
}
