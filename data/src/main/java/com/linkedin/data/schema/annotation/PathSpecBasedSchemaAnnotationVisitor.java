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

import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaTraverse;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.StringDataSchema;
import com.linkedin.data.schema.TyperefDataSchema;
import com.linkedin.data.schema.UnionDataSchema;
import com.linkedin.data.schema.util.CopySchemaUtil;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import static java.util.stream.Collectors.toList;


/**
 * {@link PathSpecBasedSchemaAnnotationVisitor} is a {@link SchemaVisitor} implementation
 * that check and parse PathSpec overrides during Schema traverse.
 *
 * For a schema that has fields that were annotated with certain annotation namespace, schema writers can override field's annotation
 * values using overriding. And overriding using following syntax could be interpreted and handled by this parser.
 *
 * Example pdl schema with overrides:
 *
 * <pre>{@code
 * @customAnnotation= {"/f1/f2" : "2nd layer" }
 * f: record Record1 {
 *     @customAnnotation= {"/f2" : "1st layer" }
 *     f1: record Record2 {
 *         @customAnnotation = "OriginalValue"
 *         f2: string
 *     }
 * }
 * }
 * </pre>
 *
 * In this example, the annotation namespace being annotated here is "customAnnotation".
 * The string field f2's customAnnotation "OriginalValue" was overridden by its upper layer fields.
 * Both  `{"/f1/f2" : "2nd layer" }` and `{"/f2" : "1st layer" }` are its overrides and
 * the overrides value is specified using PathSpec to point to the field to be overridden.
 *
 * The "originalValue" can be {@link com.linkedin.data.DataMap} or {@link com.linkedin.data.DataList} or primitive types
 * but the overrides needs to be a key-value pair, where the key is PathSpec string representation.
 *
 * also @see {@link SchemaAnnotationHandler}
 */
public class PathSpecBasedSchemaAnnotationVisitor implements SchemaVisitor
{
  private final SchemaAnnotationHandler _handler;
  private final SchemaVisitorTraversalResult _schemaVisitorTraversalResult = new SchemaVisitorTraversalResult();
  final String OVERRIDE_PATH_ERROR_MSG_TEMPLATE_MAL_FORMATTED_KEY = "MalFormatted key as PathSpec found: %s";
  final String OVERRIDE_PATH_ERROR_MSG_ENTRIES_NOT_IN_MAP = "Overrides entries should be key-value pairs that form a map";
  final String OVERRIDE_PATH_ERROR_MSG_ENTRIES_NOT_FOR_INCLUDED =
      "Overrides entries in record schema properties should be pointing to fields in included record schemas only. The pathSpec defined %s is not pointing to a included field.";
  final String RECORD_SCHEMA_LEVEL_ANNOTATION_NOT_ALLOWED = "Found annotations annotated at record schema level for annotation namespace \"%s\", which is not allowed";

  enum OverridePathErrorMsg
  {
    DOES_NOT_MATCH_NAME("Overriding pathSpec defined %s does not point to a valid primitive field"),
    TOO_LONG("Overriding pathSpec defined %s does not point to a valid primitive field: Path might be too long"),
    TOO_SHORT("Overriding pathSpec defined %s does not point to a valid primitive field: Path might be too short");
    private final String _error_msg;

    OverridePathErrorMsg(String error_msg)
    {
      _error_msg = error_msg;
    }

