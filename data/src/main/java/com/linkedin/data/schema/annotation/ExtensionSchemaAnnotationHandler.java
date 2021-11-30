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

import com.linkedin.data.DataMap;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.schema.compatibility.CompatibilityMessage;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;


/**
 * This SchemaAnnotationHandler is used to check extension schema annotation(@extension) compatibility
 *
 * @author Yingjie Bi
 */
public class ExtensionSchemaAnnotationHandler implements SchemaAnnotationHandler
{
  public final static String EXTENSION_ANNOTATION_NAMESPACE = "extension";

  @Override
  public ResolutionResult resolve(List<Pair<String, Object>> propertiesOverrides,
      ResolutionMetaData resolutionMetadata)
  {
    // No-op, for extension schema there is no property resolve need.
    return new ResolutionResult();
  }

  @Override
  public String getAnnotationNamespace()
  {
    return EXTENSION_ANNOTATION_NAMESPACE;
  }

  @Override
  public AnnotationValidationResult validate(Map<String, Object> resolvedProperties, ValidationMetaData metaData)
  {
    // No-op, for extension schema there is no property resolve need, therefore there is no annotation validate need.
    return new AnnotationValidationResult();
  }

  @Override
  public SchemaVisitor getVisitor()
  {
    // No need to override properties, use IdentitySchemaVisitor to skip schema traverse.
    return new IdentitySchemaVisitor();
  }

  @Override
  public boolean implementsCheckCompatibility()
  {
    return true;
  }

  @Override
  public AnnotationCompatibilityResult checkCompatibility(Map<String, Object> prevResolvedProperties, Map<String, Object> currResolvedProperties,
      CompatibilityCheckContext prevContext, CompatibilityCheckContext currContext)
  {
    AnnotationCompatibilityResult result = new AnnotationCompatibilityResult();
    // Both prevResolvedProperties and currResolvedProperties contain extension annotation namespace, check any changes of annotations on the existing fields.
    if (prevResolvedProperties.containsKey(EXTENSION_ANNOTATION_NAMESPACE) && currResolvedProperties.containsKey(EXTENSION_ANNOTATION_NAMESPACE))
    {
      DataMap prevAnnotations = (DataMap) prevResolvedProperties.get(EXTENSION_ANNOTATION_NAMESPACE);
      DataMap currAnnotations = (DataMap) currResolvedProperties.get(EXTENSION_ANNOTATION_NAMESPACE);
      prevAnnotations.forEach((key, value) ->
      {
        if (currAnnotations.containsKey(key))
        {
          // Check annotation value changes.
          if (!prevAnnotations.get(key).equals(currAnnotations.get(key)))
          {
            appendCompatibilityMessage(result, CompatibilityMessage.Impact.ANNOTATION_INCOMPATIBLE_CHANGE,
                "Updating extension annotation field: \"%s\" value is considering as a backward incompatible change.",
                key, currContext.getPathSpecToSchema());
          }
          currAnnotations.remove(key);
        }
        else
        {
          // An existing annotation field is removed.
          appendCompatibilityMessage(result, CompatibilityMessage.Impact.ANNOTATION_INCOMPATIBLE_CHANGE,
              "Removing extension annotation field: \"%s\" is considering as a backward incompatible change.",
              key, currContext.getPathSpecToSchema());
        }
      });

      currAnnotations.forEach((key, value) ->
      {
        // Adding an extension annotation field.
        appendCompatibilityMessage(result, CompatibilityMessage.Impact.ANNOTATION_INCOMPATIBLE_CHANGE,
            "Adding extension annotation field: \"%s\" is a backward incompatible change.",
            key, currContext.getPathSpecToSchema());
      });
    }
    else if (prevResolvedProperties.containsKey(EXTENSION_ANNOTATION_NAMESPACE))
    {
      // Only previous schema has extension annotation, it means the extension annotation is removed in the current schema.
      if (currContext.getPathSpecToSchema() != null)
      {
        appendCompatibilityMessage(result, CompatibilityMessage.Impact.ANNOTATION_INCOMPATIBLE_CHANGE,
            "Removing extension annotation is a backward incompatible change.",
            null, prevContext.getPathSpecToSchema());
      }
      else
      {
        // an existing field with extension annotation is removed
        appendCompatibilityMessage(result, CompatibilityMessage.Impact.ANNOTATION_INCOMPATIBLE_CHANGE,
            "Removing field: \"%s\" with extension annotation is a backward incompatible change.",
            prevContext.getSchemaField().getName(), prevContext.getPathSpecToSchema());
      }
    }
    else
    {
      if (prevContext.getPathSpecToSchema() != null)
      {
        appendCompatibilityMessage(result, CompatibilityMessage.Impact.ANNOTATION_INCOMPATIBLE_CHANGE,
            "Adding extension annotation on an existing field: \"%s\" is backward incompatible change",
            prevContext.getSchemaField().getName() , currContext.getPathSpecToSchema());
      }
      else
      {
        // Adding a new injected field with extension annotation.
        appendCompatibilityMessage(result, CompatibilityMessage.Impact.ANNOTATION_COMPATIBLE_CHANGE,
            "Adding extension annotation on new field: \"%s\" is backward compatible change", currContext.getSchemaField().getName() , currContext.getPathSpecToSchema());
      }
    }
    return result;
  }

  private void appendCompatibilityMessage(AnnotationCompatibilityResult result, CompatibilityMessage.Impact impact, String message, String context, PathSpec pathSpec)
  {
    CompatibilityMessage compatibilityMessage = new CompatibilityMessage(pathSpec, impact, message, context);
    result.addMessage(compatibilityMessage);
  }
}