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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Implementation class of {@link TraverserContext}, which is used in {@link DataSchemaRichContextTraverser}.
 */
class TraverserContextImpl implements TraverserContext
{
  private Boolean _shouldContinue = null;
  private DataSchema _parentSchema;
  private RecordDataSchema.Field _enclosingField;
  private UnionDataSchema.Member _enclosingUnionMember;
  private CurrentSchemaEntryMode _currentSchemaEntryMode;
  private SchemaVisitor.VisitorContext _visitorContext;

  private final DataSchema _originalTopLevelSchema;
  private final DataSchema _currentSchema;
  private final ArrayList<String> _traversePath;
  private final ArrayList<String> _schemaPathSpec;
  private final int _traversePathLimit;
  private final int _schemaPathLimit;

  TraverserContextImpl(DataSchema originalTopLevelSchema, DataSchema currentSchema, SchemaVisitor.VisitorContext visitorContext) {
    _originalTopLevelSchema = originalTopLevelSchema;
    _currentSchema = currentSchema;
    _visitorContext = visitorContext;
    _traversePath = new ArrayList<>();
    _traversePath.add(currentSchema.getUnionMemberKey());
    _schemaPathSpec = new ArrayList<>();
    _traversePathLimit = 1;
    _schemaPathLimit = 0;
  }

  private TraverserContextImpl(TraverserContextImpl existing, DataSchema nextSchema, int newSchemaPathLimit, int newTraversePathLimit) {
    _originalTopLevelSchema = existing._originalTopLevelSchema;
    _currentSchema = nextSchema;
    _visitorContext = existing._visitorContext;
    _traversePath = existing._traversePath;
    _schemaPathSpec = existing._schemaPathSpec;
    _schemaPathLimit = newSchemaPathLimit;
    _traversePathLimit = newTraversePathLimit;
  }

  @Override
  public SchemaVisitor.VisitorContext getVisitorContext()
  {
    return _visitorContext;
  }

  /**
   * Generate a new {@link TraverserContext} for next recursion in
   * {@link DataSchemaRichContextTraverser#doRecursiveTraversal(TraverserContextImpl)}
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
    // SchemaPathSpecComponent could be null if nextSchema is a TypeRefDataSchema
    final boolean hasNextSchemaComponent = (nextSchemaPathSpecComponent != null);
    final int newSchemaPathLimit = hasNextSchemaComponent ? _schemaPathLimit + 1 : _schemaPathLimit;
    TraverserContextImpl nextContext =
        new TraverserContextImpl(this, nextSchema, newSchemaPathLimit, _traversePathLimit + 2);
    nextContext.setParentSchema(this.getCurrentSchema());
    nextContext.setEnclosingField(this.getEnclosingField());
    nextContext.setEnclosingUnionMember(this.getEnclosingUnionMember());

    // Add the next component to the traverse path.
    safeAdd(_traversePath, _traversePathLimit, nextTraversePathComponent);
    // Add full name of the schema to the traverse path.
    safeAdd(_traversePath, _traversePathLimit + 1, nextSchema.getUnionMemberKey());

    // Add the schema path.
    if (hasNextSchemaComponent) {
      safeAdd(_schemaPathSpec, _schemaPathLimit, nextSchemaPathSpecComponent);
    }

    nextContext.setCurrentSchemaEntryMode(nextSchemaEntryMode);
    return nextContext;
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
  public List<String> getSchemaPathSpec()
  {
    return Collections.unmodifiableList(_schemaPathSpec.subList(0, _schemaPathLimit));
  }

  @Override
  public DataSchema getCurrentSchema()
  {
    return _currentSchema;
  }

  @Override
  public List<String> getTraversePath()
  {
    return Collections.unmodifiableList(_traversePath.subList(0, _traversePathLimit));
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

  private static void safeAdd(List<String> list, int index, String value) {
    assert value != null;
    if (index < list.size()) {
      list.set(index, value);
    } else {
      list.add(index, value);
    }
  }
}