    @Override
    public String toString()
    {
      return _error_msg;
    }
  }
  /**
   * Keep a mapping from original DataSchema read from {@link DataSchemaRichContextTraverser} to a DataSchema constructed by this visitor
   */
  private final IdentityHashMap<DataSchema, DataSchema> _seenDataSchemaMapping = new IdentityHashMap<>();
  /**
   * This variable stores an updated schema when {@link PathSpecBasedSchemaAnnotationVisitor} need to update the schema under traversal.
   * It was initialized in {@link #createOrReUseSchemaAndAttachToParent(TraverserContext, boolean)}
   */
  private DataSchema _schemaConstructed = null;
  /**
   * Use this data structure to store whether a record schema "RecordA" has fields contains overrides to a record schema "RecordB"
   * i.e. We see an edge from RecordA to RecordB
   * The key will only be record's full name, value will be a {@link Set} contains record's full name
   *
   * For example
   * <pre>{@code
   * record RecordA {
   *   @customAnnotation = {"/recordAf3": ""} // RecordA overrides to RecordC
   *   recordAf1: RecordC
   *   recordAf2: record RecordB {
   *     @customAnnotation = {"/recordAf3": ""} // RecordB overrides to RecordA
   *     recordBf1: RecordA
   *     recordBf2: string
   *   }
   *   recordAf3: string
   *   @customAnnotation = {"/recordBf2": ""} // RecordA overrides to RecordD
   *   recordAf4: RecordD
   * }
   * }
   * </pre>
   *
   * In this example, we see edge
   * RecordA to RecordC
   * RecordB to RecordA
   * RecordA to RecordD
   *
   * The _directedEdges should have entries
   * <pre>
   * {
   *   "RecordA": set(["RecordC", "RecordD"])
   *   "RecordB": set(["RecordA"])
   * }
   * </pre>
   *
   */
  private Map<String, Set<String>> _directedEdges = new HashMap<>();
  /**
   * If a cycle was detected by checking the edge using {@link #detectCycle(String, String)},
   * a string pair representing this edge would be added to the HashSet and cached.
   */
  private HashSet<Pair<String, String>> _cycleCache = new HashSet<>();

  /**
   * If the schema A has annotations overrides that resolves to schema B and its descendents.
   * then we have an directed edge from A to B.
   *
   * Given a directed edge and a _directedEdges map storing all edges seen, this function detects whether adding new edge to the edge map would
   * produce any cycles.
   *
   * Cyclic referencing detection in schema override referencing is essentially detecting whether edges seen are forming any cycles
   *
   * @param startSchemaName the schema name of the start of edge
   * @param endSchemaName the schema name of the end of the edge
   * @return a boolean to tell whether the edge from startSchemaName to endSchemaName form a cycle
   */
  private boolean detectCycle(String startSchemaName, String endSchemaName)
  {
    if (startSchemaName.equals(endSchemaName) || _cycleCache.contains(ImmutablePair.of(startSchemaName, endSchemaName)))
    {
      return true;
    }

    // There were no cycles before checking this edge(startSchemaName -> endSchemaName) ,
    // So the goal is see if can find path (endSchemaName -> startSchemaName)
    HashSet<String> visited = new HashSet<>();
    boolean wouldFormCycle = checkReachability(endSchemaName, startSchemaName, visited, _directedEdges);

    if (wouldFormCycle)
    {
      _cycleCache.add(ImmutablePair.of(startSchemaName, endSchemaName));
    }
    return wouldFormCycle;
  }

  /**
   * DFS routine to check if we can reach targetSchemaName from currentSchemaName
   * @param currentSchemaName the current schema name where the search started
   * @param targetSchemaName  the target schema name to be searched
   * @param visited a hashSet holds visited schema names
   * @param edges a map tells whether schema A has annotation overrides that could resolve to schema B and its descendents (i.e edge A-B)
   * @return whether the targetSchemaName can be reached from currentSchemaName using recursive dfs search.
   */
  private static boolean checkReachability(String currentSchemaName, String targetSchemaName, HashSet<String> visited,
                                           Map<String, Set<String>> edges)
  {
    visited.add(currentSchemaName);

    if (currentSchemaName.equals(targetSchemaName))
    {
      return true;
    }
    Set<String> nextNodes = edges.computeIfAbsent(currentSchemaName, key -> new HashSet<>());
    for (String nextNode: nextNodes)
    {
      if (!visited.contains(nextNode))
      {
        if (checkReachability(nextNode, targetSchemaName, visited, edges))
        {
          return true;
        }
      }
    }
    return false;
  }

  public PathSpecBasedSchemaAnnotationVisitor(SchemaAnnotationHandler handler)
  {
    _handler = handler;
    assert(_handler != null && _handler.getAnnotationNamespace() != null);
  }

  @Override
  public VisitorContext getInitialVisitorContext()
  {
    return new PathSpecTraverseVisitorContext();
  }

  @Override
  public SchemaVisitorTraversalResult getSchemaVisitorTraversalResult()
  {
    return _schemaVisitorTraversalResult;
  }

