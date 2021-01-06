package com.linkedin.pegasus.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.testing.Test;

/**
 * This plugin configures the alternateJvmTest task. This allows users to run their tests with a different JVM.
 * This will allow users to support multiple JVMs if their libraries have upstreams in multiple JVM versions.
 * This task will only run if and only if the alternateJvm project property is set.
 * You can use it manually like this: "ligradle -PalternateJvm=/export/apps/jdk/JDK-11_0_5-zulu/bin/java check".
 *
 * The alternateJvm property takes either a jvm_version (can be found in ~/jdk-versions.json) or a direct path to a
 * java executable.
 *
 * You can also add this command to whatever mint command you wish to.
 *
 *
 * WARNING: Please do not set this in gradle.properties because this would set that JVM for ALL your tests.
 **/
public class AlternateJvmTestPlugin implements Plugin<Project> {
  public static final String ALTERNATE_JVM_TEST_PROPERTY_NAME = "alternateJvm";

  private final static Logger LOG = Logging.getLogger(AlternateJvmTestPlugin.class);

  @Override
  public void apply(Project project) {
    configureAlternateJVMForTests(project);
  }

  /**
   * This method configures the alternateJVM for all tests.
   *
   * @param project The project property "alternateJvm" must be provided in order for the task to be configured
   */
  private void configureAlternateJVMForTests(Project project) {
    if (!project.hasProperty(ALTERNATE_JVM_TEST_PROPERTY_NAME) || project.property(ALTERNATE_JVM_TEST_PROPERTY_NAME) == null) {
      return;
    }

    String alternateJvmProperty = project.property(ALTERNATE_JVM_TEST_PROPERTY_NAME).toString();

    LOG.lifecycle("Setting Test tasks to run with {} jvm.", alternateJvmProperty);

    // We set the java version for tests to the property passed in
    project.getTasks().withType(Test.class).forEach(task -> task.setExecutable(alternateJvmProperty));
  }
}
