/**
 *  Copyright (c) 2019 LinkedIn Corp.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.linkedin.pegasus.gradle;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.SourceSet;


public class PegasusPluginLoader implements Plugin<Project>
{
  @Override
  public void apply(Project project)
  {
    PegasusPlugin plugin = new PegasusPlugin();
    plugin.setPluginType(getClass());
    plugin.apply(project);

    Class<? extends Task> generateRestClientTaskClass;
    try
    {
      generateRestClientTaskClass = (Class<? extends Task>) Class
          .forName("com.linkedin.pegasus.gradle.tasks.GenerateRestClientTask");
    }
    catch (ClassNotFoundException e)
    {
      throw new GradleException("Could not load GenerateRestClientTask class.");
    }

    project.afterEvaluate(proj -> proj.getTasks().withType(generateRestClientTaskClass, task -> {
      Method method;
      try
      {
        method = generateRestClientTaskClass
            .getDeclaredMethod("setRestli1BuildersDeprecated", boolean.class);
      }
      catch (NoSuchMethodException e)
      {
        throw new GradleException("Could not find method setRestli1BuildersDeprecated.");
      }

      try
      {
        method.invoke(task, false);
      }
      catch (IllegalAccessException | InvocationTargetException e)
      {
        throw new GradleException("Could not invoke method setRestli1BuildersDeprecated.");
      }
    }));
  }

  /**
   * Since PegasusPlugin is loaded with reflection, any method/variable in PegasusPlugin must be
   * exported by this loader to be accessible to external gradle files.
   *
   * This method is needed by restli-int-test-server/build.gradle
   */
  public static String getGeneratedDirPath(Project project, SourceSet sourceSet, String genType)
  {
    return PegasusPlugin.getGeneratedDirPath(project, sourceSet, genType);
  }
}