  @Override
  public void callbackOnContext(TraverserContext context, DataSchemaTraverse.Order order)
  {

    if (order == DataSchemaTraverse.Order.POST_ORDER)
    {
      // Use post order visit to validate override paths
      VisitorContext postVisitContext = context.getVisitorContext();
      List<AnnotationEntry> annotationEntries =
          ((PathSpecTraverseVisitorContext) postVisitContext).getAnnotationEntriesFromParentSchema();

      // Do annotationEntry validity checking
      for (AnnotationEntry annotationEntry : annotationEntries)
      {
        if (annotationEntry.isOverride() &&
            (annotationEntry.getOverridePathValidStatus() == AnnotationEntry.OverridePathValidStatus.UNCHECKED))
        {
          markAnnotationEntryInvalid(annotationEntry, OverridePathErrorMsg.DOES_NOT_MATCH_NAME);
        }
      }

      if (context.getParentSchema() == null)
      {
        getSchemaVisitorTraversalResult().setConstructedSchema(_schemaConstructed);
      }
      return;
    }

    VisitorContext visitorContext = context.getVisitorContext();
    // Prepare visitorContext for next level recursion
    PathSpecTraverseVisitorContext newVisitorContext = new PathSpecTraverseVisitorContext();
    // {@link PathSpecBasedSchemaAnnotationVisitor} will build new skeleton schema on the fly, after seeing the original schema
    // If there has been a skeleton schema already built for one data schema, it will reuse that cached one
    // also see {@link PathSpecTraverseVisitorContext}
    DataSchema newSchema = null;
    DataSchema parentSchema = context.getParentSchema();
    DataSchema currentSchema = context.getCurrentSchema();
    List<AnnotationEntry> currentAnnotationEntries = ((PathSpecTraverseVisitorContext) visitorContext).getAnnotationEntriesFromParentSchema();

    // match & filter current overrides
    if (parentSchema != null && !(parentSchema.getType() == DataSchema.Type.TYPEREF))
    {
      // skip if parent is Typeref because schemaPathSpec would not contain Typeref component.
      String pathSpecMatchingSegment = context.getSchemaPathSpec().peekLast();
      currentAnnotationEntries = currentAnnotationEntries.stream()
                                         // Filter out overrides that matched to current path segment for match.
                                         .filter(annotationEntry ->
                                                     (annotationEntry.getOverridePathValidStatus() ==
                                                      AnnotationEntry.OverridePathValidStatus.UNCHECKED) &&
                                                     annotationEntry.getRemainingPaths().size() > 0 &&
                                                     Objects.equals(
                                                         annotationEntry.getRemainingPaths().peekFirst(),
                                                         pathSpecMatchingSegment))
                                         // After the pathSegment has been matched, move it from remaining path to matched path
                                         .peek(annotationEntry ->
                                               {
                                                 annotationEntry.getMatchedPaths().add(pathSpecMatchingSegment);
                                                 annotationEntry.getRemainingPaths().pollFirst();
                                               }).collect(toList());
    }
    assert (currentAnnotationEntries.stream()
                            .filter(AnnotationEntry::isOverride)
                            .allMatch(annotationEntry -> annotationEntry.getOverridePathValidStatus() ==
                                                         AnnotationEntry.OverridePathValidStatus.UNCHECKED));
    // add {@link annotationEntry}s from enclosing schema or field
    if (parentSchema != null)
    {
      switch (parentSchema.getType())
      {
        case RECORD:
          RecordDataSchema.Field enclosingField = context.getEnclosingField();
          ArrayDeque<String> fullTraversePath = new ArrayDeque<>(context.getTraversePath());
          // Need to exclude this currentSchema's path so that it is field's path
          fullTraversePath.pollLast();
          currentAnnotationEntries.addAll(generateAnnotationEntryFromField(enclosingField, fullTraversePath));

          break;
        case TYPEREF:
          currentAnnotationEntries.addAll(
              generateAnnotationEntryFromTypeRefSchema((TyperefDataSchema) parentSchema, context.getTraversePath()));
          break;
        default:
          break;
      }
    }
    // add {@link annotationEntry}s from named schema
    currentAnnotationEntries.addAll(generateAnnotationEntryFromNamedSchema(currentSchema, context.getTraversePath()));

    // cyclic referencing checking:
    // after merging the override paths from the RecordDataSchema's fields
    // need to check whether this will produce cyclic overriding
    // @see {@link #detectCycle} for details
    // Note: cyclic annotation in TypeRef is also handled through its de-referenced record schema
    if (currentSchema.getType() == DataSchema.Type.RECORD)
    {
      String currentSchemaFullName = ((RecordDataSchema) currentSchema).getFullName();
      for (AnnotationEntry annotationEntry : currentAnnotationEntries)
      {
        String overrideStartSchemaName = annotationEntry.getStartSchemaName();
        if (detectCycle(overrideStartSchemaName, currentSchemaFullName))
        {
          //If cycles found, report errors
          getSchemaVisitorTraversalResult().addMessage(context.getTraversePath(),
                                                 "Found overrides that forms a cyclic-referencing: Overrides entry in " +
                                                 "traverser path \"%s\" with its pathSpec value \"%s\" is pointing to the field " +
                                                 "with traverser path \"%s\" and schema name \"%s\", this is causing cyclic-referencing.",
                                                       new PathSpec(annotationEntry.getPathToAnnotatedTarget().toArray(new String[0])).toString(),
                                                       annotationEntry.getOverridePathSpecStr(),
                                                       new PathSpec(context.getTraversePath().toArray(new String[0])).toString(),
                                                       currentSchemaFullName);
          context.setShouldContinue(Boolean.FALSE);
          newVisitorContext.setAnnotationEntriesFromParentSchema(currentAnnotationEntries);
          context.setVisitorContext(newVisitorContext);
          return;
        }
        else
        {
          // If no cycles found, add to current edges seen
          _directedEdges.computeIfAbsent(overrideStartSchemaName, key -> new HashSet<>()).add(currentSchemaFullName);
        }
      }
    }

    // process current schema
    try
    {
      if (DataSchemaRichContextTraverser.isLeafSchema(currentSchema))
      {
        newSchema = createOrReUseSchemaAndAttachToParent(context, (currentAnnotationEntries.size() != 0));
        newSchema.getResolvedProperties().putAll(
            resolveAnnotationEntries(currentAnnotationEntries, context.getSchemaPathSpec()));

        // Do annotationEntry validity checking
        for (AnnotationEntry annotationEntry : currentAnnotationEntries)
        {
          if (annotationEntry.isOverride())
          {
            if (annotationEntry.getRemainingPaths().size() == 0)
            {
              annotationEntry.setOverridePathValidStatus(AnnotationEntry.OverridePathValidStatus.VALID);
            }
            else
            {
              markAnnotationEntryInvalid(annotationEntry, OverridePathErrorMsg.TOO_LONG);
            }
          }
        }
      }
      else if (currentSchema.isComplex())
      {
        // Either all non-overrides to TypeRefDataSchema, or all overrides to other complex dataSchema
        assert (currentAnnotationEntries.stream().noneMatch(AnnotationEntry::isOverride) ||
                currentAnnotationEntries.stream().allMatch(AnnotationEntry::isOverride));

        // Do annotationEntry validity checking
        if ((currentSchema.getType() != DataSchema.Type.TYPEREF))
        {
          for (AnnotationEntry annotationEntry : currentAnnotationEntries)
          {
            if (annotationEntry.isOverride() && (annotationEntry.getRemainingPaths().size() == 0))
            {
              markAnnotationEntryInvalid(annotationEntry, OverridePathErrorMsg.TOO_SHORT);
            }
          }
        }

        if (currentAnnotationEntries.stream()
                            .anyMatch(annotationEntry -> !annotationEntry.isOverride() || // non-overrides from typeref
                                                         (annotationEntry.getOverridePathValidStatus() ==
                                                          AnnotationEntry.OverridePathValidStatus.UNCHECKED)))
        {
          // If there are unresolved annotation entries that resolving to complex data schema and its descendants.
          // Need to tell the traverser to continue traversing
          newSchema = createOrReUseSchemaAndAttachToParent(context, true);
          context.setShouldContinue(Boolean.TRUE);
        }
        else
        {
          // Order matters: Need to check "seen" before creating new or reuse
          context.setShouldContinue(!_seenDataSchemaMapping.containsKey(currentSchema));
          newSchema = createOrReUseSchemaAndAttachToParent(context, false);
        }
      }
    }
    catch (CloneNotSupportedException e)
    {
      throw new IllegalStateException(
          String.format("encounter unexpected CloneNotSupportedException at traverse path location %s",
                        Arrays.toString(context.getTraversePath().toArray())), e);
    }
    // Process record schema with "included" fields, before setting overrides for next visitorContext
    currentAnnotationEntries.addAll(generateAnnotationEntryFromInclude(currentSchema, context.getTraversePath()));
    newVisitorContext.setAnnotationEntriesFromParentSchema(currentAnnotationEntries);
    newVisitorContext.setOutputParentSchema(newSchema);
    context.setVisitorContext(newVisitorContext);
  }

