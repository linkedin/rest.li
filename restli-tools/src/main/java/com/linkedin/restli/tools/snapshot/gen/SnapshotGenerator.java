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

package com.linkedin.restli.tools.snapshot.gen;


import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.JsonBuilder;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.SchemaToJsonEncoder;
import com.linkedin.data.schema.TyperefDataSchema;
import com.linkedin.data.schema.UnionDataSchema;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.restspec.ActionSchema;
import com.linkedin.restli.restspec.ActionsSetSchema;
import com.linkedin.restli.restspec.AssocKeySchema;
import com.linkedin.restli.restspec.AssociationSchema;
import com.linkedin.restli.restspec.BatchFinderSchema;
import com.linkedin.restli.restspec.CollectionSchema;
import com.linkedin.restli.restspec.EntitySchema;
import com.linkedin.restli.restspec.FinderSchema;
import com.linkedin.restli.restspec.IdentifierSchema;
import com.linkedin.restli.restspec.MetadataSchema;
import com.linkedin.restli.restspec.ParameterSchema;
import com.linkedin.restli.restspec.ParameterSchemaArray;
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.restli.restspec.RestMethodSchema;
import com.linkedin.restli.restspec.RestSpecCodec;
import com.linkedin.restli.restspec.ServiceErrorSchema;
import com.linkedin.restli.restspec.ServiceErrorSchemaArray;
import com.linkedin.restli.restspec.ServiceErrorsSchema;
import com.linkedin.restli.restspec.SimpleSchema;
import com.linkedin.restli.tools.snapshot.check.Snapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author Moira Tagle
 */
public class SnapshotGenerator
{
  private ResourceSchema _topLevelSchema;
  private DataSchemaResolver _schemaResolver;

  public SnapshotGenerator(ResourceSchema resourceSchema, DataSchemaResolver schemaResolver)
  {
    _topLevelSchema = resourceSchema;
    _schemaResolver = schemaResolver;
  }

  public List<NamedDataSchema> generateModelList()
  {
    List<NamedDataSchema> result = new ArrayList<NamedDataSchema>();
    Map<String, NamedDataSchema> map = new HashMap<String, NamedDataSchema>();
    findModelsResource(_topLevelSchema, map, result);
    return result;
  }

  public File writeFile(File outdirFile, String fileName) throws IOException
  {
    fileName += RestConstants.SNAPSHOT_FILENAME_EXTENTION;
    final File file = new File(outdirFile, fileName);
    try (JsonBuilder jsonBuilder = new JsonBuilder(JsonBuilder.Pretty.INDENTED, new FileWriter(file)))
    {
      SchemaToJsonEncoder encoder = new SchemaToJsonEncoder(jsonBuilder);

      jsonBuilder.writeStartObject();
      jsonBuilder.writeFieldName(Snapshot.MODELS_KEY);
      jsonBuilder.writeStartArray();

      List<NamedDataSchema> models = generateModelList();
      models.sort(Comparator.comparing(NamedDataSchema::getFullName));

      for (DataSchema model : models)
      {
        encoder.encode(model);
      }

      jsonBuilder.writeEndArray();

      jsonBuilder.writeFieldName(Snapshot.SCHEMA_KEY);
      jsonBuilder.writeDataTemplate(_topLevelSchema, true);

      jsonBuilder.writeEndObject();
      jsonBuilder.flush();
    }

    return file;
  }

  private void findModelsResource(ResourceSchema resourceSchema, Map<String, NamedDataSchema> foundTypes, List<NamedDataSchema> typeOrder)
  {
    String schema = resourceSchema.getSchema();
    if (schema != null)
    {
      recordType(schema, foundTypes, typeOrder);
    }

    findModelsActionSet(resourceSchema, foundTypes, typeOrder);

    findModelsAssocation(resourceSchema, foundTypes, typeOrder);

    findModelsCollection(resourceSchema, foundTypes, typeOrder);

    findModelsSimple(resourceSchema, foundTypes, typeOrder);
  }

