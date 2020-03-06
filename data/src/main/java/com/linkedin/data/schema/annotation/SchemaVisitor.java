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

import com.linkedin.data.schema.DataSchemaTraverse;


/**
 * Interface for SchemaVisitor, which will be called by {@link DataSchemaRichContextTraverser}.
 */
public interface SchemaVisitor
{
  /**
   * The callback function that will be called by {@link DataSchemaRichContextTraverser} visiting the dataSchema under traversal.
   * This function will be called TWICE within {@link DataSchemaRichContextTraverser}, during two {@link DataSchemaTraverse.Order}s
   * {@link DataSchemaTraverse.Order#PRE_ORDER} and {@link DataSchemaTraverse.Order#POST_ORDER} respectively.
   *
   * @param context
   * @param order the order given by {@link DataSchemaRichContextTraverser} to tell whether this call happens during pre order or post order
   */
  void callbackOnContext(TraverserContext context, DataSchemaTraverse.Order order);

  /**
   * {@link SchemaVisitor} implements this method to return an initial {@link VisitorContext}
   * {@link VisitorContext} will be stored inside {@link TraverserContext} and then
   * passed to {@link SchemaVisitor} during recursive traversal
   *
   * @return an initial {@link VisitorContext} that will be stored by {@link SchemaVisitor}
   *
   * @see VisitorContext
   */
  VisitorContext getInitialVisitorContext();

  /**
   * The visitor should store a {@link SchemaVisitorTraversalResult} which stores this visitor's traversal result.
   *
   * @return traversal result after the visitor traversed the schema
   */
  SchemaVisitorTraversalResult getSchemaVisitorTraversalResult();

  /**
   * A context that is defined and handled by {@link SchemaVisitor}
   *
   * The {@link DataSchemaRichContextTraverser} will get the initial context and then
   * passing this as part of {@link TraverserContext}
   *
   * {@link SchemaVisitor} implementations can store customized information that want to pass during recursive traversal here
   * similar to how {@link TraverserContext} is used.
   *
   * @see TraverserContext
   */
  interface VisitorContext
  {
  }
}
