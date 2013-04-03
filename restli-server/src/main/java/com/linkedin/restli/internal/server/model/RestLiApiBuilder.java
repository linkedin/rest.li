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


import com.linkedin.restli.server.ResourceConfigException;
import com.linkedin.restli.server.RestLiConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
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

  public RestLiApiBuilder(final RestLiConfig config)
  {
    if (config.getResourcePackageNamesSet().isEmpty() && config.getResourceClassNamesSet().isEmpty())
    {
      throw new ResourceConfigException("At least one package containing rest-annotated classes or one resource class must be specified");
    }

    _packageNames = config.getResourcePackageNamesSet();
    _classNames = config.getResourceClassNamesSet();
  }

  @Override
  public Map<String, ResourceModel> build()
  {
    RestLiClasspathScanner scanner =
        new RestLiClasspathScanner(_packageNames, _classNames, Thread.currentThread().getContextClassLoader());
    scanner.scanPackages();
    final String errorMessage = scanner.scanClasses();
    if (!errorMessage.isEmpty())
    {
      _log.error(errorMessage);
    }

    Set<Class<?>> annotatedClasses = scanner.getMatchedClasses();

    if (annotatedClasses.isEmpty())
    {
      StringBuilder sb =
          new StringBuilder("Could not find any rest-li classes in the following packages: "
              + StringUtils.join(_packageNames, ','));
      throw new ResourceConfigException(sb.toString());
    }

    return buildResourceModels(annotatedClasses);
  }

  public static Map<String, ResourceModel> buildResourceModels(
          final Set<Class<?>> restliAnnotatedClasses)
  {
    Map<Class<?>, ResourceModel> resourceModels = new HashMap<Class<?>, ResourceModel>();

    for (Class<?> annotatedClass : restliAnnotatedClasses)
    {
      ResourceModel resourceModel = RestLiAnnotationReader.processResource(annotatedClass);
      resourceModels.put(annotatedClass, resourceModel);
    }

    Map<String, ResourceModel> rootResourceModels = new HashMap<String, ResourceModel>();

    for (Class<?> annotatedClass : restliAnnotatedClasses)
    {
      ResourceModel resourceModel = resourceModels.get(annotatedClass);
      if (resourceModel.isRoot())
      {
        String path = "/" + resourceModel.getName();
        final ResourceModel existingResource = rootResourceModels.get(path);
        if (existingResource != null)
        {
          _log.warn(String.format("Resource classes \"%s\" and \"%s\" clash on the resource name \"%s\".",
                                  existingResource.getResourceClass().getCanonicalName(),
                                  resourceModel.getResourceClass().getCanonicalName(),
                                  existingResource.getName()));
        }
        rootResourceModels.put(path, resourceModel);
      }
      else
      {
        ResourceModel parentModel = resourceModels.get(resourceModel.getParentResourceClass());
        if (parentModel == null)
        {
          throw new ResourceConfigException("Could not find model for parent class'"
              + resourceModel.getParentResourceClass().getName() + "'for: "
              + resourceModel.getName());
        }
        resourceModel.setParentResourceModel(parentModel);
        parentModel.addSubResource(resourceModel.getName(), resourceModel);
      }
    }

    return rootResourceModels;
  }

}
