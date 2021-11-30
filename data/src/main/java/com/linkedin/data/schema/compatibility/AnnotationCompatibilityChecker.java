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
package com.linkedin.data.schema.compatibility;

import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.schema.annotation.DataSchemaRichContextTraverser;
import com.linkedin.data.schema.annotation.AnnotationCheckResolvedPropertiesVisitor;
import com.linkedin.data.schema.annotation.SchemaAnnotationHandler;
import com.linkedin.data.schema.annotation.SchemaAnnotationHandler.CompatibilityCheckContext;
import com.linkedin.data.schema.annotation.SchemaAnnotationHandler.AnnotationCompatibilityResult;
import com.linkedin.data.schema.annotation.SchemaAnnotationProcessor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *  The annotation compatibility check is part of the annotation processing framework.
 *  If users use annotation processor to resolve properties,
 *  they may also provide a annotation compatibility method to define the way to check annotation's compatibility.
 *  In this checker, it will call the {@link SchemaAnnotationHandler#checkCompatibility} method.
 *
 * @author Yingjie Bi
 */
public class AnnotationCompatibilityChecker
{
  private static final Logger _log = LoggerFactory.getLogger(AnnotationCompatibilityChecker.class);

  /**
   * Check the pegasus schema annotation compatibility
   * Process prevSchema and currSchema in the SchemaAnnotationProcessor to get the resolved result with resolvedProperties.
   * then using the resolvedProperties to do the annotation compatibility check.
   * @param prevSchema previous data schema
   * @param currSchema current data schema
   * @param handlers SchemaAnnotationHandler list
   * @return List<AnnotationCompatibilityResult>
   */
  public static List<AnnotationCompatibilityResult> checkPegasusSchemaAnnotation(DataSchema prevSchema, DataSchema currSchema,
      List<SchemaAnnotationHandler> handlers)
  {
    // Update handler list to only contain handlers with implementation of checkCompatibility.
    handlers = handlers
        .stream()
        .filter( h -> h.implementsCheckCompatibility())
        .collect(Collectors.toList());
    SchemaAnnotationProcessor.SchemaAnnotationProcessResult prevSchemaResult = processSchemaAnnotation(prevSchema, handlers);
    SchemaAnnotationProcessor.SchemaAnnotationProcessResult currSchemaResult = processSchemaAnnotation(currSchema, handlers);
    Map<PathSpec, Pair<CompatibilityCheckContext, Map<String, Object>>> prevResolvedPropertiesMap
        = getNodeToResolvedProperties(prevSchemaResult);
    Map<PathSpec, Pair<CompatibilityCheckContext, Map<String, Object>>> currResolvedPropertiesMap
        = getNodeToResolvedProperties(currSchemaResult);

    return getCompatibilityResult(prevResolvedPropertiesMap, currResolvedPropertiesMap, handlers);
  }

  /**
   * Iterate the nodeToResolverPropertiesMap, if a node's resolvedProperty contains the same annotationNamespace as SchemaAnnotationHandler,
   * calling annotationCompatibilityCheck api which is provided in the SchemaAnnotationHandler to do the annotation compatibility check.
   */
  private static List<AnnotationCompatibilityResult> getCompatibilityResult(Map<PathSpec, Pair<CompatibilityCheckContext,
      Map<String, Object>>> prevResolvedPropertiesMap, Map<PathSpec, Pair<CompatibilityCheckContext,
      Map<String, Object>>> currResolvedPropertiesMap, List<SchemaAnnotationHandler> handlers)
  {
    List<AnnotationCompatibilityResult> results = new ArrayList<>();

    prevResolvedPropertiesMap.forEach((pathSpec, prevCheckContextAndResolvedProperty) ->
    {
      Map<String, Object> prevResolvedProperties = prevCheckContextAndResolvedProperty.getValue();
      handlers.forEach(handler ->
      {
        String annotationNamespace = handler.getAnnotationNamespace();
        if (currResolvedPropertiesMap.containsKey(pathSpec))
        {
          // If previous schema node and current schema node have the same pathSpec,
          // they may or may not contain the same annotation namespace as SchemaAnnotationHandler, we need to check further.
          Pair<CompatibilityCheckContext, Map<String, Object>>
              currCheckContextAndResolvedProperty = currResolvedPropertiesMap.get(pathSpec);
          Map<String, Object> currResolvedProperties = currCheckContextAndResolvedProperty.getValue();

          // If prevResolvedProperties or currResolvedProperties contains the same namespace as the provided SchemaAnnotationHandler,
          // we do the annotation check.
          if (prevResolvedProperties.containsKey(annotationNamespace) || currResolvedProperties.containsKey(
              annotationNamespace))
          {
            AnnotationCompatibilityResult result =
                handler.checkCompatibility(prevResolvedProperties, currResolvedProperties,
                    prevCheckContextAndResolvedProperty.getKey(), currCheckContextAndResolvedProperty.getKey());
            results.add(result);
          }
        }
        else
        {
          if (prevResolvedProperties.containsKey(annotationNamespace))
          {
            // prevResolvedPropertiesMap has a pathSpec which the newResolvedPropertiesMap does not have,
            // it means an existing field is removed.
            // pass an empty currResolvedPropertiesMap and empty currAnnotationContext to the annotation check.
            AnnotationCompatibilityResult result =
                handler.checkCompatibility(prevResolvedProperties, new HashMap<>(),
                    prevCheckContextAndResolvedProperty.getKey(), new CompatibilityCheckContext());
            results.add(result);
          }
        }
      });
      if (currResolvedPropertiesMap.containsKey(pathSpec))
      {
        currResolvedPropertiesMap.remove(pathSpec);
      }
    });

    currResolvedPropertiesMap.forEach((pathSpec, currCheckContextAndResolvedProperty) ->
    {
      handlers.forEach(handler ->
      {
        String annotationNamespace = handler.getAnnotationNamespace();
        Map<String, Object> currResolvedProperties = currCheckContextAndResolvedProperty.getValue();
        if (currResolvedProperties.containsKey(annotationNamespace))
        {
          // currResolvedPropertiesMap has a PathSpec which the prevResolvedPropertiesMap does not have,
          // it means there is a new field with new annotations,
          // pass an empty prevResolvedPropertiesMap and empty prevAnnotationContext to the annotation check.
          AnnotationCompatibilityResult result = handler.checkCompatibility(new HashMap<>(), currResolvedProperties,
              new CompatibilityCheckContext(), currCheckContextAndResolvedProperty.getKey());
          results.add(result);
        }
      });
    });
    return results;
  }

  private static Map<PathSpec, Pair<CompatibilityCheckContext, Map<String, Object>>> getNodeToResolvedProperties(
      SchemaAnnotationProcessor.SchemaAnnotationProcessResult result)
  {
    AnnotationCheckResolvedPropertiesVisitor visitor = new AnnotationCheckResolvedPropertiesVisitor();
    DataSchemaRichContextTraverser traverser = new DataSchemaRichContextTraverser(visitor);
    traverser.traverse(result.getResultSchema());
    return visitor.getNodeToResolvedPropertiesMap();
  }

  private static SchemaAnnotationProcessor.SchemaAnnotationProcessResult processSchemaAnnotation(DataSchema dataSchema,
      List<SchemaAnnotationHandler> handlers)
  {
    SchemaAnnotationProcessor.SchemaAnnotationProcessResult result =
        SchemaAnnotationProcessor.process(handlers, dataSchema, new SchemaAnnotationProcessor.AnnotationProcessOption(), false);
    // If any of the nameDataSchema failed to be processed, throw exception
    if (result.hasError())
    {
      String schemaName = ((NamedDataSchema) dataSchema).getFullName();
      _log.error("Annotation processing for data schema [{}] failed, detailed error: \n",
          schemaName);
      _log.error(result.getErrorMsgs());
      throw new RuntimeException("Could not process annotation of data schema: " + schemaName + " while processing annotation compatibility check.");
    }
    return result;
  }
}
