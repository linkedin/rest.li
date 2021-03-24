package com.linkedin.pegasus.gradle.publishing

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import java.util.zip.ZipFile

/**
 * Regression test to certify Maven publication behavior
 *
 * <p>Historically rest.li is tightly coupled to the features of Ivy - specifically, Ivy's ability to publish
 * an alternate set of parallel dependencies for the dataTemplate configuration and its associated artifact.
 *
 * <p>The Maven POM format is incapable of representing this pattern.  Therefore the only alternative to produce and
 * consume Maven-style metadata is by augmenting it with Gradle Module Metadata.
 *
 * <p>NB: this test runs with Gradle 6.6, which introduces the API
 * {@link org.gradle.api.publish.Publication#withoutBuildIdentifier()}. This is not strictly required but aids in test
 * reproducibility, as otherwise a random UUID is persisted to the .module file.  Note that the build Id will be omitted
 * by default starting with Gradle 7.0. See <a href="https://github.com/gradle/gradle/issues/13800">gradle/gradle#13800</a>.
 *
 * <p>For more about Gradle Module Metadata, see <a href="https://docs.gradle.org/6.8.3/userguide/publishing_gradle_module_metadata.html">Understanding Gradle Module Metadata</a>.
 *
 * <p>Grandparent -> parent -> child pattern certifies that the child project can transitively resolve references
 * to schemas contained in grandparent's data-template jar
 */
class PegasusPluginMavenPublishIntegrationTest extends Specification {

  @Rule
  TemporaryFolder grandparentProject

  @Rule
  TemporaryFolder parentProject

  @Rule
  TemporaryFolder childProject

  @Rule
  TemporaryFolder localRepo

  URL localMavenRepo

  def setup() {
    localMavenRepo = localRepo.newFolder('local-maven-repo').toURI().toURL()
  }

