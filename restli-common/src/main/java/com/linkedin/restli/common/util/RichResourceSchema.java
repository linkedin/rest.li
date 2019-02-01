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

package com.linkedin.restli.common.util;


import com.linkedin.data.template.StringArray;
import com.linkedin.restli.restspec.ActionSchema;
import com.linkedin.restli.restspec.ActionSchemaArray;
import com.linkedin.restli.restspec.ActionsSetSchema;
import com.linkedin.restli.restspec.AssociationSchema;
import com.linkedin.restli.restspec.BatchFinderSchema;
import com.linkedin.restli.restspec.BatchFinderSchemaArray;
import com.linkedin.restli.restspec.CollectionSchema;
import com.linkedin.restli.restspec.CustomAnnotationContentSchemaMap;
import com.linkedin.restli.restspec.EntitySchema;
import com.linkedin.restli.restspec.FinderSchema;
import com.linkedin.restli.restspec.FinderSchemaArray;
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.restli.restspec.RestMethodSchema;
import com.linkedin.restli.restspec.RestMethodSchemaArray;
import com.linkedin.restli.restspec.SimpleSchema;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;


/**
 * Provides convenience accessor methods to the generated ResourceSchema DataTemplate, particular those fields
 * that are common to all the four different major types of resources (such as methods, finders, actions, subresources)
 * that are burdonsome to access manually given how the ResourceSchema has been defined and generated.
 *
 * @author jbetz@linkedin.com
 */
public class RichResourceSchema
{
  public static Collection<RichResourceSchema> toRichResourceSchemas(Collection<ResourceSchema> resourceSchemas)
  {
    ArrayList<RichResourceSchema> results = new ArrayList<RichResourceSchema>(resourceSchemas.size());
    for(ResourceSchema resourceSchema : resourceSchemas)
    {
      results.add(new RichResourceSchema(resourceSchema));
    }
    return results;
  }

  public enum ResourceType
  {
    COLLECTION,
    ASSOCIATION,
    SIMPLE,
    ACTION_SET
  }

  private final ResourceSchema _resourceSchema;

  private final ResourceType _type;
  private final StringArray _supports;
  private final RestMethodSchemaArray _methods;
  private final FinderSchemaArray _finders;
  private final BatchFinderSchemaArray _batchFinders;
  private final ActionSchemaArray _actions;
  private final EntitySchema _entity;
  private final ActionSchemaArray _entityActions;
  private final Collection<RichResourceSchema> _subresources;

  private final Map<String, RestMethodSchema> _methodsByName;
  private final Map<String, FinderSchema> _findersByName;
  private final Map<String, BatchFinderSchema> _batchFindersByName;
  private final Map<String, ActionSchema> _actionsByName;
  private final Map<String, ActionSchema> _entityActionsByName;
  private final Map<String, RichResourceSchema> _subresourcesByName;

