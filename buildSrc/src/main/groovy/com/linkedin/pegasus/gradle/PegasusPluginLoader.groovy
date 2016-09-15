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

package com.linkedin.pegasus.gradle


import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet

class PegasusPluginLoader implements Plugin<Project>
{
  @Override
  void apply(Project project)
  {
    final PegasusPlugin plugin = new PegasusPlugin()
    plugin.setPluginType(this.class)
    plugin.apply(project)

    project.afterEvaluate { proj ->
      proj.tasks.withType(Class.forName('com.linkedin.pegasus.gradle.tasks.GenerateRestClientTask')) { task -> task._isRestli1BuildersDeprecated = false
      }
    }
  }

  /*
  Since PegasusPlugin is loaded with reflection, any method/variable in PegasusPlugin must be
  exported by this loader to be accessible to external gradle files.

  This method is needed by restli-int-test-server/build.gradle
 */

  public static String getGeneratedDirPath(Project project, SourceSet sourceSet, String genType)
  {
    PegasusPlugin.getGeneratedDirPath(project, sourceSet, genType)
  }
}
