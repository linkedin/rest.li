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
import com.linkedin.data.schema.DataSchemaTraverse;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.UnionDataSchema;
import java.util.ArrayDeque;


/**
 * Context defined by {@link DataSchemaRichContextTraverser} that will be updated and handled during traversal
 *
 * A new {@link TraverserContext} object will be created before entering child from parent.
 * In this way, {@link TraverserContext} behaves similar to elements inside a stack.
 */
public interface TraverserContext
{
  /**
   * Use this flag to control whether DataSchemaRichContextTraverser should continue to traverse from parent to child.
   * This variable can be set to null for default behavior. Setting to null is equal to not calling this method.
   */
  void setShouldContinue(Boolean shouldContinue);

  /**
   * {@link SchemaVisitor} should not modify other parts of {@link TraverserContext}.
   * But if {@link SchemaVisitor}s want to set customized context inside {@link TraverserContext} and retrieve from it,
   * {@link SchemaVisitor.VisitorContext} is how {@link SchemaVisitor} should use to
   * persist that customized data during traversal. In detail, when the {@link DataSchemaRichContextTraverser} traverses through schema,
   * new {@link TraverserContext} could be created, but {@link SchemaVisitor.VisitorContext} will be passed from old {@link TraverserContext} to
   * newly created one.
   *
   * @see SchemaVisitor.VisitorContext
   */
  void setVisitorContext(SchemaVisitor.VisitorContext visitorContext);

  /**
   * Getter method for {@link SchemaVisitor.VisitorContext} stored inside {@link TraverserContext}
   * @return {@link SchemaVisitor.VisitorContext}, if set.
   */
  SchemaVisitor.VisitorContext getVisitorContext();

  /**
   * Return the top level schema the traverser is traversing on.
   * @return top level schema;
   */
  DataSchema getTopLevelSchema();

  /**
   * During traversal, the {@link TraverserContext} contains the current schema under traversal
   * @return the current schema under traversal
   */
  DataSchema getCurrentSchema();

  /**
   * During traversal, the {@link TraverserContext} can return the parent schema of the current schema under traversal
   * If the current schema under traversal happens to be the top level schema, then this method returns null
   * @return the parent schema of the current schema.
   */
  DataSchema getParentSchema();

  /**
   * If the context is passing down from a {@link RecordDataSchema}, this attribute will be set with the enclosing
   * {@link RecordDataSchema.Field}
   */
  RecordDataSchema.Field getEnclosingField();

  /**
   * If the context is passing down from a {@link UnionDataSchema}, this attribute will be set with the enclosing
   * {@link UnionDataSchema.Member}
   */
  UnionDataSchema.Member getEnclosingUnionMember();

  /**
   * This traverse path is a very detailed path, and is same as the path used in {@link DataSchemaTraverse}
   * This path's every component corresponds to a move by traverser, and its components have TypeRef components and record name.
   * Example:
   * <pre>
   * record Test {
   *   f1: record Nested {
   *     f2: typeref TypeRef_Name=int
   *   }
   * }
   * </pre>
   * The traversePath to the f2 field would be as detailed as "/Test/f1/Nested/f2/TypeRef_Name/int"
   * Meanwhile its schema pathSpec is as simple as "/f1/f2"
   *
   */
  ArrayDeque<String> getTraversePath();

  /**
   * This is the path components corresponds to {@link PathSpec}, it would not have TypeRef component inside its component list, also it would only contain field's name
   */
  ArrayDeque<String> getSchemaPathSpec();

  /**
   * This attribute tells how currentSchema stored in the context is linked from its parentSchema
   * For example, if the {@link CurrentSchemaEntryMode} specify the currentSchema is an union member of parent Schema,
   * User can expect parentSchema is a {@link UnionDataSchema} and the {@link #getEnclosingUnionMember} should return the
   * enclosing union member that stores the current schema.
   *
   * @see CurrentSchemaEntryMode
   */
  CurrentSchemaEntryMode getCurrentSchemaEntryMode();
}
