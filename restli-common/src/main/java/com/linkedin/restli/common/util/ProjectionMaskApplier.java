/*
   Copyright (c) 2018 LinkedIn Corp.

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

import com.linkedin.data.DataMap;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.Name;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.TyperefDataSchema;
import com.linkedin.data.schema.UnionDataSchema;
import com.linkedin.data.transform.Escaper;
import com.linkedin.data.transform.filter.FilterConstants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Helper class that applies a projection mask to a {@link DataSchema} by building a new schema
 * and only including those fields present in the provided projection.
 *
 * @author Soojung Ha
 * @author Evan Williams
 */
public class ProjectionMaskApplier
{
  private static final Set<String> ARRAY_RANGE_PARAMS =
      new HashSet<>(Arrays.asList(FilterConstants.START, FilterConstants.COUNT));

  /**
   * Build a new schema that contains only the projected fields from the original schema recursively.
   * @param schema schema to build from
   * @param maskMap projection mask data map
   * @return new schema containing only projected fields
   */
  public static DataSchema buildSchemaByProjection(DataSchema schema, DataMap maskMap)
  {
    return buildSchemaByProjection(schema, maskMap, Collections.emptyList());
  }

  /**
   * Build a new schema that contains only the projected fields from the original schema recursively.
   * @param schema schema to build from
   * @param maskMap projection mask data map
   * @param nonSchemaFieldsToAllowInProjectionMask Field names to allow in the projection mask even if the field is not
   *                                               present in the Schema. These fields will be ignored and the new
   *                                               schema will not have a corresponding field.
   * @return new schema containing only projected fields
   * @throws InvalidProjectionException if a field specified in the projection mask is not present in the source schema.
   */
  public static DataSchema buildSchemaByProjection(DataSchema schema, DataMap maskMap,
      Collection<String> nonSchemaFieldsToAllowInProjectionMask)
  {
    if (maskMap == null || maskMap.isEmpty())
    {
      throw new IllegalArgumentException("Invalid projection masks.");
    }

    if (schema instanceof RecordDataSchema)
    {
      return buildRecordDataSchemaByProjection((RecordDataSchema) schema, maskMap, nonSchemaFieldsToAllowInProjectionMask);
    }
    else if (schema instanceof UnionDataSchema)
    {
      return buildUnionDataSchemaByProjection((UnionDataSchema) schema, maskMap, nonSchemaFieldsToAllowInProjectionMask);
    }
    else if (schema instanceof ArrayDataSchema)
    {
      return buildArrayDataSchemaByProjection((ArrayDataSchema) schema, maskMap, nonSchemaFieldsToAllowInProjectionMask);
    }
    else if (schema instanceof MapDataSchema)
    {
      return buildMapDataSchemaByProjection((MapDataSchema) schema, maskMap, nonSchemaFieldsToAllowInProjectionMask);
    }
    else if (schema instanceof TyperefDataSchema)
    {
      return buildTyperefDataSchemaByProjection((TyperefDataSchema) schema, maskMap);
    }

    throw new IllegalArgumentException("Unexpected data schema type: " + schema);
  }

  /**
   * Build a new {@link TyperefDataSchema} schema that contains only the masked fields.
   */
  private static TyperefDataSchema buildTyperefDataSchemaByProjection(TyperefDataSchema originalSchema, DataMap maskMap)
  {
    TyperefDataSchema newSchema = new TyperefDataSchema(new Name(originalSchema.getFullName()));
    if (originalSchema.getProperties() != null)
    {
      newSchema.setProperties(originalSchema.getProperties());
    }
    if (originalSchema.getDoc() != null)
    {
      newSchema.setDoc(originalSchema.getDoc());
    }
    if (originalSchema.getAliases() != null)
    {
      newSchema.setAliases(originalSchema.getAliases());
    }
    DataSchema newRefSchema = buildSchemaByProjection(originalSchema.getRef(), maskMap);
    newSchema.setReferencedType(newRefSchema);
    return newSchema;
  }

