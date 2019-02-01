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

package com.linkedin.restli.docgen;


import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.DataSchemaTraverse;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.PegasusSchemaParser;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.StringArray;
import com.linkedin.restli.restspec.ActionSchema;
import com.linkedin.restli.restspec.AssocKeySchema;
import com.linkedin.restli.restspec.AssociationSchema;
import com.linkedin.restli.restspec.BatchFinderSchema;
import com.linkedin.restli.restspec.CollectionSchema;
import com.linkedin.restli.restspec.FinderSchema;
import com.linkedin.restli.restspec.IdentifierSchema;
import com.linkedin.restli.restspec.MetadataSchema;
import com.linkedin.restli.restspec.ParameterSchema;
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.restli.server.ResourceLevel;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Compute the adjacency relationship in a collection of {@link ResourceSchema}.
 *
 * @author Keren Jin
 */
public class RestLiResourceRelationship
{
  /**
   * @param resourceSchemas collection of {@link ResourceSchema} to be processed
   * @param schemaResolver resolver that resolves the resource schemas in {@code resourceSchemas}
   */
  public RestLiResourceRelationship(ResourceSchemaCollection resourceSchemas,
                                    DataSchemaResolver schemaResolver)
  {
    _resourceSchemas = resourceSchemas;
    _schemaResolver = schemaResolver;
    _schemaParser = null;

    findDataModels();
  }

  /**
   * @param resourceSchemas collection of {@link ResourceSchema} to be processed
   * @param schemaParser parser that parses text into {@link DataSchema}
   */
  public RestLiResourceRelationship(ResourceSchemaCollection resourceSchemas,
                                    PegasusSchemaParser schemaParser)
  {
    _resourceSchemas = resourceSchemas;
    _schemaResolver = null;
    _schemaParser = schemaParser;

    findDataModels();
  }

  /**
   * @return all {@link ResourceSchema} in the relationship graph
   */
  public ResourceSchemaCollection getResourceSchemaCollection()
  {
    return _resourceSchemas;
  }

  /**
   * @return all parsed schema names
   */
  public Set<String> getSchemaNames()
  {
    return Collections.unmodifiableSet(_schemaParser.getResolver().bindings().keySet());
  }

  /**
   * @return map of {@link NamedDataSchema} in the relationship graph accessible from their names
   */
  public SortedMap<String, NamedDataSchema> getDataModels()
  {
    return _dataModels;
  }

  /**
   * @param o object to be queried
   * @param <T> type of the object
   * @return {@link Node} of the specified object that expresses its adjacency in the relationship graph
   */
  public <T> Node<T> getRelationships(T o)
  {
    return _relationships.get(o);
  }

  private NamedDataSchema extractSchema(String className)
  {
    if (_schemaParser == null)
    {
      // 'online mode': resolve data schema from RecordTemplate Class SCHEMA field
      final StringBuilder errorMessage = new StringBuilder();
      final NamedDataSchema schema = _schemaResolver.findDataSchema(className, errorMessage);
      if (errorMessage.length() > 0)
      {
        return null;
      }

      return schema;
    }
    else
    {
      // 'offline mode': resolve data schema from input
      final DataSchema schema = _schemaParser.lookupName(className);
      // we're currently only interested in records
      return schema instanceof RecordDataSchema ? (RecordDataSchema)schema : null;
    }
  }

