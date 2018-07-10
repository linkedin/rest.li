/*
   Copyright (c) 2012 LinkedIn Corp.

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

package com.linkedin.restli.docgen;


import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.server.RestLiInternalException;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.internal.server.model.ResourceModelEncoder;
import com.linkedin.restli.internal.server.model.ResourceModelEncoder.NullDocsProvider;
import com.linkedin.restli.restspec.ActionSchema;
import com.linkedin.restli.restspec.ActionSchemaArray;
import com.linkedin.restli.restspec.ActionsSetSchema;
import com.linkedin.restli.restspec.AssociationSchema;
import com.linkedin.restli.restspec.CollectionSchema;
import com.linkedin.restli.restspec.EntitySchema;
import com.linkedin.restli.restspec.FinderSchema;
import com.linkedin.restli.restspec.FinderSchemaArray;
import com.linkedin.restli.restspec.ParameterSchema;
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.restli.restspec.RestMethodSchema;
import com.linkedin.restli.restspec.RestMethodSchemaArray;
import com.linkedin.restli.restspec.SimpleSchema;
import com.linkedin.restli.restspec.RestSpecCodec;
import com.linkedin.restli.server.ResourceLevel;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;


/**
 * A collection of ResourceSchema, supporting visitor-style iteration. Each ResourceSchema
 * (and sub-resource) is identified by a dot-delimited path e.g. "groups" or "groups.contacts"
 *
 * @author dellamag
 */
public class ResourceSchemaCollection
{
  /**
   * For each given {@link ResourceModel}, the classpath is checked for a .restspec.json
   * matching the name of the {@link ResourceModel},  if found it is loaded.  If a .restspec.json file
   * is not found, one is created {@link ResourceSchemaCollection} from specified root {@link ResourceModel}.
   * All resources will be recursively traversed to discover subresources.
   * Root resources not specified are excluded.
   *
   * @param rootResources root resources in ResourceModel type
   * @return constructed ResourceSchemaCollection
   */
  public static ResourceSchemaCollection loadOrCreateResourceSchema(Map<String, ResourceModel> rootResources)
  {
    final ResourceModelEncoder encoder = new ResourceModelEncoder(new NullDocsProvider());
    final Map<String, ResourceSchema> schemaMap = new TreeMap<String, ResourceSchema>();
    for (ResourceModel resource : rootResources.values())
    {
      schemaMap.put(resource.getName(), encoder.loadOrBuildResourceSchema(resource));
    }

    return new ResourceSchemaCollection(schemaMap);
  }

  /**
   * Create {@link ResourceSchemaCollection} from idl files.
   *
   * @param restspecSearchPaths file system paths to search for idl files
   * @return constructed ResourceSchemaCollection
   */
  public static ResourceSchemaCollection createFromIdls(String[] restspecSearchPaths)
  {
    final RestSpecCodec codec = new RestSpecCodec();
    final Map<String, ResourceSchema> resourceSchemaMap = new HashMap<String, ResourceSchema>();

    for (String path : restspecSearchPaths)
    {
      final File dir = new File(path);
      if (! dir.isDirectory())
      {
        throw new IllegalArgumentException(String.format("path '%s' is not a directory", dir.getAbsolutePath()));
      }
      final File[] idlFiles = dir.listFiles(new FileFilter() {
        @Override
        public boolean accept(File pathname)
        {
          return pathname.getName().endsWith(RestConstants.RESOURCE_MODEL_FILENAME_EXTENSION);
        }
      });

      for (File idlFile : idlFiles)
      {
        try
        {
          final FileInputStream is = new FileInputStream(idlFile);
          final ResourceSchema resourceSchema = codec.readResourceSchema(is);
          resourceSchemaMap.put(resourceSchema.getName(), resourceSchema);
        }
        catch (IOException e)
        {
          throw new RestLiInternalException(String.format("Error loading restspec IDL file '%s'", idlFile.getName()), e);
        }
      }
    }

    return new ResourceSchemaCollection(resourceSchemaMap);
  }

