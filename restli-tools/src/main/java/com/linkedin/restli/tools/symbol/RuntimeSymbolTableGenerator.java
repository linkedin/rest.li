/*
   Copyright (c) 2019 LinkedIn Corp.

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

package com.linkedin.restli.tools.symbol;

import com.linkedin.data.codec.symbol.InMemorySymbolTable;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.EnumDataSchema;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.TyperefDataSchema;
import com.linkedin.data.schema.UnionDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.TemplateRuntimeException;
import com.linkedin.data.transform.patch.PatchConstants;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.common.ActionResponse;
import com.linkedin.restli.common.BatchFinderCriteriaResult;
import com.linkedin.restli.common.BatchRequest;
import com.linkedin.restli.common.CollectionMetadata;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.CreateStatus;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.ErrorDetails;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.KeyValueRecord;
import com.linkedin.restli.common.Link;
import com.linkedin.restli.common.LinkArray;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.PegasusSchema;
import com.linkedin.restli.common.UpdateEntityStatus;
import com.linkedin.restli.common.UpdateStatus;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Generates symbol tables at runtime.
 */
class RuntimeSymbolTableGenerator {

  private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeSymbolTableGenerator.class);

  /**
   * Generate and return the current container's symbol table.
   *
   * @param symbolTableNameHandler The symbol table name handler to generate symbol table names.
   * @param resourceSchemas        The set of {@link DataSchema} referenced by resources.
   */
  static InMemorySymbolTable generate(SymbolTableNameHandler symbolTableNameHandler, Set<DataSchema> resourceSchemas)
  {
    Set<String> symbols = new HashSet<>();
    addFrameworkSymbols(symbols);
    Set<DataSchema> frameworkSchemas = new HashSet<>();
    collectFrameworkSchemas(frameworkSchemas);

    Set<DataSchema> processedSchemas = new HashSet<>();
    frameworkSchemas.forEach(schema -> expandAndCollectSymbols(schema, processedSchemas, symbols));
    resourceSchemas.forEach(schema -> expandAndCollectSymbols(schema, processedSchemas, symbols));

    // Sort symbols to ensure stable ordering across invocations for the same input.
    List<String> symbolList = new ArrayList<>(symbols);
    Collections.sort(symbolList);
    String symbolTableName = symbolTableNameHandler.generateName(symbolList);
    return new InMemorySymbolTable(symbolTableName, symbolList);
  }

  private static void addFrameworkSymbols(Set<String> symbols)
  {
    // Action Response
    symbols.add(ActionResponse.VALUE_NAME);

    // Batch Request
    symbols.add(BatchRequest.ENTITIES);

    // BatchFinderCriteriaResult
    symbols.add(BatchFinderCriteriaResult.ERROR);
    symbols.add(BatchFinderCriteriaResult.ISERROR);

    // BatchKV Response
    symbols.add(BatchKVResponse.RESULTS);
    symbols.add(BatchKVResponse.ERRORS);

    // Collection Response
    symbols.add(CollectionResponse.ELEMENTS);
    symbols.add(CollectionResponse.METADATA);
    symbols.add(CollectionResponse.PAGING);

    // KeyValueRecord
    symbols.add(KeyValueRecord.KEY_FIELD_NAME);
    symbols.add(KeyValueRecord.VALUE_FIELD_NAME);
    symbols.add(KeyValueRecord.PARAMS_FIELD_NAME);

    // Patch
    symbols.add(PatchRequest.PATCH);
    symbols.add(PatchConstants.SET_COMMAND);
    symbols.add(PatchConstants.DELETE_COMMAND);
    symbols.add(PatchConstants.REORDER_COMMAND);
    symbols.add(PatchConstants.FROM_INDEX);
    symbols.add(PatchConstants.TO_INDEX);

    // UpdateEntityStatus
    symbols.add(UpdateEntityStatus.ENTITY);
  }

  private static void collectFrameworkSchemas(Set<DataSchema> resourceSchemas)
  {
    Class<?>[] frameworkClasses = {
        ErrorResponse.class,
        ErrorDetails.class,
        CollectionMetadata.class,
        CreateStatus.class,
        EmptyRecord.class,
        PegasusSchema.class,
        Link.class,
        LinkArray.class,
        UpdateStatus.class
    };

    for (Class<?> klass : frameworkClasses)
    {
      try
      {
        resourceSchemas.add(DataTemplateUtil.getSchema(klass));
      }
      catch (TemplateRuntimeException e)
      {
        LOGGER.debug("Failed to get schema from class: " + klass);
      }
    }
  }

  private static void expandAndCollectSymbols(DataSchema resourceSchema,
      Set<DataSchema> processedSchemas,
      Set<String> symbols)
  {
    if (resourceSchema instanceof TyperefDataSchema)
    {
      TyperefDataSchema typerefDataSchema = (TyperefDataSchema) resourceSchema;
      expandAndCollectSymbols(typerefDataSchema.getDereferencedDataSchema(), processedSchemas, symbols);
      return;
    }
    else if (resourceSchema instanceof ArrayDataSchema)
    {
      ArrayDataSchema arrayDataSchema = (ArrayDataSchema) resourceSchema;
      expandAndCollectSymbols(arrayDataSchema.getItems(), processedSchemas, symbols);
      return;
    }
    else if (resourceSchema instanceof MapDataSchema)
    {
      MapDataSchema mapDataSchema = (MapDataSchema) resourceSchema;
      expandAndCollectSymbols(mapDataSchema.getValues(), processedSchemas, symbols);
      return;
    }

    if (processedSchemas.contains(resourceSchema))
    {
      return;
    }

    processedSchemas.add(resourceSchema);

    if (resourceSchema instanceof RecordDataSchema)
    {
      RecordDataSchema recordDataSchema = (RecordDataSchema) resourceSchema;
      for (RecordDataSchema.Field field : recordDataSchema.getFields())
      {
        symbols.add(field.getName());
        expandAndCollectSymbols(field.getType(), processedSchemas, symbols);
      }
    }
    else if (resourceSchema instanceof UnionDataSchema)
    {
      UnionDataSchema unionDataSchema = (UnionDataSchema) resourceSchema;
      for (UnionDataSchema.Member member : unionDataSchema.getMembers())
      {
        symbols.add(member.getUnionMemberKey());
        expandAndCollectSymbols(member.getType(), processedSchemas, symbols);
      }
    }
    else if (resourceSchema instanceof EnumDataSchema)
    {
      EnumDataSchema enumDataSchema = (EnumDataSchema) resourceSchema;
      symbols.addAll(enumDataSchema.getSymbols());
    }
  }
}

