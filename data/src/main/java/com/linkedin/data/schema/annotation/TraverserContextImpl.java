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

import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.UnionDataSchema;
import java.util.ArrayDeque;


/**
 * Implementation class of {@link TraverserContext}, which is used in {@link DataSchemaRichContextTraverser}.
 */
class TraverserContextImpl implements TraverserContext
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
  TraverserContextImpl getNextContext(String nextTraversePathComponent, String nextSchemaPathSpecComponent,
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

  void setOriginalTopLevelSchema(DataSchema originalTopLevelSchema)
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

  @Override
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

  void setCurrentSchema(DataSchema currentSchema)
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

  void setEnclosingField(RecordDataSchema.Field enclosingField)
  {
    _enclosingField = enclosingField;
  }

  @Override
  public UnionDataSchema.Member getEnclosingUnionMember()
  {
    return _enclosingUnionMember;
  }

  void setEnclosingUnionMember(UnionDataSchema.Member enclosingUnionMember)
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
