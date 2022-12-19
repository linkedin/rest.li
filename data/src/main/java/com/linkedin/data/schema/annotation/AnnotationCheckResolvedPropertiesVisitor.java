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
import com.linkedin.data.schema.UnionDataSchema;
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

  private static final String FIELD_INDICATOR = "$field";
  private static final String UNION_MEMBER_KEY_INDICATOR = "$unionMemberKey";

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
    RecordDataSchema.Field schemaField = context.getEnclosingField();
    UnionDataSchema.Member unionMember = context.getEnclosingUnionMember();

    ArrayDeque<String> pathToSchema = new ArrayDeque<>(context.getSchemaPathSpec());
    pathToSchema.addFirst(((NamedDataSchema)context.getTopLevelSchema()).getName());

    if (schemaField != null && pathToSchema.peekLast().equals(context.getEnclosingField().getName()))
    {
      // Current node is a field of a record schema, get the field's annotation.
      // Add FIELD_INDICATOR in the pathSpec to differentiate field annotation and field type schema annotation.
      pathToSchema.addLast(FIELD_INDICATOR);
      PathSpec pathSpec = new PathSpec(pathToSchema);

      _nodeToResolvedPropertiesMap.put(pathSpec,
          new ImmutablePair<>(generateCompatibilityCheckContext(schemaField, unionMember, currentSchema, pathSpec),
              chooseProperties(schemaField.getResolvedProperties(), schemaField.getProperties())));
      pathToSchema.removeLast();
    }
    else if (unionMember!= null && pathToSchema.peekLast().equals(context.getEnclosingUnionMember().getUnionMemberKey()))
    {
      // Current node is a union member, get the union member key's annotation.
      // Add UNION_MEMBER_KEY_INDICATOR in the pathSpec to differentiate union member key annotation and union type schema annotation.
      pathToSchema.addLast(UNION_MEMBER_KEY_INDICATOR);
      PathSpec pathSpec = new PathSpec(pathToSchema);

      _nodeToResolvedPropertiesMap.put(pathSpec,
              new ImmutablePair<>(generateCompatibilityCheckContext(schemaField, unionMember, currentSchema, pathSpec), unionMember.getProperties()));
      pathToSchema.removeLast();
    }

    // If there are no resolvedProperties but properties, used the properties for annotation check.
    Map<String, Object> properties = chooseProperties(currentSchema.getResolvedProperties(), currentSchema.getProperties());
    PathSpec pathSpec = new PathSpec(pathToSchema);
    _nodeToResolvedPropertiesMap.put(pathSpec, new ImmutablePair<>(generateCompatibilityCheckContext(schemaField, unionMember, currentSchema, pathSpec), properties));
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

  @Override
  public boolean shouldIncludeTyperefsInPathSpec() {
    // Record typerefs in the path spec to avoid the typeref node and its child node from having the same pathSpec.
    return true;
  }

  public Map<PathSpec, Pair<CompatibilityCheckContext, Map<String, Object>>>  getNodeToResolvedPropertiesMap()
  {
    return _nodeToResolvedPropertiesMap;
  }

  private Map<String, Object> chooseProperties(Map<String, Object> preferredProperties, Map<String, Object> fallbackProperties)
  {
    return preferredProperties.isEmpty() ? fallbackProperties : preferredProperties;
  }

  private CompatibilityCheckContext generateCompatibilityCheckContext(RecordDataSchema.Field schemaField, UnionDataSchema.Member unionMember, DataSchema currentSchema, PathSpec pathSpec)
  {
    CompatibilityCheckContext checkContext = new CompatibilityCheckContext();
    checkContext.setPathSpecToSchema(pathSpec);
    checkContext.setCurrentDataSchema(currentSchema);
    checkContext.setSchemaField(schemaField);
    checkContext.setUnionMember(unionMember);
    return checkContext;
  }
}
