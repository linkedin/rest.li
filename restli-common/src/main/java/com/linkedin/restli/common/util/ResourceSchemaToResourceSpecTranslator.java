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


import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.DataSchemaUtil;
import com.linkedin.data.schema.EnumDataSchema;
import com.linkedin.data.schema.TyperefDataSchema;
import com.linkedin.data.template.DataTemplate;
import com.linkedin.data.template.DynamicRecordMetadata;
import com.linkedin.data.template.FieldDef;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.StringArray;
import com.linkedin.restli.common.ComplexKeySpec;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.ResourceSpecImpl;
import com.linkedin.restli.common.TypeSpec;
import com.linkedin.restli.restspec.ActionSchema;
import com.linkedin.restli.restspec.ActionSchemaArray;
import com.linkedin.restli.restspec.ActionsSetSchema;
import com.linkedin.restli.restspec.AssocKeySchema;
import com.linkedin.restli.restspec.AssocKeySchemaArray;
import com.linkedin.restli.restspec.AssociationSchema;
import com.linkedin.restli.restspec.CollectionSchema;
import com.linkedin.restli.restspec.IdentifierSchema;
import com.linkedin.restli.restspec.ParameterSchema;
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.restli.restspec.RestSpecCodec;
import com.linkedin.restli.restspec.SimpleSchema;
import com.linkedin.util.CustomTypeUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Translates ResourceSchemas to a ResourceSpecs.
 *
 * Uses the provided DataSchemaResolver to locate all data schemas.
 *
 * Uses the provided ClassBindingResolver to locate all DataTemplate classes and enum classes, since they are not
 * available from the ResourceSchema.
 *
 * It's important to note that all data schema that is injected into the create ResourceSpec is taken from the
 * DataSchemaResolver, not the DataTemplate classes from the ClassBindingResolver.  This is required for use cases
 * where DataTemplate classes are not available for all data schemas.  An example of this is rest.li documentation
 * system, that, at runtime, receives ResourceSchemas about rest.li resources, and also at runtime, constructs
 * a ResourceSpec for that resource so that it can generate example resource URIs and requests using all the classes
 * that require ResourceSpec, especially classes in the RequestBuilder and RestliUriBuilder hierarchies.  In such cases,
 * a ClassBindingResolver that returns DataTemplate classes of the correct type and with correct constructors can be
 * used and all the data schema information will be resolved using the DataSchemaResolver.
 *
 */
public class ResourceSchemaToResourceSpecTranslator
{
  /**
   * Provides resolution of class bindings, which are not available directly from the ResourceSchema during translation.
   */
  public interface ClassBindingResolver
  {
    @SuppressWarnings("rawtypes")
    Class<? extends DataTemplate> resolveTemplateClass(DataSchema dataSchema);

    @SuppressWarnings("rawtypes")
    Class<? extends Enum> resolveEnumClass(EnumDataSchema dataSchema);
  }

  private final DataSchemaResolver _schemaResolver;
  private final ClassBindingResolver _bindingResolver;

  public ResourceSchemaToResourceSpecTranslator(DataSchemaResolver schemaResolver,
                                                ClassBindingResolver bindingResolver)
  {
    _schemaResolver = schemaResolver;
    _bindingResolver = bindingResolver;
  }

  /**
   * Translates a ResourceSchema to a ResourceSpec.
   *
   * @param resourceSchema provides the schema to convert.
   * @return a generated ResourceSpec that represents the same resource as the given ResourceSchema.
   */
  public ResourceSpec translate(ResourceSchema resourceSchema)
  {
    if(resourceSchema.hasCollection())
    {
      CollectionSchema collection = resourceSchema.getCollection();
      return collectionToResourceSpec(resourceSchema, collection);
    }
    else if(resourceSchema.hasAssociation())
    {
      AssociationSchema association = resourceSchema.getAssociation();
      return associationToResourceSpec(resourceSchema, association);
    }
    else if(resourceSchema.hasActionsSet())
    {
      ActionsSetSchema actionsSet = resourceSchema.getActionsSet();
      return actionSetToResourceSpec(actionsSet);
    }
    else if(resourceSchema.hasSimple())
    {
      SimpleSchema simple = resourceSchema.getSimple();
      return simpleToResourceSpec(resourceSchema, simple);
    }
    else
    {
      throw new IllegalStateException("ResourceSchema does not have any of the recognized types (collection, association, actionSet, simple), exactly one is required.");
    }
  }

