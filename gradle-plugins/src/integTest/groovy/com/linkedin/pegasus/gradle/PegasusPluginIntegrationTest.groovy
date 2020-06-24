package com.linkedin.pegasus.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

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
        .withArguments("mainDataTemplateJar")
        .forwardOutput()
        .build()

    then:
    result.task(':mainDataTemplateJar').outcome == TaskOutcome.SUCCESS
  }
}
