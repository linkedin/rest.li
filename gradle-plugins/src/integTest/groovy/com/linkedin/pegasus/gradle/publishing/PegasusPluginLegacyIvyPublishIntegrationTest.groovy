package com.linkedin.pegasus.gradle.publishing

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import java.util.zip.ZipFile

/**
 * Regression test to certify legacy Ivy publication behavior
 *
 * <p>Grandparent -> parent -> child pattern certifies that the child project can transitively resolve references
 * to schemas contained in grandparent's data-template jar
 */
class PegasusPluginLegacyIvyPublishIntegrationTest extends Specification {

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

  def 'publishes and consumes dataTemplate configurations'() {
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
    |//legacy publishing configuration
    |tasks.withType(Upload) {
    |  repositories {
    |    ivy { url '$localIvyRepo' }
    |  }
    |}""".stripMargin()

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
        .withArguments('uploadDataTemplate', 'uploadTestDataTemplate', 'uploadAvroSchema', 'uploadTestAvroSchema', 'uploadArchives', '-is')
        //.forwardOutput()
        //.withDebug(true)

    def grandparentResult = grandparentRunner.build()

    then:
    grandparentResult.task(':compileMainGeneratedDataTemplateJava').outcome == TaskOutcome.SUCCESS
    grandparentResult.task(':uploadDataTemplate').outcome == TaskOutcome.SUCCESS
    grandparentResult.task(':uploadArchives').outcome == TaskOutcome.SUCCESS

    def grandparentProjectIvyDescriptor = new File(localIvyRepo.path, 'com.linkedin.pegasus-grandparent-demo/grandparent/1.0.0/ivy-1.0.0.xml')
    grandparentProjectIvyDescriptor.exists()
    def grandparentProjectIvyDescriptorContents = grandparentProjectIvyDescriptor.text
    def expectedGrandparentContents = new File(Thread.currentThread().contextClassLoader.getResource('ivy/legacy/expectedGrandparentIvyDescriptorContents.txt').toURI()).text
    grandparentProjectIvyDescriptorContents.contains expectedGrandparentContents

    def grandparentProjectPrimaryArtifact = new File(localIvyRepo.path, 'com.linkedin.pegasus-grandparent-demo/grandparent/1.0.0/grandparent-1.0.0.jar')
    grandparentProjectPrimaryArtifact.exists()
    def grandparentProjectDataTemplateArtifact = new File(localIvyRepo.path, 'com.linkedin.pegasus-grandparent-demo/grandparent/1.0.0/grandparent-data-template-1.0.0.jar')
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
    |//legacy publishing configuration
    |tasks.withType(Upload) {
    |  repositories {
    |    ivy { url '$localIvyRepo' }
    |  }
    |}""".stripMargin()

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
        .withArguments('uploadDataTemplate', 'uploadTestDataTemplate', 'uploadAvroSchema', 'uploadTestAvroSchema', 'uploadArchives', '-is')
        //.forwardOutput()
        //.withDebug(true)

    def parentResult = parentRunner.build()

    then:
    parentResult.task(':compileMainGeneratedDataTemplateJava').outcome == TaskOutcome.SUCCESS
    parentResult.task(':uploadDataTemplate').outcome == TaskOutcome.SUCCESS
    parentResult.task(':uploadArchives').outcome == TaskOutcome.SUCCESS

    def parentProjectIvyDescriptor = new File(localIvyRepo.path, 'com.linkedin.pegasus-parent-demo/parent/1.0.0/ivy-1.0.0.xml')
    parentProjectIvyDescriptor.exists()
    def parentProjectIvyDescriptorContents = parentProjectIvyDescriptor.text
    def expectedParentContents = new File(Thread.currentThread().contextClassLoader.getResource('ivy/legacy/expectedParentIvyDescriptorContents.txt').toURI()).text
    parentProjectIvyDescriptorContents.contains expectedParentContents

    def parentProjectPrimaryArtifact = new File(localIvyRepo.path, 'com.linkedin.pegasus-parent-demo/parent/1.0.0/parent-1.0.0.jar')
    parentProjectPrimaryArtifact.exists()
    def parentProjectDataTemplateArtifact = new File(localIvyRepo.path, 'com.linkedin.pegasus-parent-demo/parent/1.0.0/parent-data-template-1.0.0.jar')
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
    |//legacy publishing configuration
    |tasks.withType(Upload) {
    |  repositories {
    |    ivy { url '$localIvyRepo' }
    |  }
    |}""".stripMargin()

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
        .withArguments('uploadDataTemplate', 'uploadTestDataTemplate', 'uploadAvroSchema', 'uploadTestAvroSchema', 'uploadArchives', '-is')
        //.forwardOutput()
        //.withDebug(true)

    def childResult = childRunner.build()

    then:
    childResult.task(':compileMainGeneratedDataTemplateJava').outcome == TaskOutcome.SUCCESS
    childResult.task(':uploadDataTemplate').outcome == TaskOutcome.SUCCESS
    childResult.task(':uploadArchives').outcome == TaskOutcome.SUCCESS

    def childProjectIvyDescriptor = new File(localIvyRepo.path, 'com.linkedin.pegasus-child-demo/child/1.0.0/ivy-1.0.0.xml')
    childProjectIvyDescriptor.exists()
    def childProjectIvyDescriptorContents = childProjectIvyDescriptor.text
    def expectedChildContents = new File(Thread.currentThread().contextClassLoader.getResource('ivy/legacy/expectedChildIvyDescriptorContents.txt').toURI()).text
    childProjectIvyDescriptorContents.contains expectedChildContents

    def childProjectPrimaryArtifact = new File(localIvyRepo.path, 'com.linkedin.pegasus-child-demo/child/1.0.0/child-1.0.0.jar')
    childProjectPrimaryArtifact.exists()
    def childProjectDataTemplateArtifact = new File(localIvyRepo.path, 'com.linkedin.pegasus-child-demo/child/1.0.0/child-data-template-1.0.0.jar')
    childProjectDataTemplateArtifact.exists()

    assertZipContains(childProjectDataTemplateArtifact, 'com/linkedin/child/Photo.class')
    assertZipContains(childProjectDataTemplateArtifact, 'pegasus/com/linkedin/child/Photo.pdl')
  }

  /**
   * Regression test illustrating how to consume software components published using the legacy Ivy format.
   *
   * <p>By requesting a named <b>capability</b> instead of a specific configuration name, we can consume pegasus
   * artifacts in a forward-compatible manner.
   *
   * Note that, in order to derive information about the capabilities of a software component, we must augment
   * the consumer logic with a ComponentMetadataRule.
   *
   * <p>See <a href="https://docs.gradle.org/6.8.3/userguide/feature_variants.html">Modeling feature variants and optional dependencies</a>
   * and <a href="https://docs.gradle.org/6.8.3/userguide/feature_variants.html#sec::consuming_feature_variants">Consuming Feature Variants</a>
   * for more information about capabilities.
   */
  def 'publishes with legacy ivies but derives capabilities from dataTemplate configurations'() {
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
    |//legacy publishing configuration
    |tasks.withType(Upload) {
    |  repositories {
    |    ivy { url '$localIvyRepo' }
    |  }
    |}""".stripMargin()

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
        .withArguments('uploadDataTemplate', 'uploadTestDataTemplate', 'uploadAvroSchema', 'uploadTestAvroSchema', 'uploadArchives', '-is')
    //.forwardOutput()
    //.withDebug(true)

    def grandparentResult = grandparentRunner.build()

    then:
    grandparentResult.task(':compileMainGeneratedDataTemplateJava').outcome == TaskOutcome.SUCCESS
    grandparentResult.task(':uploadDataTemplate').outcome == TaskOutcome.SUCCESS
    grandparentResult.task(':uploadArchives').outcome == TaskOutcome.SUCCESS

    def grandparentProjectIvyDescriptor = new File(localIvyRepo.path, 'com.linkedin.pegasus-grandparent-demo/grandparent/1.0.0/ivy-1.0.0.xml')
    grandparentProjectIvyDescriptor.exists()
    def grandparentProjectIvyDescriptorContents = grandparentProjectIvyDescriptor.text
    def expectedGrandparentContents = new File(Thread.currentThread().contextClassLoader.getResource('ivy/legacyWithVariantDerivation/expectedGrandparentIvyDescriptorContents.txt').toURI()).text
    grandparentProjectIvyDescriptorContents.contains expectedGrandparentContents

    def grandparentProjectPrimaryArtifact = new File(localIvyRepo.path, 'com.linkedin.pegasus-grandparent-demo/grandparent/1.0.0/grandparent-1.0.0.jar')
    grandparentProjectPrimaryArtifact.exists()
    def grandparentProjectDataTemplateArtifact = new File(localIvyRepo.path, 'com.linkedin.pegasus-grandparent-demo/grandparent/1.0.0/grandparent-data-template-1.0.0.jar')
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
    |  dataModel ('com.linkedin.pegasus-grandparent-demo:grandparent:1.0.0') {
    |    capabilities {
    |      requireCapability('com.linkedin.pegasus-grandparent-demo:grandparent-data-template:1.0.0')
    |    }
    |  }
    |  components.all(com.linkedin.pegasus.gradle.rules.PegasusIvyVariantDerivationRule)
    |}
    |
    |//legacy publishing configuration
    |tasks.withType(Upload) {
    |  repositories {
    |    ivy { url '$localIvyRepo' }
    |  }
    |}""".stripMargin()

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
        .withArguments('uploadDataTemplate', 'uploadTestDataTemplate', 'uploadAvroSchema', 'uploadTestAvroSchema', 'uploadArchives', '-is')
    //.forwardOutput()
    //.withDebug(true)

    def parentResult = parentRunner.build()

    then:
    parentResult.task(':compileMainGeneratedDataTemplateJava').outcome == TaskOutcome.SUCCESS
    parentResult.task(':uploadDataTemplate').outcome == TaskOutcome.SUCCESS
    parentResult.task(':uploadArchives').outcome == TaskOutcome.SUCCESS

    def parentProjectIvyDescriptor = new File(localIvyRepo.path, 'com.linkedin.pegasus-parent-demo/parent/1.0.0/ivy-1.0.0.xml')
    parentProjectIvyDescriptor.exists()
    def parentProjectIvyDescriptorContents = parentProjectIvyDescriptor.text
    def expectedParentContents = new File(Thread.currentThread().contextClassLoader.getResource('ivy/legacyWithVariantDerivation/expectedParentIvyDescriptorContents.txt').toURI()).text
    parentProjectIvyDescriptorContents.contains expectedParentContents

    def parentProjectPrimaryArtifact = new File(localIvyRepo.path, 'com.linkedin.pegasus-parent-demo/parent/1.0.0/parent-1.0.0.jar')
    parentProjectPrimaryArtifact.exists()
    def parentProjectDataTemplateArtifact = new File(localIvyRepo.path, 'com.linkedin.pegasus-parent-demo/parent/1.0.0/parent-data-template-1.0.0.jar')
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
    |  dataModel ('com.linkedin.pegasus-parent-demo:parent:1.0.0') {
    |    capabilities {
    |      requireCapability('com.linkedin.pegasus-parent-demo:parent-data-template:1.0.0')
    |    }
    |  }
    |  components.all(com.linkedin.pegasus.gradle.rules.PegasusIvyVariantDerivationRule)    
    |}
    |
    |//legacy publishing configuration
    |tasks.withType(Upload) {
    |  repositories {
    |    ivy { url '$localIvyRepo' }
    |  }
    |}""".stripMargin()

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
        .withArguments('uploadDataTemplate', 'uploadTestDataTemplate', 'uploadAvroSchema', 'uploadTestAvroSchema', 'uploadArchives', '-is')
    //.forwardOutput()
    //.withDebug(true)

