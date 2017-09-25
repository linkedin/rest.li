package com.linkedin.pegasus.gradle


import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.util.GFileUtils
import org.testng.annotations.Test

import java.util.jar.Attributes
import java.util.jar.JarInputStream
import java.util.jar.Manifest

class TestPathingJarUtil {

  @Test
  void testCreatesGeneratesPathingJar() {
    //setup
    final Project project = ProjectBuilder.builder().build()
    final String taskName = 'myTaskName'
    final tempFile = new File(project.buildDir, 'temp.class')
    GFileUtils.touch(tempFile)
    final FileCollection files = project.files(tempFile)

    //when
    PathingJarUtil.generatePathingJar(project, taskName, files, true)
    File pathingJar = new File(project.buildDir, "${taskName}/${project.name}-pathing.jar")
    assert pathingJar.exists()
    JarInputStream jarStream = new JarInputStream(new FileInputStream(pathingJar))
    Manifest manifest = jarStream.getManifest()
    assert manifest.getMainAttributes().getValue(Attributes.Name.CLASS_PATH).contains('temp.class')
  }

  @Test
  void testDoesNotCreatePathingJar() {
    //setup
    final Project project = ProjectBuilder.builder().build()
    final String taskName = 'myTaskName'

    final tempFile = new File(project.buildDir, 'temp.class')
    final restliTools = new File(project.buildDir, 'restli-tools-scala')

    GFileUtils.touch(tempFile)
    GFileUtils.touch(restliTools)
    final FileCollection files = project.files(tempFile, restliTools)

    //when
    File pathingJar = new File(project.buildDir, "${taskName}/${project.name}-pathing.jar")
    PathingJarUtil.generatePathingJar(project, taskName, files, false)
    assert !pathingJar.exists()
  }
}
