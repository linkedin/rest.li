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
package com.linkedin.pegasus.gradle;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.Sync;
import org.gradle.testfixtures.ProjectBuilder;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.*;


/**
 * @author Keren Jin
 */
public final class TestPegasusPlugin
{
  @Test
  public void test()
  {
    Project project = ProjectBuilder.builder().build();
    project.getPlugins().apply(PegasusPlugin.class);

    assertTrue(project.getPlugins().hasPlugin(JavaPlugin.class));

    // if any configuration is resolved in configuration phase, user script that tries to exclude certain dependencies will fail
    for (Configuration configuration : project.getConfigurations())
    {
      assertSame(configuration.getState(), Configuration.State.UNRESOLVED);
    }

    assertNotNull(project.getConfigurations().findByName("dataTemplate"));
    assertNotNull(project.getConfigurations().findByName("restClient"));

    assertTrue(project.getExtensions().getExtraProperties().get("PegasusGenerationMode") instanceof Map);

    @SuppressWarnings("unchecked")
    Map<String, PegasusOptions> pegasusOptions = (Map<String, PegasusOptions>) project
        .getExtensions().getExtraProperties().get("pegasus");

    assertFalse(pegasusOptions.get("main").hasGenerationMode(PegasusOptions.GenerationMode.AVRO));
    assertTrue(pegasusOptions.get("main").hasGenerationMode(PegasusOptions.GenerationMode.PEGASUS));
  }

  @Test
  public void testTaskTypes() {
    // Given/When: Pegasus Plugin is applied to a project.
    Project project = ProjectBuilder.builder().build();
    project.getPlugins().apply(PegasusPlugin.class);

    // Then: Validate the Delete/Sync Schema tasks are of the correct type.
    assertTrue(project.getTasks().getByName("mainDestroyStaleFiles") instanceof Delete);
    assertTrue(project.getTasks().getByName("mainCopySchemas") instanceof Sync);
  }
}