    def childResult = childRunner.build()

    then:
    childResult.task(':compileMainGeneratedDataTemplateJava').outcome == TaskOutcome.SUCCESS
    childResult.task(':uploadDataTemplate').outcome == TaskOutcome.SUCCESS
    childResult.task(':uploadArchives').outcome == TaskOutcome.SUCCESS

    def childProjectIvyDescriptor = new File(localIvyRepo.path, 'com.linkedin.pegasus-child-demo/child/1.0.0/ivy-1.0.0.xml')
    childProjectIvyDescriptor.exists()
    def childProjectIvyDescriptorContents = childProjectIvyDescriptor.text
    def expectedChildContents = new File(Thread.currentThread().contextClassLoader.getResource('ivy/legacyWithVariantDerivation/expectedChildIvyDescriptorContents.txt').toURI()).text
    childProjectIvyDescriptorContents.contains expectedChildContents

    def childProjectPrimaryArtifact = new File(localIvyRepo.path, 'com.linkedin.pegasus-child-demo/child/1.0.0/child-1.0.0.jar')
    childProjectPrimaryArtifact.exists()
    def childProjectDataTemplateArtifact = new File(localIvyRepo.path, 'com.linkedin.pegasus-child-demo/child/1.0.0/child-data-template-1.0.0.jar')
    childProjectDataTemplateArtifact.exists()

    assertZipContains(childProjectDataTemplateArtifact, 'com/linkedin/child/Photo.class')
    assertZipContains(childProjectDataTemplateArtifact, 'pegasus/com/linkedin/child/Photo.pdl')
  }

  private static boolean assertZipContains(File zip, String path) {
    return new ZipFile(zip).getEntry(path)
  }

}
