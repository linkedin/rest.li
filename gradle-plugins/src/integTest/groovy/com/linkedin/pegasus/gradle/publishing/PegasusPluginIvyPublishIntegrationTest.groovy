/*
 * Copyright (c) 2021 LinkedIn Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.linkedin.pegasus.gradle.publishing

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

import java.util.zip.ZipFile

/**
 * Regression test to certify modern Ivy publication behavior using the ivy-publish plugin
 *
 * <p>Grandparent -> parent -> child pattern certifies that the child project can transitively resolve references
 * to schemas contained in grandparent's data-template jar
 */
class PegasusPluginIvyPublishIntegrationTest extends Specification {

  @Rule
  TemporaryFolder grandparentProject

  @Rule
  TemporaryFolder parentProject

  @Rule
  TemporaryFolder childProject

  @Rule
  TemporaryFolder localRepo

  URL localIvyRepo

  def setup() {
    localIvyRepo = localRepo.newFolder('local-ivy-repo').toURI().toURL()
  }

  /**
   * Regression test illustrating how to consume software components published using the modern Ivy format.
   *
   * <p>Useful to illustrate path to move a dependency graph from legacy Upload-based publication to the modern
   * Ivy format and ivy-publish plugin, without necessitating Gradle Module Metadata.
   *
   * <p>Does not attempt to derive variants for software components.
   *
   * <p>Gradle Module Metadata is not published or consumed.
   */
  @Unroll
  def "publishes and consumes dataTemplate configuration without Gradle Module Metadata with Gradle #gradleVersion"() {
    given:
    def isAtLeastGradle7 = GradleVersion.version(gradleVersion) >= GradleVersion.version("7.0")

    def gradlePropertiesFile = grandparentProject.newFile('gradle.properties')
    gradlePropertiesFile << '''
    |group=com.linkedin.pegasus-grandparent-demo
    |version=1.0.0
    |'''.stripMargin()

    def settingsFile = grandparentProject.newFile('settings.gradle')
    settingsFile << "rootProject.name = 'grandparent'"

    grandparentProject.newFile('build.gradle') << """
    |plugins {
    |  id 'ivy-publish'
    |  id 'pegasus'
    |}
    |
    |repositories {
    |  mavenCentral()
    |}
    |
    |dependencies {
    |  dataTemplateCompile files(${System.getProperty('integTest.dataTemplateCompileDependencies')})
    |  pegasusPlugin files(${System.getProperty('integTest.pegasusPluginDependencies')})
    |}
    |
    |tasks.withType(GenerateModuleMetadata) { enabled=false }
    |
    |//modern ivy-publish configuration
    |publishing {
    |  publications {
    |    ivy(IvyPublication) {
    |      from components.java
    |    }
    |  }
    |  repositories {
    |    ivy { url '$localIvyRepo' }
    |  }
    |}
    """.stripMargin()

    // Create a simple pdl schema, borrowed from restli-example-api
    def schemaFilename = 'LatLong.pdl'
    def grandparentPegasusDir = grandparentProject.newFolder('src', 'main', 'pegasus', 'com', 'linkedin', 'grandparent')
    def grandparentPdlFile = new File("$grandparentPegasusDir.path$File.separator$schemaFilename")
    grandparentPdlFile << '''namespace com.linkedin.grandparent
      |
      |record LatLong {
      |  latitude: optional float
      |  longitude: optional float
      |}'''.stripMargin()

    when:
    def grandparentRunner = GradleRunner.create()
        .withProjectDir(grandparentProject.root)
        .withGradleVersion(gradleVersion)
        .withPluginClasspath()
        .withArguments('publish', '-is')
        //.forwardOutput()
        //.withDebug(true)

    def grandparentResult = grandparentRunner.build()

    then:
    grandparentResult.task(':compileMainGeneratedDataTemplateJava').outcome == TaskOutcome.SUCCESS
    grandparentResult.task(':generateDescriptorFileForIvyPublication').outcome == TaskOutcome.SUCCESS

    def grandparentProjectIvyDescriptor = new File(localIvyRepo.path, 'com.linkedin.pegasus-grandparent-demo/grandparent/1.0.0/ivy-1.0.0.xml')
    grandparentProjectIvyDescriptor.exists()
    def grandparentProjectIvyDescriptorContents = grandparentProjectIvyDescriptor.text
    def expectedGrandparentContents = new File(Thread.currentThread().contextClassLoader
        .getResource("ivy/modern/${isAtLeastGradle7 ? 'gradle7/' : ''}expectedGrandparentIvyDescriptorContents.txt").toURI()).text
    grandparentProjectIvyDescriptorContents.contains expectedGrandparentContents

    def grandparentProjectPrimaryArtifact = new File(localIvyRepo.path, 'com.linkedin.pegasus-grandparent-demo/grandparent/1.0.0/grandparent-1.0.0.jar')
    grandparentProjectPrimaryArtifact.exists()
    //NB note naming scheme of data-template jar changes when classifier, not appendix, is used
    def grandparentProjectDataTemplateArtifact = new File(localIvyRepo.path, 'com.linkedin.pegasus-grandparent-demo/grandparent/1.0.0/grandparent-1.0.0-data-template.jar')
    grandparentProjectDataTemplateArtifact.exists()

    assertZipContains(grandparentProjectDataTemplateArtifact, 'com/linkedin/grandparent/LatLong.class')
    assertZipContains(grandparentProjectDataTemplateArtifact, 'pegasus/com/linkedin/grandparent/LatLong.pdl')

    when: 'a parent project consumes the grandparent project data-template jar'

    gradlePropertiesFile = parentProject.newFile('gradle.properties')
    gradlePropertiesFile << '''
    |group=com.linkedin.pegasus-parent-demo
    |version=1.0.0
    |'''.stripMargin()

    settingsFile = parentProject.newFile('settings.gradle')
    settingsFile << "rootProject.name = 'parent'"

    parentProject.newFile('build.gradle') << """
    |plugins {
    |  id 'ivy-publish'
    |  id 'pegasus'
    |}
    |
    |repositories {
    |  ivy { url '$localIvyRepo' }
    |  mavenCentral()
    |}
    |
    |dependencies {
    |  dataTemplateCompile files(${System.getProperty('integTest.dataTemplateCompileDependencies')})
    |  pegasusPlugin files(${System.getProperty('integTest.pegasusPluginDependencies')})
    |
    |  dataModel group: 'com.linkedin.pegasus-grandparent-demo', name: 'grandparent', version: '1.0.0', configuration: 'dataTemplate'
    |}
    |
    |tasks.withType(GenerateModuleMetadata) { enabled=false }
    |
    |//modern ivy-publish configuration
    |publishing {
    |  publications {
    |    ivy(IvyPublication) {
    |      from components.java
    |    }
    |  }
    |  repositories {
    |    ivy { url '$localIvyRepo' }
    |  }
    |}
    """.stripMargin()

    // Create a simple pdl schema which references a grandparent type
    schemaFilename = 'EXIF.pdl'
    def parentPegasusDir = parentProject.newFolder('src', 'main', 'pegasus', 'com', 'linkedin', 'parent')
    def parentPdlFile = new File("$parentPegasusDir.path$File.separator$schemaFilename")
    parentPdlFile << '''namespace com.linkedin.parent
      |
      |import com.linkedin.grandparent.LatLong
      |
      |record EXIF {
      |  isFlash: optional boolean = true
      |  location: optional LatLong
      |}'''.stripMargin()

    def parentRunner = GradleRunner.create()
        .withProjectDir(parentProject.root)
        .withGradleVersion(gradleVersion)
        .withPluginClasspath()
        .withArguments('publish', '-is')
        //.forwardOutput()
        //.withDebug(true)

    def parentResult = parentRunner.build()

    then:
    parentResult.task(':compileMainGeneratedDataTemplateJava').outcome == TaskOutcome.SUCCESS
    parentResult.task(':generateDescriptorFileForIvyPublication').outcome == TaskOutcome.SUCCESS

    def parentProjectIvyDescriptor = new File(localIvyRepo.path, 'com.linkedin.pegasus-parent-demo/parent/1.0.0/ivy-1.0.0.xml')
    parentProjectIvyDescriptor.exists()
    def parentProjectIvyDescriptorContents = parentProjectIvyDescriptor.text
    def expectedParentContents = new File(Thread.currentThread().contextClassLoader
        .getResource("ivy/modern/${isAtLeastGradle7 ? 'gradle7/' : ''}expectedParentIvyDescriptorContents.txt").toURI()).text
    parentProjectIvyDescriptorContents.contains expectedParentContents

    def parentProjectPrimaryArtifact = new File(localIvyRepo.path, 'com.linkedin.pegasus-parent-demo/parent/1.0.0/parent-1.0.0.jar')
    parentProjectPrimaryArtifact.exists()
    //NB note naming scheme of data-template jar changes when classifier, not appendix, is used
    def parentProjectDataTemplateArtifact = new File(localIvyRepo.path, 'com.linkedin.pegasus-parent-demo/parent/1.0.0/parent-1.0.0-data-template.jar')
    parentProjectDataTemplateArtifact.exists()

    assertZipContains(parentProjectDataTemplateArtifact, 'com/linkedin/parent/EXIF.class')
    assertZipContains(parentProjectDataTemplateArtifact, 'pegasus/com/linkedin/parent/EXIF.pdl')

    when: 'a child project transitively consumes the grandparent project data-template jar'

    gradlePropertiesFile = childProject.newFile('gradle.properties')
    gradlePropertiesFile << '''
    |group=com.linkedin.pegasus-child-demo
    |version=1.0.0
    |'''.stripMargin()

    settingsFile = childProject.newFile('settings.gradle')
    settingsFile << "rootProject.name = 'child'"

    childProject.newFile('build.gradle') << """
    |plugins {
    |  id 'ivy-publish'
    |  id 'pegasus'
    |}
    |
    |repositories {
    |  ivy { url '$localIvyRepo' }
    |  mavenCentral()
    |}
    |
    |dependencies {
    |  dataTemplateCompile files(${System.getProperty('integTest.dataTemplateCompileDependencies')})
    |  pegasusPlugin files(${System.getProperty('integTest.pegasusPluginDependencies')})
    |
    |  dataModel group: 'com.linkedin.pegasus-parent-demo', name: 'parent', version: '1.0.0', configuration: 'dataTemplate'
    |}
    |
    |tasks.withType(GenerateModuleMetadata) { enabled=false }
    |
    |generateDataTemplate {
    |  doFirst {
    |    logger.lifecycle 'Dumping {} classpath:', it.path
    |    resolverPath.files.each { logger.lifecycle it.name }
    |  }
    |}
    |
    |//modern ivy-publish configuration
    |publishing {
    |  publications {
    |    ivy(IvyPublication) {
    |      from components.java
    |    }
    |  }
    |  repositories {
    |    ivy { url '$localIvyRepo' }
    |  }
    |}
    |""".stripMargin()

    // Create a simple pdl schema which references parent and grandparent types
    schemaFilename = 'Photo.pdl'
    def childPegasusDir = childProject.newFolder('src', 'main', 'pegasus', 'com', 'linkedin', 'child')
    def childPdlFile = new File("$childPegasusDir.path$File.separator$schemaFilename")
    childPdlFile << '''namespace com.linkedin.child
      |
      |import com.linkedin.grandparent.LatLong
      |import com.linkedin.parent.EXIF
      |
      |record Photo {
      |  id: long
      |  urn: string
      |  title: string
      |  exif: EXIF
      |  backupLocation: optional LatLong
      |}'''.stripMargin()

    def childRunner = GradleRunner.create()
        .withProjectDir(childProject.root)
        .withGradleVersion(gradleVersion)
        .withPluginClasspath()
        .withArguments('publish', '-is')
        //.forwardOutput()
        //.withDebug(true)

    def childResult = childRunner.build()

    then:
    childResult.task(':compileMainGeneratedDataTemplateJava').outcome == TaskOutcome.SUCCESS
    childResult.task(':generateDescriptorFileForIvyPublication').outcome == TaskOutcome.SUCCESS

    def childProjectIvyDescriptor = new File(localIvyRepo.path, 'com.linkedin.pegasus-child-demo/child/1.0.0/ivy-1.0.0.xml')
    childProjectIvyDescriptor.exists()
    def childProjectIvyDescriptorContents = childProjectIvyDescriptor.text
    def expectedChildContents = new File(Thread.currentThread().contextClassLoader
        .getResource("ivy/modern/${isAtLeastGradle7 ? 'gradle7/' : ''}expectedChildIvyDescriptorContents.txt").toURI()).text
    childProjectIvyDescriptorContents.contains expectedChildContents

    def childProjectPrimaryArtifact = new File(localIvyRepo.path, 'com.linkedin.pegasus-child-demo/child/1.0.0/child-1.0.0.jar')
    childProjectPrimaryArtifact.exists()
    //NB note naming scheme of data-template jar changes when classifier, not appendix, is used
    def childProjectDataTemplateArtifact = new File(localIvyRepo.path, 'com.linkedin.pegasus-child-demo/child/1.0.0/child-1.0.0-data-template.jar')
    childProjectDataTemplateArtifact.exists()

    assertZipContains(childProjectDataTemplateArtifact, 'com/linkedin/child/Photo.class')
    assertZipContains(childProjectDataTemplateArtifact, 'pegasus/com/linkedin/child/Photo.pdl')

    where:
    gradleVersion << [ '6.1', '6.9', '7.0.2' ]
  }

  def 'ivy-publish fails gracefully with Gradle 5.2.1'() {
    given:
    grandparentProject.newFile('build.gradle') << """
    |plugins {
    |  id 'ivy-publish'
    |  id 'pegasus'
    |}""".stripMargin()

    when:
    def grandparentRunner = GradleRunner.create()
        .withProjectDir(grandparentProject.root)
        .withGradleVersion('5.2.1')
        .withPluginClasspath()
        .withArguments('tasks')
        //.forwardOutput()
        //.withDebug(true)

    def grandparentResult = grandparentRunner.buildAndFail()

    then:
    grandparentResult.output.contains 'Using the ivy-publish plugin with the pegasus plugin requires Gradle 6.1 or higher'
  }

  private static boolean assertZipContains(File zip, String path) {
    return new ZipFile(zip).getEntry(path)
  }

}