  public RichResourceSchema(ResourceSchema resourceSchema)
  {
    _resourceSchema = resourceSchema;

    if(resourceSchema.hasCollection())
    {
      CollectionSchema collection = resourceSchema.getCollection();
      _type = ResourceType.COLLECTION;
      _supports = collection.getSupports();
      _methods = collection.hasMethods() ? collection.getMethods() : new RestMethodSchemaArray(0);
      _finders = collection.hasFinders() ? collection.getFinders() : new FinderSchemaArray(0);
      _batchFinders = collection.hasBatchFinders() ? collection.getBatchFinders() : new BatchFinderSchemaArray(0);
      _actions = collection.hasActions() ? collection.getActions() : new ActionSchemaArray(0);
      _entity = collection.getEntity();

    }
    else if(resourceSchema.hasAssociation())
    {
      _type = ResourceType.ASSOCIATION;
      AssociationSchema association = resourceSchema.getAssociation();
      _supports = association.getSupports();
      _methods = association.hasMethods() ? association.getMethods() : new RestMethodSchemaArray(0);
      _finders = association.hasFinders() ? association.getFinders() : new FinderSchemaArray(0);
      _batchFinders = association.hasBatchFinders() ? association.getBatchFinders() : new BatchFinderSchemaArray(0);
      _actions = association.hasActions() ? association.getActions() : new ActionSchemaArray(0);
      _entity = association.getEntity();

    }
    else if(resourceSchema.hasSimple())
    {
      _type = ResourceType.SIMPLE;
      SimpleSchema simple = resourceSchema.getSimple();
      _supports = simple.getSupports();
      _methods = simple.hasMethods() ? simple.getMethods() : new RestMethodSchemaArray(0);
      _finders = new FinderSchemaArray(0);
      _batchFinders = new BatchFinderSchemaArray(0);
      _actions = new ActionSchemaArray(0);
      _entity = simple.getEntity();
    }
    else if(resourceSchema.hasActionsSet())
    {
      _type = ResourceType.ACTION_SET;
      ActionsSetSchema actionSet = resourceSchema.getActionsSet();
      _supports = new StringArray(0);
      _methods = new RestMethodSchemaArray(0);
      _finders = new FinderSchemaArray(0);
      _batchFinders = new BatchFinderSchemaArray(0);
      _actions = actionSet.hasActions() ? actionSet.getActions() : new ActionSchemaArray(0);
      _entity = null;
    }
    else
    {
      throw new IllegalArgumentException("Invalid resourceSchema, must be one of: " + EnumSet.allOf(ResourceType.class));
    }

    if(resourceSchema.hasSimple())
    {
      SimpleSchema simple = resourceSchema.getSimple();
      _entityActions = simple.hasActions() ? simple.getActions() : new ActionSchemaArray(0);
    }
    else if(_entity != null)
    {
      _entityActions = _entity.hasActions() ? _entity.getActions() : new ActionSchemaArray(0);
    }
    else
    {
      _entityActions = new ActionSchemaArray(0);
    }

    if(_entity != null)
    {
      _subresources = _entity.hasSubresources() ? toRichResourceSchemas(_entity.getSubresources()) : Collections.<RichResourceSchema>emptyList();
    }
    else
    {
      _subresources = Collections.emptyList();
    }

    _methodsByName = new HashMap<String, RestMethodSchema>(_methods.size());
    for(RestMethodSchema method : _methods)
    {
      _methodsByName.put(method.getMethod(), method);
    }

    _findersByName = new HashMap<String, FinderSchema>(_finders.size());
    for(FinderSchema finder : _finders)
    {
      _findersByName.put(finder.getName(), finder);
    }

    _batchFindersByName = new HashMap<String, BatchFinderSchema>(_batchFinders.size());
    for(BatchFinderSchema batchFinder : _batchFinders)
    {
      _batchFindersByName.put(batchFinder.getName(), batchFinder);
    }

    _actionsByName = new HashMap<String, ActionSchema>(_actions.size());
    for(ActionSchema action : _actions)
    {
      _actionsByName.put(action.getName(), action);
    }

    _entityActionsByName = new HashMap<String, ActionSchema>(_entityActions.size());
    for(ActionSchema entityAction : _entityActions)
    {
      _entityActionsByName.put(entityAction.getName(), entityAction);
    }

    _subresourcesByName = new HashMap<String, RichResourceSchema>(_subresources.size());
    for(RichResourceSchema subresource : _subresources)
    {
      _subresourcesByName.put(subresource.getName(), subresource);
    }
  }

  public String getName()
  {
    return _resourceSchema.getName();
  }

  public String getNamespace()
  {
    return _resourceSchema.getNamespace();
  }

  public String getPath()
  {
    return _resourceSchema.getPath();
  }

  public String getSchema()
  {
    return _resourceSchema.getSchema();
  }

  public String getDoc()
  {
    return _resourceSchema.getDoc();
  }

  public CustomAnnotationContentSchemaMap getAnnotations()
  {
    return _resourceSchema.getAnnotations();
  }

  public ResourceSchema getResourceSchema()
  {
    return _resourceSchema;
  }

  public ResourceType getType()
  {
    return _type;
  }

  public StringArray getSupports()
  {
    return _supports;
  }

  public RestMethodSchemaArray getMethods()
  {
    return _methods;
  }

  public RestMethodSchema getMethod(String name)
  {
    return _methodsByName.get(name);
  }

  public FinderSchemaArray getFinders()
  {
    return _finders;
  }

  public FinderSchema getFinder(String name)
  {
    return _findersByName.get(name);
  }

  public BatchFinderSchemaArray getBatchFinders()
  {
    return _batchFinders;
  }

  public BatchFinderSchema getBatchFinder(String name)
  {
    return _batchFindersByName.get(name);
  }

  public ActionSchemaArray getActions()
  {
    return _actions;
  }

  public ActionSchema getAction(String name)
  {
    return _actionsByName.get(name);
  }

  public EntitySchema getEntity()
  {
    return _entity;
  }

  public ActionSchemaArray getEntityActions()
  {
    return _entityActions;
  }

  public ActionSchema getEntityAction(String name)
  {
    return _entityActionsByName.get(name);
  }

  public Collection<RichResourceSchema> getSubresources()
  {
    return _subresources;
  }

  public RichResourceSchema getSubresource(String name)
  {
    return _subresourcesByName.get(name);
  }
}