  def 'publishes and consumes dataTemplate configurations with Gradle Module Metadata'() {
    given:
    def gradlePropertiesFile = grandparentProject.newFile('gradle.properties')
    gradlePropertiesFile << '''
    |group=com.linkedin.pegasus-grandparent-demo
    |version=1.0.0
    |'''.stripMargin()

    def settingsFile = grandparentProject.newFile('settings.gradle')
    settingsFile << "rootProject.name = 'grandparent'"

    grandparentProject.newFile('build.gradle') << """
    |plugins {
    |  id 'maven-publish'
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
    |//modern maven-publish configuration
    |publishing {
    |  publications {
    |    maven(MavenPublication) {
    |      from components.java
    |      withoutBuildIdentifier()
    |    }
    |  }
    |  repositories {
    |    maven { url '$localMavenRepo' }
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
        .withPluginClasspath()
        .withGradleVersion('6.6') // the minimum version supporting the Publication#withoutBuildIdentifier() API
        .withArguments('publish', '-is')
        //.forwardOutput()
        //.withDebug(true)

    def grandparentResult = grandparentRunner.build()

    then:
    grandparentResult.task(':compileMainGeneratedDataTemplateJava').outcome == TaskOutcome.SUCCESS
    grandparentResult.task(':generatePomFileForMavenPublication').outcome == TaskOutcome.SUCCESS

    def grandparentProjectPomFile = new File(localMavenRepo.path, 'com/linkedin/pegasus-grandparent-demo/grandparent/1.0.0/grandparent-1.0.0.pom')
    grandparentProjectPomFile.exists()
    def grandparentProjectPomFileContents = grandparentProjectPomFile.text
    def expectedGrandparentContents = new File(Thread.currentThread().contextClassLoader.getResource('maven/expectedGrandparentPomFile.pom').toURI()).text
    grandparentProjectPomFileContents == expectedGrandparentContents

    def grandparentProjectModuleFile = new File(localMavenRepo.path, 'com/linkedin/pegasus-grandparent-demo/grandparent/1.0.0/grandparent-1.0.0.module')
    grandparentProjectModuleFile.exists()
    def grandparentProjectModuleFileContents = grandparentProjectModuleFile.text
    def expectedGrandparentModuleContents = new File(Thread.currentThread().contextClassLoader.getResource('maven/expectedGrandparentModuleFile.module').toURI()).text
    grandparentProjectModuleFileContents == expectedGrandparentModuleContents

    def grandparentProjectPrimaryArtifact = new File(localMavenRepo.path, 'com/linkedin/pegasus-grandparent-demo/grandparent/1.0.0/grandparent-1.0.0.jar')
    grandparentProjectPrimaryArtifact.exists()
    //NB note naming scheme of data-template jar changes when classifier, not appendix, is used
    def grandparentProjectDataTemplateArtifact = new File(localMavenRepo.path, 'com/linkedin/pegasus-grandparent-demo/grandparent/1.0.0/grandparent-1.0.0-data-template.jar')
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
    |  id 'maven-publish'
    |  id 'pegasus'
    |}
    |
    |repositories {
    |  maven { url '$localMavenRepo' }
    |  mavenCentral()
    |}
    |
    |dependencies {
    |  dataTemplateCompile files(${System.getProperty('integTest.dataTemplateCompileDependencies')})
    |  pegasusPlugin files(${System.getProperty('integTest.pegasusPluginDependencies')})
    |
    |  //dataModel group: 'com.linkedin.pegasus-grandparent-demo', name: 'grandparent', version: '1.0.0', configuration: 'dataTemplate'
    |  dataModel ('com.linkedin.pegasus-grandparent-demo:grandparent:1.0.0') {
    |    capabilities {
    |      requireCapability('com.linkedin.pegasus-grandparent-demo:grandparent-data-template:1.0.0') // TODO Gradle 6.0 requires an explicit version, 6.? does not
    |    }
    |  }
    |}
    |
    |//modern maven-publish configuration
    |publishing {
    |  publications {
    |    maven(MavenPublication) {
    |      from components.java
    |      withoutBuildIdentifier()
    |    }
    |  }
    |  repositories {
    |    maven { url '$localMavenRepo' }
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
        .withPluginClasspath()
        .withGradleVersion('6.6') // the minimum version supporting the Publication#withoutBuildIdentifier() API
        .withArguments('publish', '-is')
        //.forwardOutput()
        //.withDebug(true)

    def parentResult = parentRunner.build()

    then:
    parentResult.task(':compileMainGeneratedDataTemplateJava').outcome == TaskOutcome.SUCCESS
    parentResult.task(':generatePomFileForMavenPublication').outcome == TaskOutcome.SUCCESS

    def parentProjectPomFile = new File(localMavenRepo.path, 'com/linkedin/pegasus-parent-demo/parent/1.0.0/parent-1.0.0.pom')
    parentProjectPomFile.exists()
    def parentProjectPomFileContents = parentProjectPomFile.text
    def expectedParentContents = new File(Thread.currentThread().contextClassLoader.getResource('maven/expectedParentPomFile.pom').toURI()).text
    parentProjectPomFileContents == expectedParentContents

    def parentProjectModuleFile = new File(localMavenRepo.path, 'com/linkedin/pegasus-parent-demo/parent/1.0.0/parent-1.0.0.module')
    parentProjectModuleFile.exists()
    def parentProjectModuleFileContents = parentProjectModuleFile.text
    def expectedParentModuleContents = new File(Thread.currentThread().contextClassLoader.getResource('maven/expectedParentModuleFile.module').toURI()).text
    parentProjectModuleFileContents == expectedParentModuleContents

    def parentProjectPrimaryArtifact = new File(localMavenRepo.path, 'com/linkedin/pegasus-parent-demo/parent/1.0.0/parent-1.0.0.jar')
    parentProjectPrimaryArtifact.exists()
    //NB note naming scheme of data-template jar changes when classifier, not appendix, is used
    def parentProjectDataTemplateArtifact = new File(localMavenRepo.path, 'com/linkedin/pegasus-parent-demo/parent/1.0.0/parent-1.0.0-data-template.jar')
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
    |  id 'maven-publish'
    |  id 'pegasus'
    |}
    |
    |repositories {
    |  maven { url '$localMavenRepo' }
    |  mavenCentral()
    |}
    |
    |dependencies {
    |  dataTemplateCompile files(${System.getProperty('integTest.dataTemplateCompileDependencies')})
    |  pegasusPlugin files(${System.getProperty('integTest.pegasusPluginDependencies')})
    |
    |  //dataModel group: 'com.linkedin.pegasus-parent-demo', name: 'parent', version: '1.0.0', configuration: 'dataTemplate'
    |  dataModel ('com.linkedin.pegasus-parent-demo:parent:1.0.0') {
    |    capabilities {
    |      requireCapability('com.linkedin.pegasus-parent-demo:parent-data-template:1.0.0') // TODO Gradle 6.0 requires an explicit version, 6.? does not
    |    }
    |  }    
    |}
    |
    |//modern maven-publish configuration
    |publishing {
    |  publications {
    |    maven(MavenPublication) {
    |      from components.java
    |      withoutBuildIdentifier()
    |    }
    |  }
    |  repositories {
    |    maven { url '$localMavenRepo' }
    |  }
    |}
    """.stripMargin()

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
        .withPluginClasspath()
        .withGradleVersion('6.6') // the minimum version supporting the Publication#withoutBuildIdentifier() API
        .withArguments('publish', '-is')
        //.forwardOutput()
        //.withDebug(true)

    def childResult = childRunner.build()

    then:
    childResult.task(':compileMainGeneratedDataTemplateJava').outcome == TaskOutcome.SUCCESS
    childResult.task(':generatePomFileForMavenPublication').outcome == TaskOutcome.SUCCESS

    def childProjectPomFile = new File(localMavenRepo.path, 'com/linkedin/pegasus-child-demo/child/1.0.0/child-1.0.0.pom')
    childProjectPomFile.exists()
    def childProjectPomFileContents = childProjectPomFile.text
    def expectedChildContents = new File(Thread.currentThread().contextClassLoader.getResource('maven/expectedChildPomFile.pom').toURI()).text
    childProjectPomFileContents == expectedChildContents

    def childProjectModuleFile = new File(localMavenRepo.path, 'com/linkedin/pegasus-child-demo/child/1.0.0/child-1.0.0.module')
    childProjectModuleFile.exists()
    def childProjectModuleFileContents = childProjectModuleFile.text
    def expectedChildModuleContents = new File(Thread.currentThread().contextClassLoader.getResource('maven/expectedChildModuleFile.module').toURI()).text
    childProjectModuleFileContents == expectedChildModuleContents

    def childProjectPrimaryArtifact = new File(localMavenRepo.path, 'com/linkedin/pegasus-child-demo/child/1.0.0/child-1.0.0.jar')
    childProjectPrimaryArtifact.exists()
    //NB note naming scheme of data-template jar changes when classifier, not appendix, is used
    def childProjectDataTemplateArtifact = new File(localMavenRepo.path, 'com/linkedin/pegasus-child-demo/child/1.0.0/child-1.0.0-data-template.jar')
    childProjectDataTemplateArtifact.exists()

    assertZipContains(childProjectDataTemplateArtifact, 'com/linkedin/child/Photo.class')
    assertZipContains(childProjectDataTemplateArtifact, 'pegasus/com/linkedin/child/Photo.pdl')
  }

  private static boolean assertZipContains(File zip, String path) {
    return new ZipFile(zip).getEntry(path)
  }

}