  private void markAnnotationEntryInvalid(AnnotationEntry annotationEntry, OverridePathErrorMsg overridePathErrorMsg)
  {
    annotationEntry.setOverridePathValidStatus(AnnotationEntry.OverridePathValidStatus.INVALID);
    getSchemaVisitorTraversalResult().addMessage(annotationEntry.getPathToAnnotatedTarget(),
                                                 overridePathErrorMsg.toString(),
                                                 annotationEntry.getOverridePathSpecStr());
  }

  private List<AnnotationEntry> generateAnnotationEntryFromInclude(DataSchema dataSchema,
                                                                   ArrayDeque<String> pathToAnnotatedTarget)
  {
    // properties within Record shouldn't be processed, unless this Record has includes and
    // those properties should be overrides.
    if (dataSchema.getType() != DataSchema.Type.RECORD)
    {
      return new ArrayList<>();
    }
    else if (((RecordDataSchema) dataSchema).getInclude().size() == 0)
    {
      if (dataSchema.getProperties().get(getAnnotationNamespace()) != null)
      {
        getSchemaVisitorTraversalResult().addMessage(pathToAnnotatedTarget,
                                                     RECORD_SCHEMA_LEVEL_ANNOTATION_NOT_ALLOWED, getAnnotationNamespace());
        return new ArrayList<>();
      }
    }

    List<AnnotationEntry> overridesForIncludes = constructOverrideAnnotationEntryFromProperties(dataSchema.getProperties(),
                                                                                                AnnotationEntry.AnnotationType.OVERRIDE_RECORD_INCLUDE,
                                                                                                pathToAnnotatedTarget,
                                                                                                dataSchema,
                                                                                                ((RecordDataSchema) dataSchema).getFullName());

    Set<String> includedFieldsNames = ((RecordDataSchema) dataSchema).getInclude()
                                                                      .stream()
                                                                      .map(DataSchema::getDereferencedDataSchema)
                                                                      .flatMap(
                                                                          recordDataSchema -> ((RecordDataSchema) recordDataSchema)
                                                                              .getFields().stream())
                                                                      .map(RecordDataSchema.Field::getName)
                                                                      .collect(Collectors.toSet());
    for (AnnotationEntry annotationEntry : overridesForIncludes)
    {
      if (!(includedFieldsNames.contains(annotationEntry.getRemainingPaths().peekFirst())))
      {
        annotationEntry.setOverridePathValidStatus(AnnotationEntry.OverridePathValidStatus.INVALID);
        getSchemaVisitorTraversalResult().addMessage(annotationEntry.getPathToAnnotatedTarget(),
                                                     OVERRIDE_PATH_ERROR_MSG_ENTRIES_NOT_FOR_INCLUDED,// NOT POINTING TO A INCLUDED SCHEMA!!
                                                     annotationEntry.getOverridePathSpecStr());
      }
    }
    return overridesForIncludes;
  }