  private void findModelsCollection(ResourceSchema resourceSchema, Map<String, NamedDataSchema> foundTypes, List<NamedDataSchema> typeOrder)
  {
    CollectionSchema collection = resourceSchema.getCollection();
    if (collection != null)
    {
      IdentifierSchema identifier = collection.getIdentifier();
      findModelsIdentifier(identifier, foundTypes, typeOrder);
      findErrorDetailTypeModels(collection, foundTypes, typeOrder);
      if (collection.hasFinders())
      {
        for (FinderSchema restMethodSchema: collection.getFinders())
        {
          findModels(restMethodSchema.getParameters(), restMethodSchema.getMetadata(), foundTypes, typeOrder);
          findErrorDetailTypeModels(restMethodSchema, foundTypes, typeOrder);
        }
      }
      if (collection.hasBatchFinders())
      {
        for (BatchFinderSchema restMethodSchema: collection.getBatchFinders())
        {
          findModels(restMethodSchema.getParameters(), restMethodSchema.getMetadata(), foundTypes, typeOrder);
          findErrorDetailTypeModels(restMethodSchema, foundTypes, typeOrder);
        }
      }
      if (collection.hasMethods())
      {
        for (RestMethodSchema restMethodSchema : collection.getMethods())
        {
          findModels(restMethodSchema.getParameters(), restMethodSchema.getMetadata(), foundTypes, typeOrder);
          findErrorDetailTypeModels(restMethodSchema, foundTypes, typeOrder);
        }
      }
      if (collection.hasActions())
      {
        for (ActionSchema actionSchema : collection.getActions())
        {
          findModelsAction(actionSchema, foundTypes, typeOrder);
          findErrorDetailTypeModels(actionSchema, foundTypes, typeOrder);
        }
      }
      if (collection.hasEntity())
      {
        EntitySchema entity = collection.getEntity();
        findModelsEntity(entity, foundTypes, typeOrder);
      }
    }
  }

  private void findModelsEntity(EntitySchema entity, Map<String, NamedDataSchema> foundTypes, List<NamedDataSchema> typeOrder)
  {
    if (entity.hasActions())
    {
      for(ActionSchema actionSchema : entity.getActions())
      {
        findModelsAction(actionSchema, foundTypes, typeOrder);
      }
    }
    if (entity.hasSubresources())
    {
      for(ResourceSchema subresourceSchema : entity.getSubresources())
      {
        findModelsResource(subresourceSchema, foundTypes, typeOrder);
      }
    }
  }

  private void findModelsIdentifier(IdentifierSchema identifier, Map<String, NamedDataSchema> foundTypes, List<NamedDataSchema> typeOrder)
  {
    String identifierType = identifier.getType();
    recordType(identifierType, foundTypes, typeOrder);

    String paramsType = identifier.getParams();
    if(paramsType != null)
    {
      recordType(paramsType, foundTypes, typeOrder);
    }
  }

