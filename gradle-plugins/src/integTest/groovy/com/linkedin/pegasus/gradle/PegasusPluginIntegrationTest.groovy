package com.linkedin.pegasus.gradle

import groovy.json.JsonOutput
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

import java.util.zip.ZipFile

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class PegasusPluginIntegrationTest extends Specification {
  @Rule
  TemporaryFolder tempDir = new TemporaryFolder()

  @Unroll
  def "apply pegasus plugin with Gradle #gradleVersion"() {
    setup:
    def buildFile = tempDir.newFile('build.gradle')
    buildFile.text = "plugins { id 'pegasus' }"

    when:
    def result = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(tempDir.root)
        .withPluginClasspath()
        .withArguments('mainDataTemplateJar')
        .forwardOutput()
        .build()

    then:
    result.task(':mainDataTemplateJar').outcome == SUCCESS

    where:
    gradleVersion << [ '5.2.1', '5.3', '5.6.4', '6.9', '7.0.2' ]
  }

  @Unroll
  def "data-template jar contains classes and schemas with Gradle #gradleVersion"() {
    setup:
    tempDir.newFile('build.gradle') << """
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
    |version = '1.0.0'
    """.stripMargin()

    tempDir.newFile('settings.gradle') << '''
    |rootProject.name = 'root'
    '''.stripMargin()

    def schemaDir = tempDir.newFolder('src', 'main', 'pegasus', 'com', 'linkedin')

    def pdlSchemaName = 'LatLong.pdl'
    new File(schemaDir, pdlSchemaName) << '''
    |namespace com.linkedin
    |
    |record LatLong {
    |  latitude: optional float
    |  longitude: optional float
    |}
    '''.stripMargin()

    def extensionSchemaName = 'LatLongExtensions.pdl'
    def extensionsDir = tempDir.newFolder('src', 'main', 'extensions', 'com', 'linkedin')
    new File(extensionsDir, extensionSchemaName) << '''
    |namespace com.linkedin
    |
    |record LatLongExtensions includes LatLong {
    |}
    '''.stripMargin()

    when:
    def result = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(tempDir.root)
        .withPluginClasspath()
        .withArguments('mainDataTemplateJar')
        .forwardOutput()
        .build()

    then:
    result.task(':mainDataTemplateJar').outcome == SUCCESS

    def dataTemplateArtifact = new File(tempDir.root, 'build/libs/root-data-template-1.0.0.jar')

    assertZipContains(dataTemplateArtifact, 'com/linkedin/LatLong.class')
    assertZipContains(dataTemplateArtifact, 'pegasus/com/linkedin/LatLong.pdl')
    assertZipContains(dataTemplateArtifact, 'legacyPegasusSchemas/com/linkedin/LatLong.pdsc')
    assertZipContains(dataTemplateArtifact, 'extensions/com/linkedin/LatLongExtensions.pdl')

    where:
    gradleVersion << [ '5.2.1', '5.3', '5.6.4', '6.9', '7.0.2' ]
  }

  def 'mainCopySchema task will remove stale PDSC'() {
    setup:
    def runner = GradleRunner.create()
        .withProjectDir(tempDir.root)
        .withPluginClasspath()
        .withArguments('mainDataTemplateJar')

    def settingsFile = tempDir.newFile('settings.gradle')
    settingsFile << "rootProject.name = 'test-project'"

    def buildFile = tempDir.newFile('build.gradle')
    buildFile << """
    |plugins {
    |  id 'pegasus'
    |}
    |
    |repositories {
    |  jcenter()
    |}
    |
    |dependencies {
    |  dataTemplateCompile files(${System.getProperty('integTest.dataTemplateCompileDependencies')})
    |  pegasusPlugin files(${System.getProperty('integTest.pegasusPluginDependencies')})
    |}
    """.stripMargin()

    def pegasusDir = tempDir.newFolder('src', 'main', 'pegasus')
    def pdscFilename1 = 'ATypeRef.pdsc'
    def pdscFile1 = new File("$pegasusDir.path$File.separator$pdscFilename1")
    def pdscData1 = [
        type: 'typeref',
        name: 'ATypeRef',
        ref : 'string',
        doc : 'A type ref data.'
    ]
    pdscFile1 << JsonOutput.prettyPrint(JsonOutput.toJson(pdscData1))
    def pdscFilename2 = 'BTypeRef.pdsc'
    def pdscFile2 = new File("$pegasusDir.path$File.separator$pdscFilename2")
    def pdscData2 = [
        type: 'typeref',
        name: 'BTypeRef',
        ref : 'string',
        doc : 'B type ref data.'
    ]
    pdscFile2 << JsonOutput.prettyPrint(JsonOutput.toJson(pdscData2))
    def mainSchemasDir = [tempDir.root, 'build', 'mainSchemas'].join(File.separator)
    def preparedPdscFile1 = new File("$mainSchemasDir$File.separator$pdscFilename1")
    def preparedPdscFile2 = new File("$mainSchemasDir$File.separator$pdscFilename2")

    when:
    def result = runner.build()

    then:
    result.task(':mainCopySchemas').getOutcome() == SUCCESS
    preparedPdscFile1.exists()
    preparedPdscFile2.exists()

    when:
    pdscFile1.delete()
    result = runner.build()

    then:
    result.task(':mainCopySchemas').getOutcome() == SUCCESS
    !preparedPdscFile1.exists()
    preparedPdscFile2.exists()
  }

  private static boolean assertZipContains(File zip, String path) {
    return new ZipFile(zip).getEntry(path)
  }
}