  private List<AnnotationEntry> generateAnnotationEntryFromField(RecordDataSchema.Field field,
                                                                 ArrayDeque<String> pathToAnnotatedTarget)
  {
    if (field.getProperties().get(getAnnotationNamespace()) == null)
    {
      return new ArrayList<>();
    }

    if (DataSchemaRichContextTraverser.isLeafSchema(field.getType().getDereferencedDataSchema()))
    {
      return constructNonOverrideAnnotationEntryFromProperties(field.getProperties().get(getAnnotationNamespace()),
                                                               AnnotationEntry.AnnotationType.NON_OVERRIDE_RECORD_FIELD,
                                                               pathToAnnotatedTarget, field);
    }
    else
    {
      // Overrides could only happen if the field's schema could not store resolvedProperties directly
      return constructOverrideAnnotationEntryFromProperties(field.getProperties(),
                                                            AnnotationEntry.AnnotationType.OVERRIDE_RECORD_FIELD,
                                                            pathToAnnotatedTarget,
                                                            field,
                                                            field.getRecord().getFullName());
    }
  }

  private List<AnnotationEntry> generateAnnotationEntryFromTypeRefSchema(TyperefDataSchema dataSchema,
                                                                         ArrayDeque<String> pathToAnnotatedTarget)
  {
    if (dataSchema.getProperties().get(getAnnotationNamespace()) == null)
    {
      return new ArrayList<>();
    }

    List<AnnotationEntry> typeRefAnnotationEntries = new ArrayList<>();

    if (DataSchemaRichContextTraverser.isLeafSchema(dataSchema.getDereferencedDataSchema()))
    {
      typeRefAnnotationEntries.addAll(
          constructNonOverrideAnnotationEntryFromProperties(dataSchema.getProperties().get(getAnnotationNamespace()),
                                                            AnnotationEntry.AnnotationType.NON_OVERRIDE_TYPE_REF, pathToAnnotatedTarget,
                                                            dataSchema));
    }
    else
    {
      // Should treat as overriding
      List<AnnotationEntry>
          annotationEntryToReturn = constructOverrideAnnotationEntryFromProperties(dataSchema.getProperties(),
                                                                                   AnnotationEntry.AnnotationType.OVERRIDE_TYPE_REF_OVERRIDE,
                                                                                   pathToAnnotatedTarget, dataSchema,
                                                                                   dataSchema.getFullName());
      typeRefAnnotationEntries.addAll(annotationEntryToReturn);
      // Need to add this "virtual" matched path for TypeRef
      typeRefAnnotationEntries.forEach(
          annotationEntry -> annotationEntry.getMatchedPaths().add(dataSchema.getFullName()));
    }

    return typeRefAnnotationEntries;
  }

