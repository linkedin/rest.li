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
   * Enum to tell how the current schema is linked from its parentSchema as a child schema
   */
  enum CurrentSchemaEntryMode
  {
    // child schema is for a record's field
    FIELD,
    // child schema is the key field of map
    MAP_KEY,
    // child schema is the value field of map
    MAP_VALUE,
    // child schema is the item of array
    ARRAY_VALUE,
    // child schema is a member of union
    UNION_MEMBER,
    // child schema is referred from a typeref schema
    TYPEREF_REF
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

  private static class TraverserContextImpl implements TraverserContext
  {
    private Boolean _shouldContinue = null;
    private DataSchema _parentSchema;
    private DataSchema _currentSchema;
    private DataSchema _originalTopLevelSchema;
    private ArrayDeque<String> _traversePath = new ArrayDeque<>();
    private ArrayDeque<String> _schemaPathSpec = new ArrayDeque<>();
    private RecordDataSchema.Field _enclosingField;
    private UnionDataSchema.Member _enclosingUnionMember;
    private CurrentSchemaEntryMode _currentSchemaEntryMode;
    private SchemaVisitor.VisitorContext _visitorContext;

    @Override
    public SchemaVisitor.VisitorContext getVisitorContext()
    {
      return _visitorContext;
    }

    /**
     * Generate a new {@link TraverserContext} for next recursion in {@link DataSchemaRichContextTraverser#doRecursiveTraversal(TraverserContextImpl)}
     *
     * @param nextTraversePathComponent pathComponent of the traverse path of the next dataSchema to be traversed
     * @param nextSchemaPathSpecComponent pathComponent of the schema path of the next dataSchema to be traversed
     * @param nextSchema the next dataSchema to be traversed
     * @param nextSchemaEntryMode how next dataSchema is linked from current dataSchema.
     * @return a new {@link TraverserContext} generated for next recursion
     */
    private TraverserContextImpl getNextContext(String nextTraversePathComponent, String nextSchemaPathSpecComponent,
                                    DataSchema nextSchema, CurrentSchemaEntryMode nextSchemaEntryMode)
    {
      TraverserContextImpl nextContext = new TraverserContextImpl();
      nextContext.setOriginalTopLevelSchema(this.getTopLevelSchema());
      nextContext.setParentSchema(this.getCurrentSchema());
      nextContext.setSchemaPathSpec(new ArrayDeque<>(this.getSchemaPathSpec()));
      nextContext.setVisitorContext(this.getVisitorContext());
      nextContext.setEnclosingField(this.getEnclosingField());
      nextContext.setEnclosingUnionMember(this.getEnclosingUnionMember());

      // Need to make a copy so if it is modified in next recursion
      // it won't affect this recursion
      nextContext.setTraversePath(new ArrayDeque<>(this.getTraversePath()));
      nextContext.getTraversePath().add(nextTraversePathComponent);
      // Same as traversePath, we need to make a copy.
      nextContext.setSchemaPathSpec(new ArrayDeque<>(this.getSchemaPathSpec()));
      // SchemaPathSpecComponent could be null if nextSchema is a TypeRefDataSchema
      if (nextSchemaPathSpecComponent != null)
      {
        nextContext.getSchemaPathSpec().add(nextSchemaPathSpecComponent);
      }
      nextContext.setCurrentSchema(nextSchema);
      nextContext.setCurrentSchemaEntryMode(nextSchemaEntryMode);
      return nextContext;
    }

    private void setOriginalTopLevelSchema(DataSchema originalTopLevelSchema)
    {
      _originalTopLevelSchema = originalTopLevelSchema;
    }

    public Boolean shouldContinue()
    {
      return _shouldContinue;
    }

    @Override
    public void setShouldContinue(Boolean shouldContinue)
    {
      this._shouldContinue = shouldContinue;
    }

    public void setVisitorContext(SchemaVisitor.VisitorContext visitorContext)
    {
      _visitorContext = visitorContext;
    }

    @Override
    public DataSchema getTopLevelSchema()
    {
      return _originalTopLevelSchema;
    }

    @Override
    public ArrayDeque<String> getSchemaPathSpec()
    {
      return _schemaPathSpec;
    }

    private void setSchemaPathSpec(ArrayDeque<String> schemaPathSpec)
    {
      _schemaPathSpec = schemaPathSpec;
    }

    @Override
    public DataSchema getCurrentSchema()
    {
      return _currentSchema;
    }

    private void setCurrentSchema(DataSchema currentSchema)
    {
      _currentSchema = currentSchema;
    }

    @Override
    public ArrayDeque<String> getTraversePath()
    {
      return _traversePath;
    }

    private void setTraversePath(ArrayDeque<String> traversePath)
    {
      this._traversePath = traversePath;
    }

    @Override
    public DataSchema getParentSchema()
    {
      return _parentSchema;
    }

    private void setParentSchema(DataSchema parentSchema)
    {
      _parentSchema = parentSchema;
    }

    @Override
    public RecordDataSchema.Field getEnclosingField()
    {
      return _enclosingField;
    }

    private void setEnclosingField(RecordDataSchema.Field enclosingField)
    {
      _enclosingField = enclosingField;
    }

    @Override
    public UnionDataSchema.Member getEnclosingUnionMember()
    {
      return _enclosingUnionMember;
    }

    private void setEnclosingUnionMember(UnionDataSchema.Member enclosingUnionMember)
    {
      _enclosingUnionMember = enclosingUnionMember;
    }

    @Override
    public CurrentSchemaEntryMode getCurrentSchemaEntryMode()
    {
      return _currentSchemaEntryMode;
    }

    private void setCurrentSchemaEntryMode(CurrentSchemaEntryMode currentSchemaEntryMode)
    {
      _currentSchemaEntryMode = currentSchemaEntryMode;
    }
  }
}