  private void findDataModels()
  {
    final ResourceSchemaVisitior visitor = new BaseResourceSchemaVisitor()
    {
      @Override
      public void visitResourceSchema(VisitContext visitContext,
                                      ResourceSchema resourceSchema)
      {
        final String schema = resourceSchema.getSchema();
        // ActionSet resources do not have a schema
        if (schema != null)
        {
          final NamedDataSchema schemaSchema = extractSchema(schema);
          if (schemaSchema != null)
          {
            connectSchemaToResource(visitContext, schemaSchema);
          }
        }
      }

      @Override
      public void visitCollectionResource(VisitContext visitContext,
                                          CollectionSchema collectionSchema)
      {
        final IdentifierSchema id = collectionSchema.getIdentifier();

        final NamedDataSchema typeSchema = extractSchema(id.getType());
        if (typeSchema != null)
        {
          connectSchemaToResource(visitContext, typeSchema);
        }

        final String params = id.getParams();
        if (params != null)
        {
          final NamedDataSchema paramsSchema = extractSchema(params);
          if (paramsSchema != null)
          {
            connectSchemaToResource(visitContext, paramsSchema);
          }
        }
      }

      @Override
      public void visitAssociationResource(VisitContext visitContext,
                                           AssociationSchema associationSchema)
      {
        for (AssocKeySchema key : associationSchema.getAssocKeys())
        {
          final NamedDataSchema keyTypeSchema = extractSchema(key.getType());
          if (keyTypeSchema != null)
          {
            connectSchemaToResource(visitContext, keyTypeSchema);
          }
        }
      }

      @Override
      public void visitParameter(VisitContext visitContext,
                                 RecordTemplate parentResource,
                                 Object parentMethodSchema,
                                 ParameterSchema parameterSchema)
      {
        String parameterTypeString = parameterSchema.getType();
        if (isInlineSchema(parameterTypeString)) // the parameter type field contains a inline schema, so we traverse into it
        {
          visitInlineSchema(visitContext, parameterTypeString);
        }
        else
        {
          final NamedDataSchema schema;

          // else if the parameter is using the legacy format or representing maps and lists with a separate "items" field,
          // grab the schema name from it
          if (parameterSchema.hasItems())
          {
            schema = extractSchema(parameterSchema.getItems());
          }
          else // the only remaining possibility is that the type field contains the name of a data schema
          {
            schema = extractSchema(parameterTypeString);
          }

          if (schema != null)
          {
            connectSchemaToResource(visitContext, schema);
          }
        }
      }

      @Override
      public void visitFinder(VisitContext visitContext,
                              RecordTemplate parentResource,
                              FinderSchema finderSchema)
      {
        final MetadataSchema metadata = finderSchema.getMetadata();
        if (metadata != null)
        {
          final NamedDataSchema metadataTypeSchema = extractSchema(metadata.getType());
          if (metadataTypeSchema != null)
          {
            connectSchemaToResource(visitContext, metadataTypeSchema);
          }
        }
      }

      @Override
      public void visitBatchFinder(VisitContext visitContext,
                                  RecordTemplate parentResource,
                                  BatchFinderSchema batchFinderSchema)
      {
        final MetadataSchema metadata = batchFinderSchema.getMetadata();
        if (metadata != null)
        {
          final NamedDataSchema metadataTypeSchema = extractSchema(metadata.getType());
          if (metadataTypeSchema != null)
          {
            connectSchemaToResource(visitContext, metadataTypeSchema);
          }
        }
      }

      @Override
      public void visitAction(VisitContext visitContext,
                              RecordTemplate parentResource,
                              ResourceLevel resourceLevel,
                              ActionSchema actionSchema)
      {
        final String returns = actionSchema.getReturns();
        if (returns != null)
        {
          if (isInlineSchema(returns)) // the parameter type field contains a inline schema, so we traverse into it
          {
            visitInlineSchema(visitContext, returns);
          }
          else // otherwise the type field contains the name of a data schema
          {
            final NamedDataSchema returnsSchema = extractSchema(returns);
            if (returnsSchema != null)
            {
              connectSchemaToResource(visitContext, returnsSchema);
            }
          }
        }

        final StringArray throwsArray = actionSchema.getThrows();
        if (throwsArray != null)
        {
          for (String errorName: throwsArray)
          {
            final NamedDataSchema errorSchema = extractSchema(errorName);
            if (errorSchema != null)
            {
              connectSchemaToResource(visitContext, errorSchema);
            }
          }
        }
      }

      private boolean isInlineSchema(String schemaString)
      {
        return schemaString.startsWith("{");
      }

      private void visitInlineSchema(VisitContext visitContext, String schemaString)
      {
        DataSchema schema = DataTemplateUtil.parseSchema(schemaString, _schemaResolver);
        if (schema instanceof ArrayDataSchema)
        {
          DataSchema itemSchema = ((ArrayDataSchema)schema).getItems();
          if (itemSchema instanceof NamedDataSchema)
          {
            connectSchemaToResource(visitContext, (NamedDataSchema)itemSchema);
          }
        }
        if (schema instanceof MapDataSchema)
        {
          DataSchema valueSchema = ((MapDataSchema)schema).getValues();
          if (valueSchema instanceof NamedDataSchema)
          {
            connectSchemaToResource(visitContext, (NamedDataSchema)valueSchema);
          }
        }
      }

      private void connectSchemaToResource(VisitContext visitContext, final NamedDataSchema schema)
      {
        final Node<NamedDataSchema> schemaNode = _relationships.get(schema);
        _dataModels.put(schema.getFullName(), schema);

        final DataSchemaTraverse traveler = new DataSchemaTraverse();
        traveler.traverse(schema, new DataSchemaTraverse.Callback()
        {
          @Override
          public void callback(List<String> path, DataSchema nestedSchema)
          {
            if (nestedSchema instanceof RecordDataSchema && nestedSchema != schema)
            {
              final RecordDataSchema nestedRecordSchema = (RecordDataSchema) nestedSchema;
              _dataModels.put(nestedRecordSchema.getFullName(), nestedRecordSchema);
              final Node<RecordDataSchema> node = _relationships.get(nestedRecordSchema);
              schemaNode.addAdjacentNode(node);
            }
          }
        });

        final Node<ResourceSchema> resourceNode = _relationships.get(visitContext.getParentSchema());
        resourceNode.addAdjacentNode(schemaNode);
        schemaNode.addAdjacentNode(resourceNode);
      }
    };

    ResourceSchemaCollection.visitResources(_resourceSchemas.getResources().values(), visitor);
  }

  private final ResourceSchemaCollection _resourceSchemas;
  private final DataSchemaResolver _schemaResolver;
  private final PegasusSchemaParser _schemaParser;
  private final SortedMap<String, NamedDataSchema> _dataModels = new TreeMap<String, NamedDataSchema>();
  private final Graph _relationships = new Graph();
}
