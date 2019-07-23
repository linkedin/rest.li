package com.linkedin.pegasus.gradle;

import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.util.GFileUtils;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import static org.testng.Assert.*;


public final class TestPathingJarUtil
{
  @Test
  public void testCreatesGeneratesPathingJar() throws IOException
  {
    //setup
    Project project = ProjectBuilder.builder().build();
    String taskName = "myTaskName";
    File tempFile = new File(project.getBuildDir(), "temp.class");
    GFileUtils.touch(tempFile);
    FileCollection files = project.files(tempFile);

    //when
    PathingJarUtil.generatePathingJar(project, taskName, files, true);
    File pathingJar = new File(project.getBuildDir(), taskName + '/' + project.getName() + "-pathing.jar");
    assertTrue(pathingJar.exists());
    JarInputStream jarStream = new JarInputStream(new FileInputStream(pathingJar));
    Manifest manifest = jarStream.getManifest();
    assertTrue(manifest.getMainAttributes().getValue(Attributes.Name.CLASS_PATH).contains("temp.class"));
  }

  @Test
  public void testDoesNotCreatePathingJar() throws IOException
  {
    //setup
    Project project = ProjectBuilder.builder().build();
    String taskName = "myTaskName";

    File tempFile = new File(project.getBuildDir(), "temp.class");
    File restliTools = new File(project.getBuildDir(), "restli-tools-scala");

    GFileUtils.touch(tempFile);
    GFileUtils.touch(restliTools);
    FileCollection files = project.files(tempFile, restliTools);

    //when
    File pathingJar = new File(project.getBuildDir(), taskName + '/' + project.getName() + "-pathing.jar");
    PathingJarUtil.generatePathingJar(project, taskName, files, false);
    assertFalse(pathingJar.exists());
  }
}
