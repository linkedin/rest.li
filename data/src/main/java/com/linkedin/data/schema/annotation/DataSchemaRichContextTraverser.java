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
package com.linkedin.data.schema.annotation;

import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaConstants;
import com.linkedin.data.schema.DataSchemaTraverse;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.schema.PrimitiveDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.TyperefDataSchema;
import com.linkedin.data.schema.UnionDataSchema;
import java.util.ArrayDeque;
import java.util.IdentityHashMap;


/**
 * Expanded from {@link com.linkedin.data.schema.DataSchemaTraverse}
 * There are two main differences:
 * (1) This new traverser provides rich context that passed to the visitors when visiting data schemas
 * (2) It will also traverse to the schemas even the schema has been seen before. (But there are mechanisms to prevent cycles)
 */
public class DataSchemaRichContextTraverser
{
  /**
   * Use this {@link IdentityHashMap} to prevent traversing through a cycle, when traversing along parents to child.
   * for the example below, Rcd's f1 field also points to a Rcd which formed a cycle and we should stop visiting it.
   * <pre>
   * record Rcd{
   *   f1: Rcd
   * }
   * </pre>
   *
   * But note that this HashMap will not prevent traversing a seen data schema that is not from its ancestor.
   * e.g.
   * <pre>
   *   record Rcd {
   *     f1: Rcd2
   *     f2: Rcd2
   *   }
   * </pre>
   * In this case, both f1 and f2 will be visited.
   *
   * This HashMap help the traverser to recognize when encountering "Rcd" for the second time if it forms a cycle
   */
  private final IdentityHashMap<DataSchema, Boolean> _seenAncestorsDataSchema = new IdentityHashMap<>();
  private SchemaVisitor _schemaVisitor;
  /**
   * Store the original data schema that has been passed.
   * The {@link DataSchemaRichContextTraverser} should not modify this DataSchema during the traversal
   * which could ensure the correctness of the traversal
   *
   */
  private DataSchema _originalTopLevelSchemaUnderTraversal;

  public DataSchemaRichContextTraverser(SchemaVisitor schemaVisitor)
  {
    _schemaVisitor = schemaVisitor;
  }

  public void traverse(DataSchema schema)
  {
    _originalTopLevelSchemaUnderTraversal = schema;
    TraverserContextImpl traverserContext = new TraverserContextImpl();
    traverserContext.setOriginalTopLevelSchema(_originalTopLevelSchemaUnderTraversal);
    traverserContext.setCurrentSchema(schema);
    traverserContext.setVisitorContext(_schemaVisitor.getInitialVisitorContext());
    doRecursiveTraversal(traverserContext);
  }

  private void doRecursiveTraversal(TraverserContextImpl context)
  {

    // Add full name to the context's TraversePath
    DataSchema schema = context.getCurrentSchema();
    ArrayDeque<String> path = context.getTraversePath();
    path.add(schema.getUnionMemberKey());

    // visitors
    _schemaVisitor.callbackOnContext(context, DataSchemaTraverse.Order.PRE_ORDER);

    /**
     * By default {@link DataSchemaRichContextTraverser} will only decide whether or not keep traversing based on whether the new
     * data schema has been seen.
     *
     * But the {@link SchemaVisitor} has the chance to override this control by setting {@link TraverserContext#_shouldContinue}
     * If this variable set to be {@link Boolean#TRUE}, the {@link DataSchemaRichContextTraverser} will traverse to next level (if applicable)
     * If this variable set to be {@link Boolean#FALSE}, the {@link DataSchemaRichContextTraverser} will stop traversing to next level
     * If this variable not set, the {@link DataSchemaRichContextTraverser} will decide whether or not to continue traversing based on whether
     * this data schema has been seen.
     */
    if (context.shouldContinue() == Boolean.TRUE ||
        !(context.shouldContinue() == Boolean.FALSE || _seenAncestorsDataSchema.containsKey(schema)))
    {
      _seenAncestorsDataSchema.put(schema, Boolean.TRUE);

      // Pass new context in every recursion
      TraverserContextImpl nextContext = null;

      switch (schema.getType())
      {
        case TYPEREF:
          TyperefDataSchema typerefDataSchema = (TyperefDataSchema) schema;

          nextContext = context.getNextContext(DataSchemaConstants.REF_KEY, null, typerefDataSchema.getRef(),
                                               CurrentSchemaEntryMode.TYPEREF_REF);
          doRecursiveTraversal(nextContext);
          break;
        case MAP:
          // traverse key
          MapDataSchema mapDataSchema = (MapDataSchema) schema;

          nextContext = context.getNextContext(DataSchemaConstants.MAP_KEY_REF, DataSchemaConstants.MAP_KEY_REF,
                                               mapDataSchema.getKey(), CurrentSchemaEntryMode.MAP_KEY);
          doRecursiveTraversal(nextContext);

          // then traverse values
          nextContext = context.getNextContext(PathSpec.WILDCARD, PathSpec.WILDCARD, mapDataSchema.getValues(),
                                               CurrentSchemaEntryMode.MAP_VALUE);
          doRecursiveTraversal(nextContext);
          break;
        case ARRAY:
          ArrayDataSchema arrayDataSchema = (ArrayDataSchema) schema;

          nextContext = context.getNextContext(PathSpec.WILDCARD, PathSpec.WILDCARD, arrayDataSchema.getItems(),
                                               CurrentSchemaEntryMode.ARRAY_VALUE);
          doRecursiveTraversal(nextContext);
          break;
        case RECORD:
          RecordDataSchema recordDataSchema = (RecordDataSchema) schema;
          for (RecordDataSchema.Field field : recordDataSchema.getFields())
          {
            nextContext =
                context.getNextContext(field.getName(), field.getName(), field.getType(), CurrentSchemaEntryMode.FIELD);
            nextContext.setEnclosingField(field);
            doRecursiveTraversal(nextContext);
          }
          break;
        case UNION:
          UnionDataSchema unionDataSchema = (UnionDataSchema) schema;
          for (UnionDataSchema.Member member : unionDataSchema.getMembers())
          {
            nextContext =
                context.getNextContext(member.getUnionMemberKey(), member.getUnionMemberKey(), member.getType(),
                                       CurrentSchemaEntryMode.UNION_MEMBER);
            nextContext.setEnclosingUnionMember(member);
            doRecursiveTraversal(nextContext);
          }
          break;
        default:
          // will stop recursively traversing if the current schema is a leaf node.
          assert isLeafSchema(schema);
          break;
      }
      _seenAncestorsDataSchema.remove(schema);
    }
    _schemaVisitor.callbackOnContext(context, DataSchemaTraverse.Order.POST_ORDER);
  }

  /**
   * Returns true if the dataSchema is a leaf node.
   *
   * a leaf DataSchema is a schema that doesn't have other types of DataSchema linked from it.
   * Below types are leaf DataSchemas
   * {@link com.linkedin.data.schema.PrimitiveDataSchema} ,
   * {@link com.linkedin.data.schema.EnumDataSchema} ,
   * {@link com.linkedin.data.schema.FixedDataSchema}
   *
   * Other dataSchema types, for example {@link com.linkedin.data.schema.TyperefDataSchema} could link to another DataSchema
   * so it is not a leaf DataSchema
   */
  public static boolean isLeafSchema(DataSchema dataSchema)
  {
    return (dataSchema instanceof PrimitiveDataSchema)
           || (dataSchema.getType() == DataSchema.Type.FIXED)
           || (dataSchema.getType() == DataSchema.Type.ENUM);
  }
}