  private void findModelsAssocation(ResourceSchema resourceSchema, Map<String, NamedDataSchema> foundTypes, List<NamedDataSchema> typeOrder)
  {
    AssociationSchema association = resourceSchema.getAssociation();
    if (association != null)
    {
      findErrorDetailTypeModels(association, foundTypes, typeOrder);
      for (AssocKeySchema assocKeySchema : association.getAssocKeys())
      {
        String type = assocKeySchema.getType();
        recordType(type, foundTypes, typeOrder);
      }
      if (association.hasFinders())
      {
        for (FinderSchema restMethodSchema: association.getFinders())
        {
          findModels(restMethodSchema.getParameters(),restMethodSchema.getMetadata(), foundTypes, typeOrder);
          findErrorDetailTypeModels(restMethodSchema, foundTypes, typeOrder);
        }
      }
      if (association.hasBatchFinders())
      {
        for (BatchFinderSchema restMethodSchema: association.getBatchFinders())
        {
          findModels(restMethodSchema.getParameters(), restMethodSchema.getMetadata(), foundTypes, typeOrder);
          findErrorDetailTypeModels(restMethodSchema, foundTypes, typeOrder);
        }
      }
      if (association.hasMethods())
      {
        for (RestMethodSchema restMethodSchema: association.getMethods())
        {
          findModels(restMethodSchema.getParameters(),restMethodSchema.getMetadata(), foundTypes, typeOrder);
          findErrorDetailTypeModels(restMethodSchema, foundTypes, typeOrder);
        }
      }
      if (association.hasActions())
      {
        for (ActionSchema actionSchema : association.getActions())
        {
          findModelsAction(actionSchema, foundTypes, typeOrder);
          findErrorDetailTypeModels(actionSchema, foundTypes, typeOrder);
        }
      }
      if (association.hasEntity())
      {
        EntitySchema entitySchema = association.getEntity();
        findModelsEntity(entitySchema, foundTypes, typeOrder);
      }
    }
  }

  private void findModelsSimple(ResourceSchema resourceSchema, Map<String, NamedDataSchema> foundTypes, List<NamedDataSchema> typeOrder)
  {
    SimpleSchema simple = resourceSchema.getSimple();
    if (simple != null)
    {
      findErrorDetailTypeModels(simple, foundTypes, typeOrder);
      if (simple.hasMethods())
      {
        for (RestMethodSchema restMethodSchema : simple.getMethods())
        {
          findModels(restMethodSchema.getParameters(), restMethodSchema.getMetadata(), foundTypes, typeOrder);
          findErrorDetailTypeModels(restMethodSchema, foundTypes, typeOrder);
        }
      }
      if (simple.hasActions())
      {
        for (ActionSchema actionSchema : simple.getActions())
        {
          findModelsAction(actionSchema, foundTypes, typeOrder);
          findErrorDetailTypeModels(actionSchema, foundTypes, typeOrder);
        }
      }
      if (simple.hasEntity())
      {
        EntitySchema entity = simple.getEntity();
        findModelsEntity(entity, foundTypes, typeOrder);
      }
    }
  }

  private void findModelsActionSet(ResourceSchema resourceSchema, Map<String, NamedDataSchema> foundTypes, List<NamedDataSchema> typeOrder)
  {
    ActionsSetSchema actionsSet = resourceSchema.getActionsSet();
    if (actionsSet != null)
    {
      findErrorDetailTypeModels(actionsSet, foundTypes, typeOrder);
      if (actionsSet.hasActions())
      {
        for(ActionSchema actionSchema : actionsSet.getActions())
        {
          findModelsAction(actionSchema, foundTypes, typeOrder);
          findErrorDetailTypeModels(actionSchema, foundTypes, typeOrder);
        }
      }
    }
  }

  private void findModelsAction(ActionSchema actionSchema, Map<String, NamedDataSchema> foundTypes, List<NamedDataSchema> typeOrder)
  {
    if (actionSchema.hasParameters())
    {
      for(ParameterSchema parameterSchema : actionSchema.getParameters())
      {
        findModelsParameter(parameterSchema, foundTypes, typeOrder);
      }
    }

    String returns = actionSchema.getReturns();
    if (returns != null)
    {
      recordType(returns, foundTypes, typeOrder);
    }
  }

  private void findModels(ParameterSchemaArray parameters ,MetadataSchema metadata , Map<String, NamedDataSchema> foundTypes, List<NamedDataSchema> typeOrder)
  {
    if (parameters != null)
    {
      for(ParameterSchema parameterSchema : parameters)
      {
        findModelsParameter(parameterSchema, foundTypes, typeOrder);
      }
    }

    if (metadata != null)
    {
      String type = metadata.getType();
      recordType(type, foundTypes, typeOrder);
    }
  }

