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

package com.linkedin.restli.internal.server.model;


import com.linkedin.restli.common.Graph;
import com.linkedin.restli.common.Node;
import com.linkedin.restli.server.ResourceConfigException;
import com.linkedin.restli.server.RestLiConfig;

import com.linkedin.restli.server.annotations.RestAnnotations;
import com.linkedin.restli.server.annotations.RestLiAssociation;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestLiSimpleResource;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author dellamag
 */
public class RestLiApiBuilder implements RestApiBuilder
{
  private final static Logger _log = LoggerFactory.getLogger(RestLiApiBuilder.class);

  private final Set<String> _packageNames;
  private final Set<String> _classNames;
  private final boolean _isParallelBuild;

  public RestLiApiBuilder(final RestLiConfig config)
  {
    if (config.getResourcePackageNamesSet().isEmpty() && config.getResourceClassNamesSet().isEmpty())
    {
      throw new ResourceConfigException("At least one package containing Rest.li annotated classes or one resource class must be specified");
    }

    _packageNames = config.getResourcePackageNamesSet();
    _classNames = config.getResourceClassNamesSet();
    _isParallelBuild = config.getParallelBuildChoice();
  }

  @Override
  public Map<String, ResourceModel> build()
  {
    RestLiClasspathScanner scanner =
        new RestLiClasspathScanner(_packageNames, _classNames, Thread.currentThread().getContextClassLoader(), _isParallelBuild);
    scanner.scanPackages();
    final String errorMessage = scanner.scanClasses();
    if (!errorMessage.isEmpty())
    {
      _log.error(errorMessage);
    }

    Set<Class<?>> annotatedClasses = scanner.getMatchedClasses();
    if (annotatedClasses.isEmpty())
    {
      _log.info("Could not find any Rest.li annotated class in the configuration");
      return Collections.emptyMap();
    }

    if (_isParallelBuild)
    {
      return parallelBuildResourceModels(annotatedClasses);
    }
    else
    {
      return buildResourceModels(annotatedClasses);
    }

  }

  private static Class<?> getParentResourceClass(Class<?> resourceClass)
  {
    for (Annotation a : resourceClass.getAnnotations())
    {
      if (a instanceof RestLiAssociation)
      {
        return ((RestLiAssociation)a).parent();
      }
      else if (a instanceof RestLiCollection)
      {
        return ((RestLiCollection)a).parent();
      }
      else if (a instanceof RestLiSimpleResource)
      {
        return ((RestLiSimpleResource)a).parent();
      }
    }

    return RestAnnotations.ROOT.class;
  }

  private static void processResourceInOrder(Class<?> annotatedClass, Map<Class<?>, ResourceModel> resourceModels, Map<String, ResourceModel> rootResourceModels)
  {
    if (resourceModels.containsKey(annotatedClass))
    {
      return;
    }

    Class<?> parentClass = getParentResourceClass(annotatedClass);

    // If we need to create the parent class, do it before the child class. Recurse, in case of grandparents.
    if (parentClass != RestAnnotations.ROOT.class)
    {
      processResourceInOrder(parentClass, resourceModels, rootResourceModels);
    }

    ResourceModel model = RestLiAnnotationReader.processResource(annotatedClass, resourceModels.get(parentClass));

    if (model.isRoot())
    {
      String path = "/" + model.getName();
      final ResourceModel existingResource = rootResourceModels.get(path);
      if (existingResource != null)
      {
        String errorMessage = String.format("Resource classes \"%s\" and \"%s\" clash on the resource name \"%s\".",
            existingResource.getResourceClass().getCanonicalName(),
            model.getResourceClass().getCanonicalName(),
            existingResource.getName());
        throw new ResourceConfigException(errorMessage);
      }
      rootResourceModels.put(path, model);
    }

    resourceModels.put(annotatedClass, model);
  }

  private static void asyncProcessResourceTask(Class<?> annotatedClass, Map<Class<?>, ResourceModel> resourceModels, Map<String, ResourceModel> rootResourceModels)
  {
    if (resourceModels.containsKey(annotatedClass))
    {
      return;
    }

    Class<?> parentClass = getParentResourceClass(annotatedClass);
    ResourceModel model = RestLiAnnotationReader.processResource(annotatedClass, resourceModels.get(parentClass));
    if (model.isRoot())
    {
      String path = "/" + model.getName();
      synchronized (RestLiApiBuilder.class)
      {
        final ResourceModel existingResource = rootResourceModels.get(path);
        if (existingResource != null)
        {
          String errorMessage = String.format("Resource classes \"%s\" and \"%s\" clash on the resource name \"%s\".",
                                              existingResource.getResourceClass().getCanonicalName(),
                                              model.getResourceClass().getCanonicalName(),
                                              existingResource.getName());
          throw new ResourceConfigException(errorMessage);
        }

        rootResourceModels.put(path, model);
      }

    }

    resourceModels.put(annotatedClass, model);
  }