  /**
   * Build a new {@link MapDataSchema} schema that contains only the masked fields.
   */
  private static MapDataSchema buildMapDataSchemaByProjection(MapDataSchema originalSchema, DataMap maskMap,
      Collection<String> nonSchemaFieldsToAllowInProjectionMask)
  {
    if (maskMap.containsKey(FilterConstants.WILDCARD))
    {
      DataSchema newValuesSchema = reuseOrBuildDataSchema(
          originalSchema.getValues(), maskMap.get(FilterConstants.WILDCARD), nonSchemaFieldsToAllowInProjectionMask);
      MapDataSchema newSchema = new MapDataSchema(newValuesSchema);
      if (originalSchema.getProperties() != null)
      {
        newSchema.setProperties(originalSchema.getProperties());
      }
      return newSchema;
    }

    throw new IllegalArgumentException("Missing wildcard key in projection mask: " + maskMap.keySet());
  }

  /**
   * Build a new {@link ArrayDataSchema} schema that contains only the masked fields.
   */
  private static ArrayDataSchema buildArrayDataSchemaByProjection(ArrayDataSchema originalSchema, DataMap maskMap,
      Collection<String> nonSchemaFieldsToAllowInProjectionMask)
  {
    if (maskMap.containsKey(FilterConstants.WILDCARD))
    {
      DataSchema newItemsSchema = reuseOrBuildDataSchema(
          originalSchema.getItems(), maskMap.get(FilterConstants.WILDCARD), nonSchemaFieldsToAllowInProjectionMask);
      ArrayDataSchema newSchema = new ArrayDataSchema(newItemsSchema);
      if (originalSchema.getProperties() != null)
      {
        newSchema.setProperties(originalSchema.getProperties());
      }
      return newSchema;
    }
    else if (ARRAY_RANGE_PARAMS.containsAll(maskMap.keySet()))
    {
      // If the mask contains array range parameters without a WILDCARD, return the original schema
      return originalSchema;
    }

    throw new IllegalArgumentException("Missing wildcard key in projection mask: " + maskMap.keySet());
  }

  /**
   * Build a new {@link UnionDataSchema} schema that contains only the masked fields.
   */
  private static UnionDataSchema buildUnionDataSchemaByProjection(UnionDataSchema unionDataSchema, DataMap maskMap,
      Collection<String> nonSchemaFieldsToAllowInProjectionMask)
  {
    List<UnionDataSchema.Member> newUnionMembers = new ArrayList<>();

    StringBuilder errorMessageBuilder = new StringBuilder();
    // Get the wildcard mask if one is available
    Object wildcardMask = maskMap.get(FilterConstants.WILDCARD);

    for (UnionDataSchema.Member member: unionDataSchema.getMembers())
    {
      Object maskValue = maskMap.get(Escaper.escape(member.getUnionMemberKey()));

      // If a mask is available for this specific member use that, else use the wildcard mask if that is available
      UnionDataSchema.Member newMember = null;
      if (maskValue != null)
      {
        newMember = new UnionDataSchema.Member(
            reuseOrBuildDataSchema(member.getType(), maskValue, nonSchemaFieldsToAllowInProjectionMask));
      }
      else if (wildcardMask != null)
      {
        newMember = new UnionDataSchema.Member(
            reuseOrBuildDataSchema(member.getType(), wildcardMask, nonSchemaFieldsToAllowInProjectionMask));
      }

      if (newMember != null)
      {
        if (member.hasAlias())
        {
          newMember.setAlias(member.getAlias(), errorMessageBuilder);
        }
        newMember.setDeclaredInline(member.isDeclaredInline());
        newMember.setDoc(member.getDoc());
        newMember.setProperties(member.getProperties());
        newUnionMembers.add(newMember);
      }
    }

    UnionDataSchema newUnionDataSchema = new UnionDataSchema();
    newUnionDataSchema.setMembers(newUnionMembers, errorMessageBuilder);
    if (newUnionMembers.size() > 0)
    {
      newUnionDataSchema.setAllowEmptyUnionResponse(true);
    }

    if (unionDataSchema.getProperties() != null)
    {
      newUnionDataSchema.setProperties(unionDataSchema.getProperties());
    }
    return newUnionDataSchema;
  }