  /**
   * @param visitor {@link ResourceSchemaVisitior} to visit all resource schemas with the specified visitor
   */
  public static void visitResources(Collection<ResourceSchema> resources, ResourceSchemaVisitior visitor)
  {
//    List<ResourceSchema> reverseList = new ArrayList<>(resources);
//    Collections.reverse(reverseList);
//    for (ResourceSchema schema : reverseList)
//    {
//      processResourceSchema(visitor, new ArrayList<ResourceSchema>(), schema);
//    }


//    for (ResourceSchema schema : resources)
//    {
//      processResourceSchema(visitor, new ArrayList<ResourceSchema>(), schema);
//    }

    ExecutorService executorService = Executors.newCachedThreadPool();
    List<ProcessTask> tasks = new ArrayList<>();
    for (ResourceSchema schema : resources) {
      tasks.add(new ProcessTask(visitor, new ArrayList<>(), schema));
    }

    try {
      executorService.invokeAll(tasks);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    executorService.shutdown();

//    ForkJoinPool pool = new ForkJoinPool(1);
//    List<ResourceSchema> resourceSchemas = new ArrayList<>(resources);
//    pool.invoke(new ForkJoinProcessTask(resourceSchemas, visitor));
//    pool.shutdown();
  }

  /**
   * Store the specified root resources plus the discover subresources
   * @param rootResources root resources in {@link ResourceSchema} type
   */
  public ResourceSchemaCollection(Map<String, ResourceSchema> rootResources)
  {
    _allResources = new TreeMap<String, ResourceSchema>(rootResources);
    _subResources = new IdentityHashMap<ResourceSchema, List<ResourceSchema>>();
    _parentResources = new IdentityHashMap<ResourceSchema, List<ResourceSchema>>();
    final Map<String, ResourceSchema> flattenSubResources = new TreeMap<>();

    final ResourceSchemaVisitior visitor = new BaseResourceSchemaVisitor()
    {
      @Override
      public void visitResourceSchema(VisitContext context,
                                      ResourceSchema resourceSchema)
      {
        final String qualifiedResourceName = context.getResourcePath();
        if (!_allResources.containsKey(qualifiedResourceName))
        {
          synchronized (this) {
            flattenSubResources.put(qualifiedResourceName, resourceSchema);
          }

          final List<ResourceSchema> hierarchy = context.getResourceSchemaHierarchy();

          ArrayList<ResourceSchema> parents = new ArrayList<ResourceSchema>(hierarchy);
          parents.remove(parents.size()-1);
          synchronized (this) {
            _parentResources.put(resourceSchema, parents);

            final ResourceSchema directParent = parents.get(parents.size() - 1);
            List<ResourceSchema> subList = _subResources.get(directParent);
            if (subList == null) {
              subList = new ArrayList<>();
              _subResources.put(directParent, subList);
            }

            subList.add(resourceSchema);
          }

        }
      }
    };

    visitResources(_allResources.values(), visitor);
    _allResources.putAll(flattenSubResources);
  }

  /**
   * Retrieve the resource schema for the specified path.
   *
   * @param resourcePath for root resources, the path is the name of the resource;
   *                     for subresource, the path is the fully-qualitied resource name, delimited with "."
   * @return schema of the resource
   */
  public ResourceSchema getResource(String resourcePath)
  {
    return _allResources.get(resourcePath);
  }

  /**
   * @return map from the resource path to both root resources and all discovered subresources
   */
  public Map<String, ResourceSchema> getResources()
  {
    return _allResources;
  }

  /**
   * @param parentSchema a parent resource schema
   * @return schema of direct subresources of the specified resource
   */
  public List<ResourceSchema> getSubResources(ResourceSchema parentSchema)
  {
    return _subResources.get(parentSchema);
  }

  public List<ResourceSchema> getParentResources(ResourceSchema parentSchema)
  {
    List<ResourceSchema> parents = _parentResources.get(parentSchema);
    if(parents == null)
    {
      return Collections.emptyList();
    }
    else
    {
      return parents;
    }
  }

  /**
   * @param ancestorSchema a root resource schema
   * @return schema of all nested subresources that are the descendants of the specified resource
   */
  public List<ResourceSchema> getAllSubResources(ResourceSchema ancestorSchema)
  {
    return getAllSubResourcesRecursive(ancestorSchema, new ArrayList<ResourceSchema>());
  }

  private List<ResourceSchema> getAllSubResourcesRecursive(ResourceSchema parentSchema,
                                                           List<ResourceSchema> accumulator)
  {
    final List<ResourceSchema> subResources = getSubResources(parentSchema);
    if (subResources == null)
    {
      return null;
    }

    accumulator.addAll(subResources);
    for (ResourceSchema sub : subResources)
    {
      getAllSubResourcesRecursive(sub, accumulator);
    }

    return accumulator;
  }

  private static void processResourceSchema(ResourceSchemaVisitior visitor,
                                            List<ResourceSchema> hierarchy,
                                            ResourceSchema resourceSchema)
  {
    hierarchy.add(resourceSchema);

    final ResourceSchemaVisitior.VisitContext context = buildContext(hierarchy);
    visitor.visitResourceSchema(context, resourceSchema);

    if (resourceSchema.hasCollection())
    {
      final CollectionSchema collectionSchema = resourceSchema.getCollection();
      visitor.visitCollectionResource(context, collectionSchema);

      processRestMethods(visitor, context, collectionSchema, collectionSchema.getMethods());
      processFinders(visitor, context, collectionSchema, collectionSchema.getFinders());
      processActions(visitor, context, collectionSchema, collectionSchema.getActions());

      processEntitySchema(visitor, context, collectionSchema.getEntity());
    }
    else if (resourceSchema.hasAssociation())
    {
      final AssociationSchema associationSchema = resourceSchema.getAssociation();
      visitor.visitAssociationResource(context, associationSchema);

      processRestMethods(visitor, context, associationSchema, associationSchema.getMethods());
      processFinders(visitor, context, associationSchema, associationSchema.getFinders());
      processActions(visitor, context, associationSchema, associationSchema.getActions());

      processEntitySchema(visitor, context, associationSchema.getEntity());
    }
    else if (resourceSchema.hasSimple())
    {
      final SimpleSchema simpleSchema = resourceSchema.getSimple();
      visitor.visitSimpleResource(context, simpleSchema);

      processRestMethods(visitor, context, simpleSchema, simpleSchema.getMethods());
      processActions(visitor, context, simpleSchema, simpleSchema.getActions());

      processEntitySchema(visitor, context, simpleSchema.getEntity());
    }
    else if (resourceSchema.hasActionsSet())
    {
      final ActionsSetSchema actionsSet = resourceSchema.getActionsSet();
      visitor.visitActionSetResource(context, actionsSet);

      processActions(visitor, context, actionsSet, actionsSet.getActions());
    }

    hierarchy.remove(hierarchy.size() - 1);
  }

  private static class ProcessTask implements Callable<Object> {
    private ResourceSchemaVisitior _visitor;
    private List<ResourceSchema> _hierarchy;
    private ResourceSchema _resourceSchem;

    private ProcessTask(ResourceSchemaVisitior visitor, List<ResourceSchema> hierarchy, ResourceSchema resourceSchema) {
      _visitor = visitor;
      _hierarchy = hierarchy;
      _resourceSchem = resourceSchema;
    }

    @Override
    public Object call() {
      processResourceSchema(_visitor, _hierarchy, _resourceSchem);
      return null;
    }

  }

  private static class ForkJoinProcessTask extends RecursiveAction {
    private static final int _SIZE = 4;
    private List<ResourceSchema> _schemas;
    private ResourceSchemaVisitior _visitor;
    private static final long serialVersionUID = 100003;

    private ForkJoinProcessTask(List<ResourceSchema> schemas, ResourceSchemaVisitior visitor) {
      _schemas = schemas;
      _visitor = visitor;
    }

    @Override
    protected void compute() {
      if (_schemas.size() <= _SIZE) {
        for (ResourceSchema schema : _schemas) {
          processResourceSchema(_visitor, new ArrayList<>(), schema);
        }

      } else {
        int delimiter = _schemas.size() / 4;
        List<ResourceSchema> subList1 = _schemas.subList(0, delimiter);
        List<ResourceSchema> subList2 = _schemas.subList(delimiter, delimiter * 2);
        List<ResourceSchema> subList3 = _schemas.subList(delimiter * 2, delimiter * 3);
        List<ResourceSchema> subList4 = _schemas.subList(delimiter * 3, _schemas.size());

        ForkJoinProcessTask subTask1 = new ForkJoinProcessTask(subList1, _visitor);
        ForkJoinProcessTask subTask2 = new ForkJoinProcessTask(subList2, _visitor);
        ForkJoinProcessTask subTask3 = new ForkJoinProcessTask(subList3, _visitor);
        ForkJoinProcessTask subTask4 = new ForkJoinProcessTask(subList4, _visitor);

        invokeAll(subTask1, subTask2, subTask3, subTask4);
      }

    }

  }


  private static void processEntitySchema(ResourceSchemaVisitior visitor,
                                          ResourceSchemaVisitior.VisitContext context,
                                          EntitySchema entitySchema)
  {
    visitor.visitEntityResource(context, entitySchema);

    processActions(visitor, context, entitySchema, entitySchema.getActions());

    if (entitySchema.hasSubresources())
    {
      for (ResourceSchema resourceSchema : entitySchema.getSubresources())
      {
        processResourceSchema(visitor, context.getResourceSchemaHierarchy(), resourceSchema);
      }
    }
  }

  private static void processRestMethods(ResourceSchemaVisitior visitor,
                                         ResourceSchemaVisitior.VisitContext context,
                                         RecordTemplate containingResourceType,
                                         RestMethodSchemaArray methods)
  {
    if (methods != null)
    {
      for (RestMethodSchema restMethodSchema : methods)
      {
        visitor.visitRestMethod(context, containingResourceType, restMethodSchema);

        if (restMethodSchema.hasParameters())
        {
          for (ParameterSchema parameterSchema : restMethodSchema.getParameters())
          {
            visitor.visitParameter(context,
                                   containingResourceType,
                                   restMethodSchema,
                                   parameterSchema);
          }
        }
      }
    }
  }

  private static void processFinders(ResourceSchemaVisitior visitor,
                                     ResourceSchemaVisitior.VisitContext context,
                                     RecordTemplate containingResourceType,
                                     FinderSchemaArray finders)
  {
    if (finders != null)
    {
      for (FinderSchema finderSchema : finders)
      {
        visitor.visitFinder(context, containingResourceType, finderSchema);

        if (finderSchema.hasParameters())
        {
          for (ParameterSchema parameterSchema : finderSchema.getParameters())
          {
            visitor.visitParameter(context,
                                   containingResourceType,
                                   finderSchema,
                                   parameterSchema);
          }
        }
      }
    }
  }


  private static void processActions(ResourceSchemaVisitior visitor,
                                     ResourceSchemaVisitior.VisitContext context,
                                     RecordTemplate containingResourceType,
                                     ActionSchemaArray actions)
  {
    if (actions != null)
    {
      final ResourceLevel resourceLevel = ((EntitySchema.class.equals(containingResourceType.getClass()) ||
                                           SimpleSchema.class.equals(containingResourceType.getClass())) ?
                                           ResourceLevel.ENTITY :
                                           ResourceLevel.COLLECTION);

      for (ActionSchema actionSchema : actions)
      {
        visitor.visitAction(context, containingResourceType, resourceLevel, actionSchema);

        if (actionSchema.hasParameters())
        {
          for (ParameterSchema parameterSchema : actionSchema.getParameters())
          {
            visitor.visitParameter(context,
                                   containingResourceType,
                                   actionSchema,
                                   parameterSchema);
          }
        }
      }
    }
  }

  private static ResourceSchemaVisitior.VisitContext buildContext(List<ResourceSchema> hierarchy)
  {
    final StringBuilder resourcePath = new StringBuilder();
    for (ResourceSchema resourceSchema : hierarchy)
    {
      resourcePath.append(resourceSchema.getName()).append(".");
    }
    resourcePath.deleteCharAt(resourcePath.length() - 1);

    return new ResourceSchemaVisitior.VisitContext(hierarchy, resourcePath.toString());
  }

  private final Map<String, ResourceSchema> _allResources;
  private final Map<ResourceSchema, List<ResourceSchema>> _subResources;
  private final Map<ResourceSchema, List<ResourceSchema>> _parentResources;
}
