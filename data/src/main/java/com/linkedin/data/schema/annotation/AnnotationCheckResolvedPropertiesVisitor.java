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

import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaTraverse;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.annotation.SchemaAnnotationHandler.CompatibilityCheckContext;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;


/**
 * This visitor is used to get node in schema to it's resolvedProperties.
 * The nodesToResolvedPropertiesMap will be used for schema annotation compatibility check
 *
 * @author Yingjie Bi
 */
public class AnnotationCheckResolvedPropertiesVisitor implements SchemaVisitor
{
  private Map<PathSpec, Pair<CompatibilityCheckContext, Map<String, Object>>> _nodeToResolvedPropertiesMap = new HashMap<>();

  @Override
  public void callbackOnContext(TraverserContext context, DataSchemaTraverse.Order order)
  {
    // Skip execute when order is POST_ORDER to avoid traverse the same node twice.
    // Only execute this callback when order is PRE_ORDER.
    if (order == DataSchemaTraverse.Order.POST_ORDER)
    {
      return;
    }

    DataSchema currentSchema = context.getCurrentSchema();

    ArrayDeque<String> pathToSchema = context.getSchemaPathSpec().clone();
    pathToSchema.addFirst(((NamedDataSchema)context.getTopLevelSchema()).getName());
    PathSpec pathSpec = new PathSpec(pathToSchema);

    CompatibilityCheckContext compatibilityCheckContext = new CompatibilityCheckContext();

    compatibilityCheckContext.setCurrentDataSchema(currentSchema);
    compatibilityCheckContext.setSchemaField(context.getEnclosingField());
    compatibilityCheckContext.setUnionMember(context.getEnclosingUnionMember());
    compatibilityCheckContext.setPathSpecToSchema(pathSpec);

    // If there is no resolvedProperties but properties, used the properties for annotation check.
    Map<String, Object> properties;
    if (context.getEnclosingField() != null)
    {
      RecordDataSchema.Field field = context.getEnclosingField();
      properties = chooseProperties(field.getResolvedProperties(), field.getProperties());
    }
    else if (context.getEnclosingUnionMember() != null)
    {
      // There is no resolvedProperty for unionMember
      properties = context.getEnclosingUnionMember().getProperties();
    }
    else
    {
      properties = chooseProperties(currentSchema.getResolvedProperties(), currentSchema.getProperties());
    }

    _nodeToResolvedPropertiesMap.put(pathSpec, new ImmutablePair<>(compatibilityCheckContext, properties));
  }

  @Override
  public VisitorContext getInitialVisitorContext()
  {
    return new VisitorContext(){};
  }

  @Override
  public SchemaVisitorTraversalResult getSchemaVisitorTraversalResult()
  {
    return null;
  }

  public Map<PathSpec, Pair<CompatibilityCheckContext, Map<String, Object>>>  getNodeToResolvedPropertiesMap()
  {
    return _nodeToResolvedPropertiesMap;
  }

  private Map<String, Object> chooseProperties(Map<String, Object> preferredProperties, Map<String, Object> fallbackProperties)
  {
    return preferredProperties.isEmpty() ? fallbackProperties : preferredProperties;
  }
}