  /**
   * Build a new {@link RecordDataSchema} schema that contains only the masked fields.
   */
  private static RecordDataSchema buildRecordDataSchemaByProjection(RecordDataSchema originalSchema, DataMap maskMap,
      Collection<String> nonSchemaFieldsToAllowInProjectionMask)
  {
    RecordDataSchema newRecordSchema = new RecordDataSchema(new Name(originalSchema.getFullName()), RecordDataSchema.RecordType.RECORD);
    List<RecordDataSchema.Field> newFields = new ArrayList<RecordDataSchema.Field>();
    for (Map.Entry<String, Object> maskEntry : maskMap.entrySet())
    {
      String maskFieldName = Escaper.unescapePathSegment(maskEntry.getKey());

      if (originalSchema.contains(maskFieldName))
      {
        RecordDataSchema.Field originalField = originalSchema.getField(maskFieldName);

        DataSchema fieldSchemaToUse = reuseOrBuildDataSchema(originalField.getType(), maskEntry.getValue(),
            nonSchemaFieldsToAllowInProjectionMask);
        RecordDataSchema.Field newField = buildRecordField(originalField, fieldSchemaToUse, newRecordSchema);
        newFields.add(newField);
      }
      else if (!nonSchemaFieldsToAllowInProjectionMask.contains(maskFieldName))
      {
        throw new InvalidProjectionException(
            "Projected field \"" + maskFieldName + "\" not present in schema \"" + originalSchema.getFullName() + "\"");
      }
    }

    // Fields from 'include' are no difference from other fields from original schema,
    // therefore, we are not calling newRecordSchema.setInclude() here.
    newRecordSchema.setFields(newFields, new StringBuilder()); // No errors are expected here, as the new schema is merely subset of the original
    if (originalSchema.getAliases() != null)
    {
      newRecordSchema.setAliases(originalSchema.getAliases());
    }
    if (originalSchema.getDoc() != null)
    {
      newRecordSchema.setDoc(originalSchema.getDoc());
    }
    if (originalSchema.getProperties() != null)
    {
      newRecordSchema.setProperties(originalSchema.getProperties());
    }
    return newRecordSchema;
  }

  /**
   * The maskValue from a rest.li projection mask is expected to be either:
   * 1) Integer that has value 1, which means all fields in the original schema are projected (negative projection not supported)
   * 2) DataMap, which means only selected fields in the original schema are projected
   */
  private static DataSchema reuseOrBuildDataSchema(DataSchema originalSchema, Object maskValue,
      Collection<String> nonSchemaFieldsToAllowInProjectionMask)
  {
    if (maskValue instanceof Integer && maskValue.equals(FilterConstants.POSITIVE))
    {
      return originalSchema;
    }
    else if (maskValue instanceof DataMap)
    {
      return buildSchemaByProjection(originalSchema, (DataMap) maskValue, nonSchemaFieldsToAllowInProjectionMask);
    }
    throw new IllegalArgumentException("Expected mask value to be either positive mask op or DataMap: " + maskValue);
  }

  /**
   * Build a new record field with a new projected field schema.
   * All other properties are copied over from the originalField.
   */
  private static RecordDataSchema.Field buildRecordField(RecordDataSchema.Field originalField,
      DataSchema fieldSchemaToReplace,
      RecordDataSchema recordSchemaToReplace)
  {
    RecordDataSchema.Field newField = new RecordDataSchema.Field(fieldSchemaToReplace);
    if (originalField.getAliases() != null)
    {
      // No errors are expected here, as the new schema is merely subset of the original
      newField.setAliases(originalField.getAliases(), new StringBuilder());
    }
    if (originalField.getDefault() != null)
    {
      newField.setDefault(originalField.getDefault());
    }
    if (originalField.getDoc() != null)
    {
      newField.setDoc(originalField.getDoc());
    }
    if (originalField.getName() != null)
    {
      // No errors are expected here, as the new schema is merely subset of the original
      newField.setName(originalField.getName(), new StringBuilder());
    }
    if (originalField.getOrder() != null)
    {
      newField.setOrder(originalField.getOrder());
    }
    if (originalField.getProperties() != null)
    {
      newField.setProperties(originalField.getProperties());
    }
    newField.setRecord(recordSchemaToReplace);
    newField.setOptional(originalField.getOptional());
    return newField;
  }

  /**
   * Used for halting the process of building a schema by projection when the projection is invalid,
   * allowing the calling class to catch the exception and handle it appropriately.
   */
  @SuppressWarnings("serial")
  public static class InvalidProjectionException extends RuntimeException
  {
    private InvalidProjectionException(String message)
    {
      super(message);
    }
  }
}