  private List<AnnotationEntry> generateAnnotationEntryFromNamedSchema(DataSchema dataSchema, ArrayDeque<String> pathToAnnotatedTarget)
  {
    if (dataSchema.getProperties().get(getAnnotationNamespace()) == null)
    {
      return new ArrayList<>();
    }

    AnnotationEntry.AnnotationType annotationType;
    switch(dataSchema.getType())
    {
      case FIXED:
        annotationType = AnnotationEntry.AnnotationType.NON_OVERRIDE_FIXED;
        break;
      case ENUM:
        annotationType = AnnotationEntry.AnnotationType.NON_OVERRIDE_ENUM;
        break;
      default:
        return new ArrayList<>();
    }
    return Arrays.asList(new AnnotationEntry("",
                                             dataSchema.getProperties().get(getAnnotationNamespace()), annotationType,
                                             pathToAnnotatedTarget,
                                             dataSchema));
  }

  private List<AnnotationEntry> constructNonOverrideAnnotationEntryFromProperties(Object annotationValue,
                                                                                  AnnotationEntry.AnnotationType annotationType,
                                                                                  ArrayDeque<String> pathToAnnotatedTarget,
                                                                                  Object annotatedTarget)
  {
    // annotationValue has been null-checked, no other checks needed.
    AnnotationEntry
        annotationEntry = new AnnotationEntry("", annotationValue, annotationType, pathToAnnotatedTarget, annotatedTarget);
    return new ArrayList<>(Arrays.asList(annotationEntry));
  }

