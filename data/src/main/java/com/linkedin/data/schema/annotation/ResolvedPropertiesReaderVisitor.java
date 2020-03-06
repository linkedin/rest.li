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
import com.linkedin.data.schema.EnumDataSchema;
import com.linkedin.data.schema.FixedDataSchema;
import com.linkedin.data.schema.PathSpec;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This visitor will iterate over all leaf data schemas which could stores resolvedProperties after annotations in data schemas
 * are resolved.
 *
 * The resolvedProperties will be stored in a map. For example,
 * <pre>{@code
 * record Test {
 *    @customAnnotation = {
 *      "/f1/f2": "sth"
 *    }
 *    f0: record A {
 *      f1: A
 *      @AnotherAnnotation = "NONE"
 *      f2: string
 *    }
 *  }
 * }
 * </pre>
 *
 * One can expect following in the stored map
 * <pre>
 * {
 *   PathSpec of "/f0/f1/f1/f2": {
 *     "AnotherAnnotation" : "NONE"
 *   },
 *   PathSpec of "/f0/f1/f2": {
 *     "AnotherAnnotation" : "NONE"
 *     "customAnnotation" : "sth"
 *   }
 *   PathSpec of "f0/f2" : {
 *     "AnotherAnnotation" : "NONE"
 *   }
 * }
 * </pre>
 *
 * This map can be built by following way
 *
 * <pre>
 * ResolvedPropertiesReaderVisitor resolvedPropertiesReaderVisitor = new ResolvedPropertiesReaderVisitor();
 * DataSchemaRichContextTraverser traverser = new DataSchemaRichContextTraverser(resolvedPropertiesReaderVisitor);
 * traverser.traverse(processedDataSchema);
 * Map<PathSpec, Map<String, Object>> = resolvedPropertiesReaderVisitor.getLeafFieldsToResolvedPropertiesMap()
 * </pre>
 *
 * a leaf DataSchema is a schema that doesn't have other types of DataSchema linked from it.
 * Below types are leaf DataSchemas
 * {@link com.linkedin.data.schema.PrimitiveDataSchema},
 * {@link com.linkedin.data.schema.EnumDataSchema},
 * {@link com.linkedin.data.schema.FixedDataSchema}
 *
 * Other dataSchema types, for example {@link com.linkedin.data.schema.TyperefDataSchema} could link to another DataSchema
 * so it is not a leaf DataSchema
 */
public class ResolvedPropertiesReaderVisitor implements SchemaVisitor
{
  private Map<PathSpec, Map<String, Object>> _leafFieldsToResolvedPropertiesMap = new HashMap<>();
  private static final Logger LOG = LoggerFactory.getLogger(ResolvedPropertiesReaderVisitor.class);

  @Override
  public void callbackOnContext(TraverserContext context, DataSchemaTraverse.Order order)
  {
    if (order == DataSchemaTraverse.Order.POST_ORDER)
    {
      return;
    }

    DataSchema currentSchema = context.getCurrentSchema();
    if( (currentSchema.isPrimitive() || (currentSchema instanceof EnumDataSchema) ||
         (currentSchema instanceof FixedDataSchema)))
    {
      Map<String, Object> resolvedProperties = currentSchema.getResolvedProperties();
      _leafFieldsToResolvedPropertiesMap.put(
          new PathSpec(context.getSchemaPathSpec()), resolvedProperties);

      if (LOG.isDebugEnabled())
      {
        String mapStringified = resolvedProperties.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(
            Collectors.joining("&"));
        LOG.debug(String.format("/%s ::: %s", String.join("/", context.getSchemaPathSpec()), mapStringified));
      }
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
    return null;
  }

  /**
   * This method is deprecated and should not be used due to performance consideration, because this method will generate string and use that as map keys,
   * and it is not necessarily memory-efficient.
   *
   * User should use {@link #getLeafFieldsToResolvedPropertiesMap}, which use PathSpec object as map key.
   * @return a map with {@link PathSpec} string points to leaf field as map key and the resolved properties as its value
   *
   */
  @Deprecated
  public Map<String, Map<String, Object>> getLeafFieldsPathSpecToResolvedPropertiesMap()
  {
    return _leafFieldsToResolvedPropertiesMap.entrySet()
                                               .stream()
                                               .collect(Collectors.toMap(e -> e.getKey().toString(),
                                                                                  Map.Entry::getValue));

  }

  public Map<PathSpec, Map<String, Object>> getLeafFieldsToResolvedPropertiesMap()
  {
    return _leafFieldsToResolvedPropertiesMap;
  }
}