  @SuppressWarnings("rawtypes")
  private ResourceSpec collectionToResourceSpec(ResourceSchema resourceSchema, CollectionSchema collection)
  {
    ActionSchemaArray actions = null, entityActions = null;
    StringArray supports = collection.getSupports();
    if(collection.hasActions())
    {
      actions = collection.getActions();
    }
    if(collection.getEntity().hasActions())
    {
      entityActions = collection.getEntity().getActions();
    }
    String schema = resourceSchema.getSchema();
    IdentifierSchema identifier = collection.getIdentifier();
    if(identifier.getParams() == null) // in this case we have a "simple" collection resource
    {
      DataSchema key = RestSpecCodec.textToSchema(identifier.getType(), _schemaResolver);
      return buildResourceSpec(supports,
                               toTypeSpec(key),
                               null,
                               Collections.<String, Object>emptyMap(),
                               schema,
                               actions,
                               entityActions);
    }
    else // we have a complex collection resource
    {
      DataSchema keyKeyType = RestSpecCodec.textToSchema(identifier.getType(), _schemaResolver);
      DataSchema keyParamsType = RestSpecCodec.textToSchema(identifier.getParams(), _schemaResolver);
      ComplexKeySpec<?, ?> complexKeyType = toComplexKey(keyKeyType, keyParamsType);
      return buildResourceSpec(supports,
          new TypeSpec<>(ComplexResourceKey.class, null),
                               complexKeyType,
                               Collections.<String, Object>emptyMap(),
                               schema,
                               actions,
                               entityActions);
    }
  }

  private ResourceSpec associationToResourceSpec(ResourceSchema resourceSchema, AssociationSchema association)
  {
    ActionSchemaArray actions = null, entityActions = null;
    StringArray supports = association.getSupports();
    if(association.hasActions())
    {
      actions = association.getActions();
    }
    if(association.getEntity().hasActions())
    {
      entityActions = association.getEntity().getActions();
    }
    String schema = resourceSchema.getSchema();
    AssocKeySchemaArray assocKeys = association.getAssocKeys();

    Map<String, CompoundKey.TypeInfo> keyParts = new HashMap<>();
    for (AssocKeySchema assocKey : assocKeys)
    {
      TypeSpec<?> type = toTypeSpec(RestSpecCodec.textToSchema(assocKey.getType(), _schemaResolver));
      keyParts.put(assocKey.getName(), new CompoundKey.TypeInfo(type, type));
    }
    return buildResourceSpec(supports,
        new TypeSpec<>(CompoundKey.class, null),
                             null,
                             keyParts,
                             schema,
                             actions,
                             entityActions);
  }

  private ResourceSpec actionSetToResourceSpec(ActionsSetSchema actionsSet)
  {
    ActionSchemaArray actions = null;
    if(actionsSet.hasActions())
    {
      actions = actionsSet.getActions();
    }
    return buildResourceSpec(new StringArray(0),
        new TypeSpec<>(Void.class),
                             null,
                             Collections.<String, Object>emptyMap(),
                             null,
                             actions,
                             null);
  }

  private ResourceSpec simpleToResourceSpec(ResourceSchema resourceSchema, SimpleSchema simple)
  {
    ActionSchemaArray entityActions = null;
    StringArray supports = simple.getSupports();
    if(simple.hasActions())
    {
      entityActions = simple.getActions();
    }
    String schema = resourceSchema.getSchema();

    return buildResourceSpec(supports,
                             null,
                             null,
                             Collections.<String, Object>emptyMap(),
                             schema,
                             null,
                             entityActions);
  }

  private ResourceSpec buildResourceSpec(StringArray supports,
                                         TypeSpec<?> key,
                                         ComplexKeySpec<?, ?> complexKeyType,
                                         Map<String, ?> keyParts,
                                         String entitySchemaString,
                                         ActionSchemaArray actions,
                                         ActionSchemaArray entityActions)
  {
    Set<ResourceMethod> supportedMethods = toResourceMethods(supports);

    ActionCollectionMetadata actionCollectionMetadata = toDynamicRecordMetadata(actions, entityActions);
    Map<String, DynamicRecordMetadata> actionRequestMetadata = actionCollectionMetadata._request;
    Map<String, DynamicRecordMetadata> actionResponseMetadata = actionCollectionMetadata._response;
    DataSchema entitySchema = entitySchemaString == null ? null : RestSpecCodec.textToSchema(entitySchemaString, _schemaResolver);
    TypeSpec<? extends RecordTemplate> value = toValueType(entitySchema);

    return new ResourceSpecImpl(supportedMethods,
                                actionRequestMetadata,
                                actionResponseMetadata,
                                key, complexKeyType,
                                value,
                                keyParts);
  }

  private Set<ResourceMethod> toResourceMethods(StringArray supports)
  {
    if(supports == null) return Collections.emptySet();
    Set<ResourceMethod> resourceMethods = new HashSet<>();
    for(String method : supports)
    {
      resourceMethods.add(ResourceMethod.fromString(method));
    }
    return resourceMethods;
  }

  private static class ActionCollectionMetadata
  {
    public Map<String, DynamicRecordMetadata> _response;
    public Map<String, DynamicRecordMetadata> _request;