  @SuppressWarnings("unchecked")
  private List<AnnotationEntry> constructOverrideAnnotationEntryFromProperties(Map<String, Object> schemaProperties,
                                                                               AnnotationEntry.AnnotationType annotationType,
                                                                               ArrayDeque<String> pathToAnnotatedTarget,
                                                                               Object annotatedTarget,
                                                                               String startSchemaName)
  {
    Object properties = schemaProperties.getOrDefault(getAnnotationNamespace(), Collections.emptyMap());
    if (!(properties instanceof Map))
    {
      getSchemaVisitorTraversalResult().addMessage(pathToAnnotatedTarget, OVERRIDE_PATH_ERROR_MSG_ENTRIES_NOT_IN_MAP);
      return new ArrayList<>();
    }

    Map<String, Object> propertiesMap = (Map<String, Object>) properties;
    List<AnnotationEntry> annotationEntryToReturn = new ArrayList<>();

    for (Map.Entry<String, Object> entry: propertiesMap.entrySet())
    {
      if (!PathSpec.validatePathSpecString(entry.getKey()))
      {
        getSchemaVisitorTraversalResult().addMessage(pathToAnnotatedTarget, OVERRIDE_PATH_ERROR_MSG_TEMPLATE_MAL_FORMATTED_KEY, entry.getKey());
      }
      else
      {
        AnnotationEntry annotationEntry = new AnnotationEntry(entry.getKey(),
                                                        entry.getValue(),
                                                        annotationType,
                                                        pathToAnnotatedTarget,
                                                        annotatedTarget);
        // This is override, need to set start schema name for cyclic referencing checking
        annotationEntry.setStartSchemaName(startSchemaName);
        annotationEntryToReturn.add(annotationEntry);
      }
    }
    return annotationEntryToReturn;
  }

  /**
   * This function try to process the current dataSchema being visited inside the context and create a skeleton copy of it.
   * But if the current dataSchema has been already processed, will fetch the cached copy of the skeleton schema.
   *
   * @param context {@link TraverserContext} context that contains current data schema.
   * @param hasOverridesNotResolved a boolean to tell whether there are non-resolved overrides that will be resolved into the new schema
   * @return the new schema
   * @throws CloneNotSupportedException
   */
  private DataSchema createOrReUseSchemaAndAttachToParent(TraverserContext context, boolean hasOverridesNotResolved) throws CloneNotSupportedException
  {
    DataSchema currentDataSchema = context.getCurrentSchema();
    CurrentSchemaEntryMode currentSchemaEntryMode = context.getCurrentSchemaEntryMode();
    // newSchema could be created as skeletonSchema, or fetched from cache if currentDataSchema has already been processed.
    DataSchema newSchema = null;

    if (hasOverridesNotResolved)
    {
      // if there are overrides that not resolved, always build skeleton schema
      newSchema = CopySchemaUtil.buildSkeletonSchema(currentDataSchema);
    }
    else
    {
      if (_seenDataSchemaMapping.containsKey(currentDataSchema))
      {
        newSchema = _seenDataSchemaMapping.get(currentDataSchema);
      }
      else
      {
        newSchema = CopySchemaUtil.buildSkeletonSchema(currentDataSchema);
        _seenDataSchemaMapping.put(currentDataSchema, newSchema);
      }
    }

    // attach based on visitorContext's schema, need to create new fields or union members
    PathSpecTraverseVisitorContext oldVisitorContext = (PathSpecTraverseVisitorContext) (context.getVisitorContext());
    DataSchema outputParentSchema = oldVisitorContext.getOutputParentSchema();

    if (outputParentSchema == null)
    {
      _schemaConstructed = newSchema;
      return newSchema;
    }

    switch (currentSchemaEntryMode)
    {
      case FIELD:
        assert (outputParentSchema.getType() == DataSchema.Type.RECORD);
        addField(context.getEnclosingField(), newSchema, (RecordDataSchema) outputParentSchema);
        break;
      case MAP_KEY:
        assert (outputParentSchema.getType() == DataSchema.Type.MAP);
        MapDataSchema mapDataSchema = (MapDataSchema) outputParentSchema;
        mapDataSchema.setKey((StringDataSchema) newSchema);
        break;
      case MAP_VALUE:
        assert (outputParentSchema.getType() == DataSchema.Type.MAP);
        mapDataSchema = (MapDataSchema) outputParentSchema;
        mapDataSchema.setValues(newSchema);
        break;
      case ARRAY_VALUE:
        assert (outputParentSchema.getType() == DataSchema.Type.ARRAY);
        ArrayDataSchema arrayDataSchema = (ArrayDataSchema) outputParentSchema;
        arrayDataSchema.setItems(newSchema);
        break;
      case UNION_MEMBER:
        assert (outputParentSchema.getType() == DataSchema.Type.UNION);
        addUnionMember(context.getEnclosingUnionMember(), newSchema, (UnionDataSchema) outputParentSchema);
        break;
      case TYPEREF_REF:
        TyperefDataSchema typerefDataSchema = (TyperefDataSchema) outputParentSchema;
        typerefDataSchema.setReferencedType(newSchema);
        break;
      default:
        break;
    }
    return newSchema;
  }

