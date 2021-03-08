package com.linkedin.pegasus.gradle

import groovy.json.JsonOutput
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.*

class PegasusPluginCacheabilityTest extends Specification {
  @Rule
  TemporaryFolder tempDir = new TemporaryFolder()

  def 'mainDataTemplateJar tasks are up-to-date'() {
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

    // Create a simple pdsc schema
    def schemaFilename = 'ATypeRef.pdsc'
    def pegasusDir = tempDir.newFolder('src', 'main', 'pegasus')
    def pdscFile = new File("$pegasusDir.path$File.separator$schemaFilename")
    def pdscData = [
            type: 'typeref',
            name: 'ATypeRef',
            ref:  'string',
            doc:  'A type ref data.'
    ]
    pdscFile << JsonOutput.prettyPrint(JsonOutput.toJson(pdscData))

    // Expected schema files in the build directory
    def compiledSchema = new File([tempDir.root, 'build', 'classes', 'java', 'mainGeneratedDataTemplate', 'ATypeRef.class'].join(File.separator))
    def preparedSchema = new File([tempDir.root, 'build', 'mainSchemas', schemaFilename].join(File.separator))

    when:
    def result = runner.build()

    then:
    // Validate task output are expected
    result.task(':generateDataTemplate').outcome == SUCCESS
    result.task(':compileMainGeneratedDataTemplateJava').outcome == SUCCESS
    result.task(':mainCopySchemas').outcome == SUCCESS
    result.task(':processMainGeneratedDataTemplateResources').outcome == NO_SOURCE
    result.task(':mainGeneratedDataTemplateClasses').outcome ==  SUCCESS
    result.task(':mainTranslateSchemas').outcome == SUCCESS
    result.task(':mainDataTemplateJar').outcome == SUCCESS

    // Validate compiled and prepared schemas exist
    compiledSchema.exists()
    preparedSchema.exists()

    when:
    result = runner.build()

    then:
    // Validate task output are expected
    result.task(':generateDataTemplate').outcome == UP_TO_DATE
    result.task(':compileMainGeneratedDataTemplateJava').outcome == UP_TO_DATE
    result.task(':mainCopySchemas').outcome == UP_TO_DATE
    result.task(':processMainGeneratedDataTemplateResources').outcome == NO_SOURCE
    result.task(':mainGeneratedDataTemplateClasses').outcome == UP_TO_DATE
    result.task(':mainTranslateSchemas').outcome == UP_TO_DATE
    result.task(':mainDataTemplateJar').outcome == UP_TO_DATE

    // Validate compiled and prepared schemas exist
    compiledSchema.exists()
    preparedSchema.exists()
  }
}