  public static Map<String, ResourceModel> buildResourceModels(
      final Set<Class<?>> restliAnnotatedClasses)
  {
    Map<String, ResourceModel> rootResourceModels = new HashMap<String, ResourceModel>();
    Map<Class<?>, ResourceModel> resourceModels = new HashMap<Class<?>, ResourceModel>();

    for (Class<?> annotatedClass : restliAnnotatedClasses)
    {
      processResourceInOrder(annotatedClass, resourceModels, rootResourceModels);
    }

    return rootResourceModels;
  }

  private static Map<String, ResourceModel> parallelBuildResourceModels(
      final Set<Class<?>> restliAnnotatedClasses)
  {
    Map<String, ResourceModel> rootResourceModels = new HashMap<>();
    Map<Class<?>, ResourceModel> resourceModels = new ConcurrentHashMap<>();
    Map<Class<?>, ProcessTask> rootClazzes = new HashMap<>();
    Graph clazzMap = new Graph();
    ExecutorService executorService = Executors.newWorkStealingPool();

    for (Class<?> clazz : restliAnnotatedClasses)
    {
      clazzMap.get(clazz);
      Class<?> parentClass = getParentResourceClass(clazz);
      while (parentClass != RestAnnotations.ROOT.class)
      {
        Node<Class<?>> subClazzGraph = clazzMap.get(clazz);
        Node<Class<?>> parentClazzGraph = clazzMap.get(parentClass);

        parentClazzGraph.addAdjacentNode(subClazzGraph);
        clazz = parentClass;
        parentClass = getParentResourceClass(clazz);
      }

      if (parentClass == RestAnnotations.ROOT.class)
      {
        rootClazzes.put(clazz, new ProcessTask(executorService, clazzMap.get(clazz), rootResourceModels, resourceModels));
      }

    }

    List<Future<Object>> res = null;
    try
    {
      res = executorService.invokeAll(rootClazzes.values());
    }
    catch (InterruptedException e)
    {
      _log.error(e.getMessage());
    }

    try
    {
      if (res != null && !res.isEmpty())
      {
        for (Future<Object> future : res)
        {
          future.get();
        }

      }

      return rootResourceModels;
    }
    catch (InterruptedException e)
    {
      _log.error(e.getMessage());

      //let it failed
      throw new RuntimeException(e.getMessage());
    }
    catch (ExecutionException e)
    {
      if (e.getCause() instanceof ResourceConfigException)
      {
        throw (ResourceConfigException) e.getCause();
      }
      else
      {
        _log.error(e.getMessage());
        //let it failed
        throw new RuntimeException(e.getMessage());
      }

    }
    finally {
      executorService.shutdown();
    }

  }

  private static class ProcessTask implements Callable<Object>
  {
    private Map<String, ResourceModel> _rootResourceModels;
    private Map<Class<?>, ResourceModel> _resourceModels;
    private ExecutorService _executorService;
    private Node<Class<?>> _clazz;

    private ProcessTask(ExecutorService executorService, Node<Class<?>> clazz,
        Map<String, ResourceModel> rootResourceModels, Map<Class<?>, ResourceModel> resourceModels)
    {
      _executorService = executorService;
      _rootResourceModels = rootResourceModels;
      _resourceModels = resourceModels;
      _clazz = clazz;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object call() throws InterruptedException
    {
      asyncProcessResourceTask((Class<?>) _clazz.getObject(), _resourceModels, _rootResourceModels);
      List<ProcessTask> subTasks = new ArrayList<>();
      for (Node<?> clazz : _clazz.getAdjacency())
      {
        subTasks.add(new ProcessTask(_executorService, (Node<Class<?>>) clazz, _rootResourceModels, _resourceModels));
      }

      if (!subTasks.isEmpty())
      {
        _executorService.invokeAll(subTasks);
      }

      return null;
    }

  }

}