    public ActionCollectionMetadata(Map<String, DynamicRecordMetadata> request,
                                    Map<String, DynamicRecordMetadata> response)
    {
      _request = request;
      _response = response;
    }
  }

  private static class ActionMetadata
  {
    public String _name;
    public DynamicRecordMetadata _response;
    public DynamicRecordMetadata _request;

    public ActionMetadata(String name,
                          DynamicRecordMetadata request,
                          DynamicRecordMetadata response)
    {
      _name = name;
      _request = request;
      _response = response;
    }
  }

  private ActionCollectionMetadata toDynamicRecordMetadata(ActionSchemaArray actions, ActionSchemaArray entityActions)
  {
    Map<String, DynamicRecordMetadata> response = new HashMap<>();
    Map<String, DynamicRecordMetadata> request = new HashMap<>();

    ActionSchemaArray[] actionGroups = new ActionSchemaArray[] { actions, entityActions };
    for(ActionSchemaArray actionGroup: actionGroups)
    {
      if(actionGroup != null)
      {
        for(ActionSchema action : actionGroup)
        {
          ActionMetadata metadata = toActionMetadata(action);
          request.put(metadata._name, metadata._request);
          response.put(metadata._name, metadata._response);
        }
      }
    }
    return new ActionCollectionMetadata(request, response);
  }

  @SuppressWarnings({"unchecked", "rawtypes"}) // this is dynamic, don't have concrete classes for the FieldDef
  private ActionMetadata toActionMetadata(ActionSchema action)
  {
    ArrayList<FieldDef<?>> fieldDefs = new ArrayList<>();
    if(action.hasParameters())
    {
      for(ParameterSchema parameterSchema : action.getParameters())
      {
        DataSchema dataSchema = RestSpecCodec.textToSchema(parameterSchema.getType(), _schemaResolver);
        Class<?> paramClass = toType(dataSchema);
        FieldDef<?> fieldDef = new FieldDef(parameterSchema.getName(),
                                                           paramClass,
                                                           dataSchema);
        fieldDefs.add(fieldDef);
      }
    }

    Collection<FieldDef<?>> response;
    if(action.hasReturns())
    {
      DataSchema returnType = RestSpecCodec.textToSchema(action.getReturns(), _schemaResolver);
      Class<?> returnClass = toType(returnType);
      response = Collections.<FieldDef<?>>singletonList(new FieldDef("value", returnClass, returnType));
    }
    else
    {
      response = Collections.emptyList();
    }
    return new ActionMetadata(action.getName(),
                              new DynamicRecordMetadata(action.getName(), fieldDefs),
                              new DynamicRecordMetadata(action.getName(), response));
  }

  @SuppressWarnings({"unchecked", "rawtypes"}) // this is dynamic, don't have concrete classes for the TypeSpec
  private TypeSpec<?> toTypeSpec(DataSchema schema)
  {
    return new TypeSpec(toType(schema), schema.getDereferencedDataSchema());
  }

  public Class<?> toType(DataSchema schema)
  {
    if (schema.getType() == DataSchema.Type.TYPEREF)
    {
      Class<?> javaClass = CustomTypeUtil.getJavaCustomTypeClassFromSchema((TyperefDataSchema) schema);
      if (javaClass != null) return javaClass;
    }
    DataSchema.Type dereferencedType = schema.getDereferencedType();
    DataSchema dereferencedDataSchema = schema.getDereferencedDataSchema();
    if(dereferencedDataSchema.isPrimitive())
    {
      return DataSchemaUtil.dataSchemaTypeToPrimitiveDataSchemaClass(dereferencedType);
    }
    else if(dereferencedDataSchema instanceof EnumDataSchema)
    {
      return _bindingResolver.resolveEnumClass((EnumDataSchema)dereferencedDataSchema);
    }
    else
    {
      return _bindingResolver.resolveTemplateClass(dereferencedDataSchema);
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"}) // this is dynamic, don't have concrete classes for the TypeSpec
  public ComplexKeySpec<?, ?> toComplexKey(DataSchema keyDataSchema, DataSchema paramsDataSchema)
  {
    TypeSpec<? extends RecordTemplate> complexKeyKey = toRecordTemplateType(keyDataSchema);
    TypeSpec<? extends RecordTemplate> complexKeyParams = toRecordTemplateType(paramsDataSchema);
    return new ComplexKeySpec(complexKeyKey, complexKeyParams);
  }

  public TypeSpec<? extends RecordTemplate> toValueType(DataSchema schema)
  {
    return toRecordTemplateType(schema);
  }

  @SuppressWarnings({"unchecked", "rawtypes"}) // this is dynamic, don't have concrete classes for the TypeSpec
  private TypeSpec<? extends RecordTemplate> toRecordTemplateType(DataSchema schema)
  {
    if(schema == null) return null;
    return new TypeSpec(_bindingResolver.resolveTemplateClass(schema), schema);
  }
}