  static void addField(RecordDataSchema.Field origField, DataSchema updatedFieldSchema, RecordDataSchema enclosingSchema)
  {
    RecordDataSchema.Field newField = CopySchemaUtil.copyField(origField, updatedFieldSchema);
    newField.setRecord(enclosingSchema);
    List<RecordDataSchema.Field> fields = new ArrayList<>(enclosingSchema.getFields());
    fields.add(newField);
    enclosingSchema.setFields(fields, new StringBuilder());
  }

  static void addUnionMember(UnionDataSchema.Member origMember, DataSchema updatedMemberSchema, UnionDataSchema enclosingSchema)
  {
    UnionDataSchema.Member newUnionMember = CopySchemaUtil.copyUnionMember(origMember, updatedMemberSchema);
    List<UnionDataSchema.Member> unionMembers = new ArrayList<>(enclosingSchema.getMembers());
    unionMembers.add(newUnionMember);
    enclosingSchema.setMembers(unionMembers, new StringBuilder());
  }

  /**
   * This function will use {@link SchemaAnnotationHandler#resolve(List, SchemaAnnotationHandler.ResolutionMetaData)}
   * @param propertiesOverrides {@link AnnotationEntry} list which contain overrides
   * @param pathSpecComponents components list of current pathSpec to the location where this resolution happens
   * @return a map whose key is the annotationNamespace and value be the resolved property object.
   */
  private Map<String, Object> resolveAnnotationEntries(List<AnnotationEntry> propertiesOverrides, ArrayDeque<String> pathSpecComponents)
  {
    List<Pair<String, Object>> propertiesOverridesPairs = propertiesOverrides.stream()
                                                                             .map(annotationEntry -> new ImmutablePair<>(
                                                                                 annotationEntry.getOverridePathSpecStr(),
                                                                                 annotationEntry.getAnnotationValue()))
                                                                             .collect(toList());
    SchemaAnnotationHandler.ResolutionResult result =
        _handler.resolve(propertiesOverridesPairs, new SchemaAnnotationHandler.ResolutionMetaData());
    if (result.isError())
    {
      getSchemaVisitorTraversalResult().addMessage(pathSpecComponents,
                                                   "Annotations override resolution failed in handlers for %s",
                                                   getAnnotationNamespace());
      getSchemaVisitorTraversalResult().addMessages(pathSpecComponents, result.getMessages());
    }
    return result.getResolvedResult();
  }

  private String getAnnotationNamespace()
  {
    return _handler.getAnnotationNamespace();
  }

  /**
   * An implementation of {@link VisitorContext}
   * Will be passed to this {@link PathSpecBasedSchemaAnnotationVisitor} from {@link DataSchemaRichContextTraverser}
   * through {@link TraverserContext}
   *
   */
  static class PathSpecTraverseVisitorContext implements VisitorContext
  {
    List<AnnotationEntry> getAnnotationEntriesFromParentSchema()
    {
      return _annotationEntriesFromParentSchema;
    }

    void setAnnotationEntriesFromParentSchema(List<AnnotationEntry> annotationEntriesFromParentSchema)
    {
      _annotationEntriesFromParentSchema = annotationEntriesFromParentSchema;
    }

    public DataSchema getOutputParentSchema()
    {
      return _outputParentSchema;
    }

    public void setOutputParentSchema(DataSchema outputParentSchema)
    {
      _outputParentSchema = outputParentSchema;
    }

    /**
     * Stores unresolved {@link AnnotationEntry} from last layer recursion.
     */
    private List<AnnotationEntry> _annotationEntriesFromParentSchema = new ArrayList<>();
    /**
     * This is pointer to the the actual last visited data schema that {@link PathSpecBasedSchemaAnnotationVisitor}
     * built as part of {@link #_schemaConstructed} within this visitorContext.
     *
     */
    private DataSchema _outputParentSchema = null;
  }
}
