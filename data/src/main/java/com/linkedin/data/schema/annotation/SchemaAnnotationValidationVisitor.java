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
import com.linkedin.data.schema.annotation.SchemaAnnotationHandler.AnnotationValidationResult;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * an {@link SchemaVisitor} implementation used for schema annotation validation.
 *
 * Will call {@link SchemaAnnotationHandler#validate(Map, SchemaAnnotationHandler.ValidationMetaData)}
 * to perform validation.
 *
 */
public class SchemaAnnotationValidationVisitor implements SchemaVisitor
{
  private final SchemaVisitorTraversalResult _schemaVisitorTraversalResult = new SchemaVisitorTraversalResult();
  private final SchemaAnnotationHandler _schemaAnnotationHandler;
  private static final Logger LOGGER = LoggerFactory.getLogger(SchemaAnnotationValidationVisitor.class);

  public SchemaAnnotationValidationVisitor(SchemaAnnotationHandler schemaAnnotationHandler)
  {
    _schemaAnnotationHandler = schemaAnnotationHandler;
  }

  @Override
  public void callbackOnContext(TraverserContext context, DataSchemaTraverse.Order order)
  {
    if (order == DataSchemaTraverse.Order.POST_ORDER)
    {
      //Skip post order
      return;
    }
    DataSchema schema = context.getCurrentSchema();
    SchemaAnnotationHandler.ValidationMetaData metaData = new SchemaAnnotationHandler.ValidationMetaData();
    metaData.setDataSchema(context.getCurrentSchema());
    metaData.setPathToSchema(context.getTraversePath());
    AnnotationValidationResult annotationValidationResult = _schemaAnnotationHandler.validate(schema.getResolvedProperties(),
                                                                                              metaData);
    if (!annotationValidationResult.isValid())
    {
      // merge messages
      getSchemaVisitorTraversalResult().addMessages(context.getSchemaPathSpec(), annotationValidationResult.getMessages());
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
    return _schemaVisitorTraversalResult;
  }


  public SchemaAnnotationHandler getSchemaAnnotationHandler()
  {
    return _schemaAnnotationHandler;
  }
}
