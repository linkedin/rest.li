/*
 * Copyright (c) 2020 LinkedIn Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.linkedin.data.schema.annotation;

import com.linkedin.data.schema.DataSchemaTraverse;


/**
 * This SchemaVisitor is used when annotation override is not needed.
 * It will not traverse the data schema.
 *
 * @author Yingjie Bi
 */
public class IdentitySchemaVisitor implements SchemaVisitor
{

  @Override
  public void callbackOnContext(TraverserContext context, DataSchemaTraverse.Order order) {
    // skip post order traverse
    if (order == DataSchemaTraverse.Order.POST_ORDER)
    {
      return;
    }

    // If current schema is a root node, set shouldContinue to false to avoid traverse schema.
    if (context.getParentSchema() == null)
    {
      context.setShouldContinue(false);
    }
  }

  @Override
  public VisitorContext getInitialVisitorContext()
  {
    return new VisitorContext(){};
  }

  @Override
  public SchemaVisitorTraversalResult getSchemaVisitorTraversalResult()
  {
    return new SchemaVisitorTraversalResult();
  }
}
