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

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.testng.Assert
import org.testng.annotations.Test


/**
 * @author Keren Jin
 */
class TestPegasusPlugin
{
  @Test
  public void test()
  {
    final Project project = ProjectBuilder.builder().build()
    project.apply plugin: 'pegasus'

    Assert.assertTrue(project.plugins.hasPlugin(JavaPlugin))

    // if any configuration is resolved in configuration phase, user script that tries to exclude certain dependencies will fail
    project.configurations.each {
      Assert.assertSame(it.state, Configuration.State.UNRESOLVED)
    }

    Assert.assertNotNull(project.configurations.findByName('dataTemplate'))
    Assert.assertNotNull(project.configurations.findByName('restClient'))

    Assert.assertTrue(project.pegasus instanceof Map)
    Assert.assertTrue(project.PegasusGenerationMode instanceof Map)
    Assert.assertTrue(project.pegasus.main instanceof PegasusOptions)

    Assert.assertFalse(project.pegasus.main.hasGenerationMode(PegasusOptions.GenerationMode.AVRO))
    Assert.assertTrue(project.pegasus.main.hasGenerationMode(PegasusOptions.GenerationMode.PEGASUS))
  }
}