package com.linkedin.pegasus.gradle

import groovy.json.JsonOutput
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class PegasusPluginIntegrationTest extends Specification {
  @Rule
  TemporaryFolder tempDir = new TemporaryFolder()

  def 'apply pegasus plugin'() {
    setup:
    def buildFile = tempDir.newFile('build.gradle')
    buildFile.text = "plugins { id 'pegasus' }"

    when:
    def result = GradleRunner.create()
        .withProjectDir(tempDir.root)
        .withPluginClasspath()
        .withArguments('mainDataTemplateJar')
        //.forwardOutput()
        .build()

    then:
    result.task(':mainDataTemplateJar').outcome == SUCCESS
  }

  def 'mainCopySchema task will remove stale pdsc'() {
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
}