  private void findModelsParameter(ParameterSchema parameterSchema, Map<String, NamedDataSchema> foundTypes, List<NamedDataSchema> typeOrder)
  {
    String type = parameterSchema.getType();
    if (type.equals("array") || type.equals("map"))
    {
      String items = parameterSchema.getItems();
      recordType(items, foundTypes, typeOrder);
    }
    else
    {
      recordType(type, foundTypes, typeOrder);
    }
  }

  /**
   * For a given record that includes the {@link ServiceErrorsSchema} record, keep track of all referenced
   * error detail type models.
   *
   * @param schema record that includes the {@link ServiceErrorsSchema}
   * @param foundTypes running mapping of found data schemas
   * @param typeOrder running, ordered list of found data schemas
   */
  private void findErrorDetailTypeModels(RecordTemplate schema, Map<String, NamedDataSchema> foundTypes, List<NamedDataSchema> typeOrder)
  {
    // Wrap the underlying data map in the shared schema interface
    final ServiceErrorsSchema serviceErrorsSchema = new ServiceErrorsSchema(schema.data());

    // For each service error, inspect its error detail type field and keep track of all referenced types
    final ServiceErrorSchemaArray serviceErrorSchemaArray = serviceErrorsSchema.getServiceErrors();
    if (serviceErrorSchemaArray != null)
    {
      for (ServiceErrorSchema serviceErrorSchema : serviceErrorSchemaArray)
      {
        if (serviceErrorSchema.hasErrorDetailType())
        {
          recordType(serviceErrorSchema.getErrorDetailType(), foundTypes, typeOrder);
        }
      }
    }
  }

  private void recordType(String type, Map<String, NamedDataSchema> foundTypes, List<NamedDataSchema> typeOrder)
  {
    if (!foundTypes.containsKey(type))
    {
      DataSchema schema = RestSpecCodec.textToSchema(type, _schemaResolver);
      recordType(schema, foundTypes, typeOrder);
    }
  }

  private void recordType(DataSchema schema, Map<String, NamedDataSchema> foundTypes, List<NamedDataSchema> typeOrder)
  {
    if (schema instanceof NamedDataSchema)
    {
      NamedDataSchema namedDataSchema = (NamedDataSchema) schema;

      if (!foundTypes.containsKey(namedDataSchema.getFullName()))
      {
        foundTypes.put(namedDataSchema.getFullName(), namedDataSchema);

        if (schema instanceof RecordDataSchema) // recurse into record, record any contained types.
        {
          RecordDataSchema recordDataSchema = (RecordDataSchema)schema;
          for (NamedDataSchema includedSchema : recordDataSchema.getInclude())
          {
            recordType(includedSchema, foundTypes, typeOrder);
          }
          for(RecordDataSchema.Field field : recordDataSchema.getFields())
          {
            recordType(field.getType(), foundTypes, typeOrder);
          }
        }
        else if (schema instanceof TyperefDataSchema)
        {
          recordType(schema.getDereferencedDataSchema(), foundTypes, typeOrder);
        }

        typeOrder.add(namedDataSchema);
      }
    }
    else if (schema instanceof ArrayDataSchema)
    {
      ArrayDataSchema arraySchema = (ArrayDataSchema)schema;
      recordType(arraySchema.getItems(), foundTypes, typeOrder);
    }
    else if (schema instanceof MapDataSchema)
    {
      MapDataSchema mapSchema = (MapDataSchema)schema;
      recordType(mapSchema.getValues(), foundTypes, typeOrder);
    }
    else if (schema instanceof UnionDataSchema)
    {
      UnionDataSchema unionSchema = (UnionDataSchema)schema;
      for(UnionDataSchema.Member member : unionSchema.getMembers())
      {
        recordType(member.getType(), foundTypes, typeOrder);
      }
    }
  }
}
